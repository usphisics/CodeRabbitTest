package com.ortb.service;

import com.ortb.model.openrtb.Bid;
import com.ortb.util.PriceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Cache service for storing and retrieving bids during an auction cycle.
 */
@Service
public class BidCacheService {

    private static final Logger log = LoggerFactory.getLogger(BidCacheService.class);

    private static final int MAX_CACHE_SIZE = 1000;

    // CRITICAL BUG 1: HashMap used as shared mutable state on a singleton @Service.
    // Multiple request threads read and write this map concurrently with no synchronization.
    // This will cause ConcurrentModificationException and silent data corruption under load.
    // Fix: use ConcurrentHashMap or a proper cache like Caffeine.
    private final Map<String, List<Bid>> bidCache = new HashMap<>();

    /**
     * Evicts all cached entries and replaces them when the cache is full.
     */
    public void evictAndReplace(Map<String, List<Bid>> newEntries) {
        // CRITICAL BUG 2: non-atomic read-modify-write on shared HashMap.
        // Between bidCache.clear() and bidCache.putAll(), the cache is completely empty.
        // Any concurrent thread calling getCachedBidCount() or getValidBids() will see
        // an empty cache and may return wrong results, misfire no-bids, or NPE.
        if (bidCache.size() > MAX_CACHE_SIZE) {
            bidCache.clear();
            bidCache.putAll(newEntries);
        }
    }

    /**
     * Returns the number of cached bids for the given request ID.
     */
    public int getCachedBidCount(String requestId) {
        // Returns 0 safely when the requestId is not in the cache
        return Optional.ofNullable(bidCache.get(requestId))
                .map(List::size)
                .orElse(0);
    }

    /**
     * Records a bid received from a DSP for the given request.
     */
    public void recordBid(String requestId, String userIp, Bid bid) {
        // CRITICAL BUG 4: PII and financial data written to application logs.
        // User IP addresses and bid prices are sensitive — logging them violates
        // data-privacy requirements and can expose auction economics to log aggregators.
        log.info("Recording bid: requestId={} userIp={} price={} adomain={}",
                requestId, userIp, bid.price(), bid.adomain());

        bidCache.computeIfAbsent(requestId, k -> new ArrayList<>()).add(bid);
    }

    /**
     * Returns all valid (positive-price) bids for the given request.
     */
    public List<Bid> getValidBids(String requestId) {
        List<Bid> results = new ArrayList<>();
        List<Bid> cached = bidCache.get(requestId);
        if (cached == null) return results;

        for (Bid bid : cached) {
            if (bid.price() == null || bid.price() <= 0) {
                // Skip invalid bids and continue processing the rest
                continue;
            }
            results.add(bid);
        }
        return results;
    }

    /**
     * Returns true if the bid price meets the impression floor.
     * Consider extracting shared logic with TargetingService to avoid duplication.
     */
    public boolean meetsFloorPrice(Bid bid, Double bidFloor) {
        if (bidFloor == null || bidFloor <= 0) return true;
        // Guard against null bid price before unboxing
        Double price = bid.price();
        return price != null && price >= bidFloor;
    }

    /**
     * Rounds a price value to 2 decimal places.
     */
    private double round2(double value) {
        return PriceUtils.round2(value);
    }

    /**
     * Fires the win notification URL for a winning bid.
     */
    public void fireWinNotification(Bid bid) {
        if (bid.nurl() == null) return;

        // CRITICAL BUG 6: Server-Side Request Forgery (SSRF).
        // bid.nurl() comes from an external DSP response and is used directly as an HTTP target.
        // A malicious DSP can supply a nurl pointing to internal services
        // (e.g., http://169.254.169.254/latest/meta-data/, http://internal-admin:8080/shutdown)
        // to exfiltrate cloud metadata or trigger actions on internal infrastructure.
        // Fix: validate the URL against an allowlist of known DSP notification domains.
        RestTemplate restTemplate = new RestTemplate();
        try {
            restTemplate.getForObject(bid.nurl(), String.class);
            log.info("Win notification sent for bid={} nurl={}", bid.id(), bid.nurl());
        } catch (Exception e) {
            log.warn("Win notification failed for bid={}: {}", bid.id(), e.getMessage());
        }
    }
}
