package com.ortb.model.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OpenRTB 2.6 Bid Response.
 * nbr (no-bid reason): 0=Unknown, 1=Technical Error, 2=Invalid Request, 3=Known Web Spider,
 *                      4=Suspected Non-Human Traffic, 5=Cloud/DC/Data Center, 6=Unsupported Device,
 *                      7=Blocked Publisher or Site, 8=Unmatched User
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BidResponse(
        @JsonProperty("id") String id,
        @JsonProperty("seatbid") List<SeatBid> seatBid,
        @JsonProperty("bidid") String bidId,
        @JsonProperty("cur") String cur,
        @JsonProperty("customdata") String customData,
        @JsonProperty("nbr") Integer nbr
) {
    public static BidResponse noBid(String requestId, Integer nbr) {
        return new BidResponse(requestId, List.of(), null, "USD", null, nbr);
    }

    public static BidResponse withBids(String requestId, List<SeatBid> seatBids) {
        return new BidResponse(requestId, seatBids, java.util.UUID.randomUUID().toString(), "USD", null, null);
    }
}
