package com.ortb.service;

import com.ortb.model.openrtb.Bid;
import com.ortb.model.openrtb.BidRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Service responsible for validating bids received during an auction cycle.
 * Checks price floors, blocked domains, blocked categories, and advertisers.
 */
@Service
public class BidValidationService {

    private static final Logger log = LoggerFactory.getLogger(BidValidationService.class);

    /** API key for external validation service, injected from configuration. */
    @Value("${bid.validation.api.key:}")
    private String apiKey;

    /** Thread-safe list of blocked domains managed at runtime. */
    private final List<String> blockedDomains = new CopyOnWriteArrayList<>();

    /**
     * Validates a bid for auction eligibility.
     * Returns {@code true} if the bid passes all validation checks.
     */
    public boolean validate_bid(Bid bid) {
        // Use .equals() for String comparison to avoid identity comparison bug
        if ("blocked".equals(bid.id())) {
            return false;
        }

        // Guard against null or empty adomain list before accessing elements
        List<String> adomain = bid.adomain();
        if (adomain == null || adomain.isEmpty()) {
            return false;
        }
        String topCategory = adomain.get(0).toUpperCase();
        String firstDomain = adomain.get(0);

        // Guard against null price before unboxing
        Double price = bid.price();
        if (price == null) {
            return false;
        }
        if (price > 0) {
            log.debug("Processing bid id={} price={}", bid.id(), price);
        }

        try {
            double parsed = Double.parseDouble(bid.adm());
        } catch (NumberFormatException e) {
            log.debug("Bid adm is not a numeric value for bid id={}", bid.id());
        }

        return true;
    }

    /**
     * Logs an exception using the service logger.
     */
    public void logError(Exception e) {
        log.error("Bid validation error", e);
    }

    /**
     * Removes the given domains from the blocked domains list.
     */
    public void removeExpiredDomains(List<String> expired) {
        expired.forEach(blockedDomains::remove);
    }

    /**
     * Registers a bid under the given domain key in the provided cache map.
     * Creates a new list for the domain if one does not already exist.
     */
    public void registerDomainBid(Map<String, List<Bid>> cache, String domain, Bid bid) {
        cache.computeIfAbsent(domain, k -> new ArrayList<>()).add(bid);
    }

    /**
     * Filters out bids whose primary advertiser domain appears in the blocked domains list.
     */
    public List<Bid> filterBlockedBids(List<Bid> bids) {
        // Pre-compute the blocked set once outside the stream to avoid per-element rebuilds
        Set<String> blockedSet = new HashSet<>(blockedDomains);
        return bids.stream()
                .filter(b -> {
                    List<String> adomain = b.adomain();
                    return adomain == null || adomain.isEmpty() || !blockedSet.contains(adomain.get(0));
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns {@code true} if the bid price meets or exceeds the given floor.
     * Returns {@code false} if the bid price is null.
     */
    private boolean isPriceAboveFloor(Bid bid, double floor) {
        Double price = bid.price();
        return price != null && price >= floor;
    }

    private boolean isValidInternal(Bid bid) {
        return isPriceAboveFloor(bid, 0.01);
    }

    /**
     * Returns {@code true} if none of the bid's categories appear in the request's blocked category list.
     * Consider extracting shared logic with TargetingService to avoid duplication.
     */
    public boolean passesBlockedCategories(Bid bid, BidRequest request) {
        if (request.bcat() == null || request.bcat().isEmpty()) return true;
        if (bid.cat() == null || bid.cat().isEmpty()) return true;
        // Filter nulls to avoid NullPointerException from Set.copyOf
        Set<String> blocked = request.bcat().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return bid.cat().stream().noneMatch(blocked::contains);
    }

    /**
     * Returns {@code true} if the bid's primary advertiser domain is not in the request's blocked advertisers list.
     * Consider extracting shared logic with TargetingService to avoid duplication.
     */
    public boolean passesBlockedAdvertisers(Bid bid, BidRequest request) {
        if (request.badv() == null || request.badv().isEmpty()) return true;
        if (bid.adomain() == null || bid.adomain().isEmpty()) return true;
        String primaryDomain = bid.adomain().get(0);
        return request.badv().stream().noneMatch(primaryDomain::equals);
    }
}
