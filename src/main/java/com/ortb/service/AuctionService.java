package com.ortb.service;

import com.ortb.model.auction.AuctionCandidate;
import com.ortb.model.openrtb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Stream;

/**
 * Unified auction engine.
 *
 * <p>For each impression in the incoming {@link BidRequest}:
 * <ol>
 *   <li>Gathers local ad candidates from the inventory (filtered by {@link TargetingService}).</li>
 *   <li>Gathers external candidates from all configured DSPs via {@link DspClientService}
 *       (DSP calls happen in parallel before this method is invoked; results are passed in).</li>
 *   <li>Merges all candidates, sorts by CPM price descending.</li>
 *   <li>Applies the auction pricing rule:</li>
 *   <ul>
 *     <li>{@code at=1} First-price: winner pays its own bid.</li>
 *     <li>{@code at=2} Second-price plus: winner pays second-highest price + $0.01.</li>
 *   </ul>
 *   <li>Returns a {@link BidResponse} containing the winning {@link Bid} per impression.</li>
 * </ol>
 */
@Service
public class AuctionService {

    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);

    private final AdInventoryService inventoryService;
    private final TargetingService targetingService;
    private final DspClientService dspClientService;

    public AuctionService(
            AdInventoryService inventoryService,
            TargetingService targetingService,
            DspClientService dspClientService) {
        this.inventoryService = inventoryService;
        this.targetingService = targetingService;
        this.dspClientService = dspClientService;
    }

    /**
     * Main entry point. Processes the bid request, runs the auction for each
     * impression, and returns the final bid response.
     */
    public BidResponse process(BidRequest request) {
        log.info("Processing bid request id={} impressions={}", request.id(), request.imp().size());

        // Fetch external DSP bids once for the whole request (parallel calls inside)
        List<AuctionCandidate> externalCandidates = dspClientService.fetchExternalCandidates(request);

        List<Bid> winningBids = new ArrayList<>();

        for (Imp imp : request.imp()) {
            runAuctionForImpression(imp, request, externalCandidates)
                    .ifPresent(winningBids::add);
        }

        if (winningBids.isEmpty()) {
            log.info("No-bid for request id={}", request.id());
            return BidResponse.noBid(request.id(), 0);
        }

        return BidResponse.withBids(request.id(), List.of(SeatBid.of(winningBids)));
    }

    // -------------------------------------------------------------------------
    // Per-impression auction
    // -------------------------------------------------------------------------

    private Optional<Bid> runAuctionForImpression(
            Imp imp, BidRequest request, List<AuctionCandidate> externalCandidates) {

        // Local candidates
        List<AuctionCandidate> local = inventoryService.getAllAds().stream()
                .filter(ad -> targetingService.isEligible(ad, imp, request))
                .map(ad -> AuctionCandidate.fromLocalAd(ad, imp.id()))
                .toList();

        // External candidates matching this impression
        List<AuctionCandidate> external = externalCandidates.stream()
                .filter(c -> imp.id().equals(c.impId()))
                .toList();

        // Merge and sort by price descending
        List<AuctionCandidate> allCandidates = Stream.concat(local.stream(), external.stream())
                .sorted(Comparator.comparingDouble(AuctionCandidate::price).reversed())
                .toList();

        if (allCandidates.isEmpty()) {
            log.debug("No eligible candidates for impression id={}", imp.id());
            return Optional.empty();
        }

        AuctionCandidate winner = allCandidates.getFirst();
        double clearingPrice = computeClearingPrice(winner, allCandidates, request.at());

        log.info("Auction winner for imp={}: adId={} source={} bidPrice={} clearingPrice={}",
                imp.id(), winner.adId(), winner.source(), winner.price(), clearingPrice);

        return Optional.of(winner.toBid(clearingPrice));
    }

    // -------------------------------------------------------------------------
    // Pricing
    // -------------------------------------------------------------------------

    /**
     * Determines the actual clearing price based on auction type.
     * <ul>
     *   <li>{@code at=1} First-price → winner pays own bid.</li>
     *   <li>{@code at=2} Second-price plus → winner pays second-highest + $0.01;
     *       if only one bidder, pays own price.</li>
     * </ul>
     */
    private double computeClearingPrice(
            AuctionCandidate winner, List<AuctionCandidate> sorted, Integer auctionType) {

        int at = (auctionType == null) ? 1 : auctionType;
        return switch (at) {
            case 2 -> sorted.size() > 1 ? round2(sorted.get(1).price() + 0.01) : winner.price();
            default -> winner.price(); // first-price
        };
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
