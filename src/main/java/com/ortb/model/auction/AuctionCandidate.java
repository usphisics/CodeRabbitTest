package com.ortb.model.auction;

import com.ortb.model.ad.Ad;
import com.ortb.model.openrtb.Bid;

import java.util.List;
import java.util.UUID;

/**
 * Normalized auction entry — represents either a local {@link Ad} or an external
 * DSP {@link Bid} in a form the auction engine can compare purely by price.
 *
 * @param adId     Creative / ad identifier
 * @param adm      Ad markup (HTML banner, VAST XML, etc.)
 * @param price    CPM price offered by this candidate
 * @param adomain  Advertiser domain(s)
 * @param crid     Creative ID
 * @param cat      IAB content categories
 * @param w        Creative width
 * @param h        Creative height
 * @param nurl     Win-notice URL (called after auction win)
 * @param impId    Impression ID this candidate is bidding on
 * @param source   "local" for inventory ads, or the DSP seat ID for external bids
 */
public record AuctionCandidate(
        String adId,
        String adm,
        double price,
        List<String> adomain,
        String crid,
        List<String> cat,
        int w,
        int h,
        String nurl,
        String impId,
        String source
) {
    /**
     * Builds the final OpenRTB {@link Bid} using the supplied clearing price
     * (which may differ from {@link #price()} in second-price auctions).
     */
    public Bid toBid(double clearingPrice) {
        return Bid.builder()
                .id(UUID.randomUUID().toString())
                .impId(impId)
                .price(clearingPrice)
                .adId(adId)
                .adm(adm)
                .nurl(nurl)
                .adomain(adomain)
                .crid(crid)
                .cat(cat)
                .w(w)
                .h(h)
                .build();
    }

    /** Creates a candidate from an ad in the local inventory. */
    public static AuctionCandidate fromLocalAd(Ad ad, String impId) {
        return new AuctionCandidate(
                ad.id(),
                ad.adm(),
                ad.price(),
                ad.adomain(),
                ad.crid(),
                ad.cat(),
                ad.w(),
                ad.h(),
                ad.nurl(),
                impId,
                "local"
        );
    }

    /**
     * Creates a candidate from a bid returned by an external DSP.
     *
     * @param bid  The {@link Bid} object from the DSP's BidResponse
     * @param seat The DSP's seat identifier (used for attribution)
     */
    public static AuctionCandidate fromExternalBid(Bid bid, String seat) {
        return new AuctionCandidate(
                bid.adId() != null ? bid.adId() : bid.id(),
                bid.adm(),
                // Coalesce null price to 0.0 to avoid NPE when unboxing a null Double
                bid.price() != null ? bid.price() : 0.0,
                bid.adomain(),
                bid.crid(),
                bid.cat(),
                bid.w() != null ? bid.w() : 0,
                bid.h() != null ? bid.h() : 0,
                bid.nurl(),
                bid.impId(),
                seat
        );
    }
}
