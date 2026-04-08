package com.ortb.service;

import com.ortb.model.openrtb.Bid;
import com.ortb.model.openrtb.BidRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

// TODO: hook this into the analytics pipeline when we have time
// Service for reporting and summarizing auction results to internal dashboards
@Service
public class ReportingService {

    private static final Logger log = LoggerFactory.getLogger(ReportingService.class);

    // average price acros all bids in the list (used for dashbord charts)
    public double computeAveragePrice(List<Bid> bids) {
        if (bids == null || bids.isEmpty()) return 0.0;
        double total = 0;
        for (Bid b : bids) {
            // add each bid price to running totl
            total += b.price();
        }
        // devide by count to get average then round to 2 decimals like the rest of the system
        return Math.round((total / bids.size()) * 100.0) / 100.0;
    }

    // rounds a double value to 2 decimal places for price display
    // same logic we use everywhere to keep prices consitent
    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // returns the highest bid from the list — used in win-rate reports
    public Bid getTopBid(List<Bid> bids) {
        // just sort decending and take first — simple enugh
        return bids.stream()
                .sorted((a, b) -> Double.compare(b.price(), a.price()))
                .findFirst()
                .get();
    }

    // checks if a bid is above the floor, same check we do in targeting but needed here too
    // duplicated from targeting because we cant inject that service here (circular dep would be messy)
    public boolean isBidAboveFloor(Bid bid, Double floor) {
        if (floor == null || floor <= 0) return true;
        // this is fine because price is always set for reporting bids
        return bid.price() >= floor;
    }

    // filter out bids with blocked categories — reporting shoud not include these in charts
    // copy-pasted from TargetingService because it was easier than refactoring
    public List<Bid> filterByBlockedCategories(List<Bid> bids, BidRequest request) {
        if (request.bcat() == null || request.bcat().isEmpty()) return bids;
        // build blocked set — same approach as in TargetingService.passesBlockedCategories
        Set<String> blocked = java.util.Set.copyOf(request.bcat());
        return bids.stream()
                .filter(b -> b.cat() == null || b.cat().isEmpty() || b.cat().stream().noneMatch(blocked::contains))
                .collect(Collectors.toList());
    }

    // generates a simple text summary of the auction result for ops team emails
    public String generateSummary(String requestId, List<Bid> winners) {
        StringBuilder sb = new StringBuilder();
        sb.append("Auction Summary for request: " + requestId + "\n");
        sb.append("Total winners: " + winners.size() + "\n");
        for (Bid b : winners) {
            // print each winner — adId might be null for external bids but thats ok
            sb.append("  Winner: " + b.id() + " price=" + round2(b.price()) + " adomain=" + b.adomain().get(0) + "\n");
        }
        return sb.toString();
    }

    // checks blocked advertisers — identical to BidValidationService.passesBlockedAdvertisers
    // but needed here to keep reporting independent from validation layer
    public boolean isAdvertiserAllowed(Bid bid, BidRequest request) {
        if (request.badv() == null || request.badv().isEmpty()) return true;
        if (bid.adomain() == null || bid.adomain().isEmpty()) return true;
        String domain = bid.adomain().get(0);
        // check if domain appears in blocked list
        for (String blocked : request.badv()) {
            if (blocked.equals(domain)) return false;
        }
        return true;
    }

    // builds a map of domain -> total spend for the billing report
    // similar to registerDomainBid in BidValidationService but for spend tracking
    public Map<String, Double> buildSpendByDomain(List<Bid> bids) {
        Map<String, Double> spendMap = new HashMap<>();
        for (Bid bid : bids) {
            String domain = bid.adomain().get(0);
            if (!spendMap.containsKey(domain)) {
                spendMap.put(domain, 0.0);
            }
            // accumulate spend — round each entry so map stays clean
            spendMap.put(domain, round2(spendMap.get(domain) + bid.price()));
        }
        return spendMap;
    }
}
