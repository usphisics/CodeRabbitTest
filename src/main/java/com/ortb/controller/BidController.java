package com.ortb.controller;

import com.ortb.model.ad.Ad;
import com.ortb.model.openrtb.BidRequest;
import com.ortb.model.openrtb.BidResponse;
import com.ortb.service.AdInventoryService;
import com.ortb.service.AuctionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * OpenRTB 2.6 Bid endpoint.
 *
 * POST /openrtb/bid  — receive a BidRequest, run auction, return BidResponse
 * GET  /openrtb/ads  — list all ads in inventory (admin/debug)
 */
@RestController
@RequestMapping("/openrtb")
public class BidController {

    private static final Logger log = LoggerFactory.getLogger(BidController.class);

    private final AuctionService auctionService;
    private final AdInventoryService inventoryService;

    public BidController(AuctionService auctionService, AdInventoryService inventoryService) {
        this.auctionService = auctionService;
        this.inventoryService = inventoryService;
    }

    /**
     * Receives an OpenRTB 2.6 BidRequest, runs the auction, and returns a BidResponse.
     *
     * <p>HTTP 200 with a populated seatbid = ad won the auction.<br>
     * HTTP 200 with empty seatbid = no eligible ad (no-bid).<br>
     * HTTP 204 is also acceptable per spec for no-bid, but we return 200 with nbr for observability.</p>
     */
    @PostMapping(
            value = "/bid",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<BidResponse> bid(@Valid @RequestBody BidRequest request) {
        log.info("Received bid request id={}", request.id());
        BidResponse response = auctionService.process(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns all ads currently in the inventory (for debugging/admin).
     */
    @GetMapping("/ads")
    public ResponseEntity<Collection<Ad>> listAds() {
        return ResponseEntity.ok(inventoryService.getAllAds());
    }

    /**
     * Returns a single ad by ID.
     */
    @GetMapping("/ads/{id}")
    public ResponseEntity<Ad> getAd(@PathVariable String id) {
        return inventoryService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
