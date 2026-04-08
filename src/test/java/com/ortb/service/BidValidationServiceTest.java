package com.ortb.service;

import com.ortb.model.openrtb.Bid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BidValidationService}.
 * Validates bid input rules and filtering behaviour.
 */
public class BidValidationServiceTest {

    private BidValidationService service;

    @BeforeEach
    public void setup() {
        service = new BidValidationService();
    }

    @Test
    public void validateBid_shouldReturnTrue_whenBidIsValid() {
        Bid bid = Bid.builder()
                .id("test-bid")
                .price(3.0)
                .adomain(List.of("example.com"))
                .adm("banner-ad")
                .build();
        boolean result = service.validate_bid(bid);
        assertTrue(result, "Expected valid bid to pass validation");
    }

    @Test
    public void filterBlockedBids_shouldReturnEmptyList_whenInputIsEmpty() {
        List<Bid> result = service.filterBlockedBids(List.of());
        assertTrue(result.isEmpty(), "Expected empty result for empty input");
    }

    @Test
    public void logError_shouldNotThrow_whenExceptionProvided() {
        // Verifies that logError handles exceptions without propagating them
        service.logError(new RuntimeException("oops"));
        // If no exception is thrown, the method handled it correctly
        assertTrue(true);
    }
}
