package com.ortb.service;

import com.ortb.model.ad.Ad;
import com.ortb.model.openrtb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Runs the auction for a given BidRequest.
 *
 * Auction types (BidRequest.at):
 *  1 = First-price sealed bid  → winner pays their own bid price
 *  2 = Second-price plus       → winner pays second-highest price + $0.01
 *
 * For each impression in the request:
 *  1. Gather all ads from inventory
 *  2. Filter via TargetingService
 *  3. Sort eligible ads by price (descending)
 *  4. Apply auction pricing rule
 *  5. Build Bid object for the winner
 */
@Service
public class AuctionService {

    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);

    private final AdInventoryService inventoryService;
    private final TargetingService targetingService;

    public AuctionService(AdInventoryService inventoryService, TargetingService targetingService) {
        this.inventoryService = inventoryService;
        this.targetingService = targetingService;
    }

    /**
     * Processes the BidRequest and returns a BidResponse.
     * Returns a no-bid response (empty seatbid) when no eligible ads are found.
     */
    public BidResponse process(BidRequest request) {
        log.info("Processing bid request id={} impressions={}", request.id(), request.imp().size());

        List<Bid> winningBids = new ArrayList<>();

        for (Imp imp : request.imp()) {
            runAuctionForImpression(imp, request)
                    .ifPresent(winningBids::add);
        }

        if (winningBids.isEmpty()) {
            log.info("No-bid for request id={}", request.id());
            return BidResponse.noBid(request.id(), 0);
        }

        SeatBid seatBid = SeatBid.of(winningBids);
        return BidResponse.withBids(request.id(), List.of(seatBid));
    }

    // -------------------------------------------------------------------------

    private Optional<Bid> runAuctionForImpression(Imp imp, BidRequest request) {
        // Gather eligible ads
        List<Ad> eligible = inventoryService.getAllAds().stream()
                .filter(ad -> targetingService.isEligible(ad, imp, request))
                .sorted(Comparator.comparingDouble(Ad::price).reversed())
                .toList();

        if (eligible.isEmpty()) {
            log.debug("No eligible ads for impression id={}", imp.id());
            return Optional.empty();
        }

        Ad winner = eligible.getFirst();
        double clearingPrice = computeClearingPrice(winner, eligible, request.at());

        log.info("Auction winner for imp={}: adId={} price={} clearingPrice={}",
                imp.id(), winner.id(), winner.price(), clearingPrice);

        return Optional.of(buildBid(winner, imp.id(), clearingPrice));
    }

    /**
     * Compute the actual clearing price based on auction type.
     *  at=1 (first-price): pay own bid
     *  at=2 (second-price+): pay second-highest + $0.01
     */
    private double computeClearingPrice(Ad winner, List<Ad> sorted, Integer auctionType) {
        int at = (auctionType == null) ? 1 : auctionType;
        return switch (at) {
            case 2 -> {
                // Second-price plus
                if (sorted.size() > 1) {
                    yield round2(sorted.get(1).price() + 0.01);
                }
                yield winner.price(); // only one bidder → pays own price
            }
            default -> winner.price(); // First-price
        };
    }

    private Bid buildBid(Ad ad, String impId, double clearingPrice) {
        return Bid.builder()
                .id(UUID.randomUUID().toString())
                .impId(impId)
                .price(clearingPrice)
                .adId(ad.id())
                .adm(ad.adm())
                .nurl(ad.nurl())
                .adomain(ad.adomain())
                .crid(ad.crid())
                .cat(ad.cat())
                .w(ad.w())
                .h(ad.h())
                .build();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
