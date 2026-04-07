package com.ortb.model.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OpenRTB 2.6 SeatBid — collection of bids from a buyer seat.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SeatBid(
        @JsonProperty("bid") List<Bid> bid,
        @JsonProperty("seat") String seat,
        @JsonProperty("group") Integer group
) {
    public static SeatBid of(List<Bid> bids) {
        return new SeatBid(bids, "default", 0);
    }
}
