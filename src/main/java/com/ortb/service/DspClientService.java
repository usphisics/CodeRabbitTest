package com.ortb.service;

import com.ortb.config.DspProperties;
import com.ortb.config.DspProperties.DspEndpoint;
import com.ortb.model.auction.AuctionCandidate;
import com.ortb.model.openrtb.Bid;
import com.ortb.model.openrtb.BidRequest;
import com.ortb.model.openrtb.BidResponse;
import com.ortb.model.openrtb.Imp;
import com.ortb.model.openrtb.SeatBid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Sends the incoming {@link BidRequest} to all configured external DSPs in parallel,
 * collects their {@link BidResponse}s, validates the bids, and converts them to
 * {@link AuctionCandidate}s that can compete in the unified auction.
 *
 * <p>Design principles:
 * <ul>
 *   <li>Each DSP is called concurrently on a virtual thread.</li>
 *   <li>Per-DSP deadline is honoured via {@link CompletableFuture#get(long, TimeUnit)}.</li>
 *   <li>Any DSP failure (network error, timeout, bad JSON) is caught and logged; the
 *       auction proceeds with the remaining DSPs' bids.</li>
 *   <li>Basic bid validation is applied: price {@literal >} 0, impId present, floor price,
 *       blocked categories, blocked advertisers.</li>
 * </ul>
 */
@Service
public class DspClientService {

    private static final Logger log = LoggerFactory.getLogger(DspClientService.class);

    private final DspProperties dspProperties;
    private final RestTemplate restTemplate;
    private final Executor executor;
    private final TargetingService targetingService;

    public DspClientService(
            DspProperties dspProperties,
            RestTemplate dspRestTemplate,
            @Qualifier("dspExecutor") Executor executor,
            TargetingService targetingService) {
        this.dspProperties = dspProperties;
        this.restTemplate = dspRestTemplate;
        this.executor = executor;
        this.targetingService = targetingService;
    }

    /**
     * Broadcasts the bid request to all configured DSPs concurrently and returns
     * all valid {@link AuctionCandidate}s across every DSP and every impression.
     *
     * @param request the incoming OpenRTB BidRequest to forward
     * @return flat list of valid external candidates (may be empty)
     */
    public List<AuctionCandidate> fetchExternalCandidates(BidRequest request) {
        List<DspEndpoint> dsps = dspProperties.getDsps();
        if (dsps.isEmpty()) {
            return List.of();
        }

        log.debug("Forwarding bid request id={} to {} DSP(s)", request.id(), dsps.size());

        // Fire all DSP calls concurrently
        Map<DspEndpoint, CompletableFuture<List<AuctionCandidate>>> futures = new LinkedHashMap<>();
        for (DspEndpoint dsp : dsps) {
            CompletableFuture<List<AuctionCandidate>> future =
                    CompletableFuture.supplyAsync(() -> callDsp(dsp, request), executor);
            futures.put(dsp, future);
        }

        // Collect results, respecting per-DSP timeouts
        List<AuctionCandidate> allCandidates = new ArrayList<>();
        for (Map.Entry<DspEndpoint, CompletableFuture<List<AuctionCandidate>>> entry : futures.entrySet()) {
            DspEndpoint dsp = entry.getKey();
            try {
                List<AuctionCandidate> candidates =
                        entry.getValue().get(dsp.getTimeoutMs(), TimeUnit.MILLISECONDS);
                log.debug("DSP {} returned {} valid candidates", dsp.getId(), candidates.size());
                allCandidates.addAll(candidates);
            } catch (TimeoutException e) {
                log.warn("DSP {} timed out after {}ms — skipping", dsp.getId(), dsp.getTimeoutMs());
                entry.getValue().cancel(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for DSP {} — skipping", dsp.getId());
            } catch (ExecutionException e) {
                log.warn("DSP {} returned an error — skipping: {}", dsp.getId(), e.getCause().getMessage());
            }
        }

        log.info("External DSPs contributed {} total candidates for request id={}",
                allCandidates.size(), request.id());
        return Collections.unmodifiableList(allCandidates);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    /**
     * Calls a single DSP synchronously (runs inside a virtual thread).
     * Returns a (possibly empty) list of valid AuctionCandidates.
     */
    private List<AuctionCandidate> callDsp(DspEndpoint dsp, BidRequest request) {
        log.debug("Calling DSP {} at {}", dsp.getId(), dsp.getEndpoint());
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            HttpEntity<BidRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<BidResponse> response = restTemplate.exchange(
                    dsp.getEndpoint(), HttpMethod.POST, entity, BidResponse.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.debug("DSP {} returned HTTP {} with no body", dsp.getId(), response.getStatusCode());
                return List.of();
            }

            BidResponse bidResponse = response.getBody();
            return extractValidCandidates(bidResponse, dsp, request);

        } catch (RestClientException e) {
            log.warn("HTTP error calling DSP {}: {}", dsp.getId(), e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.error("Unexpected error calling DSP {}", dsp.getId(), e);
            return List.of();
        }
    }

    /**
     * Extracts bids from a BidResponse, validates each one against the original
     * request (floor price, blocked cats/advs, basic sanity), and converts valid
     * bids to {@link AuctionCandidate}s.
     */
    private List<AuctionCandidate> extractValidCandidates(
            BidResponse bidResponse, DspEndpoint dsp, BidRequest request) {

        if (bidResponse.seatBid() == null || bidResponse.seatBid().isEmpty()) {
            return List.of();
        }

        // Build a quick lookup: impId → Imp for validation
        Map<String, Imp> impMap = request.imp().stream()
                .collect(Collectors.toMap(Imp::id, imp -> imp));

        List<AuctionCandidate> candidates = new ArrayList<>();

        for (SeatBid seatBid : bidResponse.seatBid()) {
            if (seatBid.bid() == null) continue;

            for (Bid bid : seatBid.bid()) {
                Imp imp = impMap.get(bid.impId());
                if (imp == null) {
                    log.debug("DSP {} bid references unknown impId={} — discarding", dsp.getId(), bid.impId());
                    continue;
                }
                if (!targetingService.isExternalBidEligible(bid, imp, request)) {
                    log.debug("DSP {} bid id={} failed eligibility check — discarding", dsp.getId(), bid.id());
                    continue;
                }
                candidates.add(AuctionCandidate.fromExternalBid(bid, dsp.getSeat()));
            }
        }
        return candidates;
    }
}
