package com.ortb.service;

import com.ortb.model.ad.Ad;
import com.ortb.model.ad.AdFormat;
import com.ortb.model.ad.Targeting;
import com.ortb.model.openrtb.Bid;
import com.ortb.model.openrtb.BidRequest;
import com.ortb.model.openrtb.Device;
import com.ortb.model.openrtb.Geo;
import com.ortb.model.openrtb.Imp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Evaluates whether an Ad is eligible for a given impression/request combination.
 *
 * Filters applied (in order):
 *  1. Format compatibility (banner/video)
 *  2. Size compatibility (banner dimensions vs. ad creative dimensions)
 *  3. Floor price (ad price must meet or exceed the impression bid floor)
 *  4. Blocked categories (bcat in BidRequest vs. ad categories)
 *  5. Blocked advertisers (badv in BidRequest vs. ad domain)
 *  6. Country targeting
 *  7. Region targeting
 *  8. Device type targeting
 *  9. Content category targeting
 * 10. Keyword targeting
 */
@Service
public class TargetingService {

    private static final Logger log = LoggerFactory.getLogger(TargetingService.class);

    /**
     * Returns true if the ad is eligible for this impression within the bid request context.
     */
    public boolean isEligible(Ad ad, Imp imp, BidRequest request) {
        if (!matchesFormat(ad, imp)) {
            log.debug("Ad {} filtered: format mismatch", ad.id());
            return false;
        }
        if (!matchesSize(ad, imp)) {
            log.debug("Ad {} filtered: size mismatch", ad.id());
            return false;
        }
        if (!meetsFloorPrice(ad, imp)) {
            log.debug("Ad {} filtered: below floor price {}", ad.id(), imp.bidFloor());
            return false;
        }
        if (!passesBlockedCategories(ad, request)) {
            log.debug("Ad {} filtered: blocked category", ad.id());
            return false;
        }
        if (!passesBlockedAdvertisers(ad, request)) {
            log.debug("Ad {} filtered: blocked advertiser domain", ad.id());
            return false;
        }

        Targeting t = ad.targeting();
        Device device = request.device();
        Geo geo = (device != null) ? device.geo() : null;

        if (!matchesCountry(t, geo)) {
            log.debug("Ad {} filtered: country targeting", ad.id());
            return false;
        }
        if (!matchesRegion(t, geo)) {
            log.debug("Ad {} filtered: region targeting", ad.id());
            return false;
        }
        if (!matchesDeviceType(t, device)) {
            log.debug("Ad {} filtered: device type targeting", ad.id());
            return false;
        }
        if (!matchesCategories(t, request)) {
            log.debug("Ad {} filtered: category targeting", ad.id());
            return false;
        }
        if (!matchesKeywords(t, request)) {
            log.debug("Ad {} filtered: keyword targeting", ad.id());
            return false;
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Format & size
    // -------------------------------------------------------------------------

    private boolean matchesFormat(Ad ad, Imp imp) {
        return switch (ad.format()) {
            case BANNER -> imp.banner() != null;
            case VIDEO  -> imp.video() != null;
            case NATIVE -> imp.banner() == null && imp.video() == null; // simplification
        };
    }

    private boolean matchesSize(Ad ad, Imp imp) {
        if (ad.format() != AdFormat.BANNER || imp.banner() == null) return true;

        var banner = imp.banner();

        // If explicit w/h set on banner, match directly
        if (banner.w() != null && banner.h() != null) {
            return ad.w() == banner.w() && ad.h() == banner.h();
        }

        // If format list provided, check if any format matches
        if (banner.format() != null && !banner.format().isEmpty()) {
            return banner.format().stream()
                    .anyMatch(f -> f.w() != null && f.h() != null
                            && f.w() == ad.w() && f.h() == ad.h());
        }

        return true; // no size restriction
    }

    // -------------------------------------------------------------------------
    // Floor price
    // -------------------------------------------------------------------------

    private boolean meetsFloorPrice(Ad ad, Imp imp) {
        if (imp.bidFloor() == null || imp.bidFloor() <= 0) return true;
        return ad.price() >= imp.bidFloor();
    }

    // -------------------------------------------------------------------------
    // Blocked categories / advertisers (request-level)
    // -------------------------------------------------------------------------

    private boolean passesBlockedCategories(Ad ad, BidRequest request) {
        if (request.bcat() == null || request.bcat().isEmpty()) return true;
        if (ad.cat() == null || ad.cat().isEmpty()) return true;
        Set<String> blocked = Set.copyOf(request.bcat());
        return ad.cat().stream().noneMatch(blocked::contains);
    }

    private boolean passesBlockedAdvertisers(Ad ad, BidRequest request) {
        if (request.badv() == null || request.badv().isEmpty()) return true;
        if (ad.adomain() == null || ad.adomain().isEmpty()) return true;
        Set<String> blocked = Set.copyOf(request.badv());
        return ad.adomain().stream().noneMatch(blocked::contains);
    }

    // -------------------------------------------------------------------------
    // Geo targeting
    // -------------------------------------------------------------------------

    private boolean matchesCountry(Targeting t, Geo geo) {
        if (t.countries() == null || t.countries().isEmpty()) return true;
        if (geo == null || geo.country() == null) return false;
        return t.countries().contains(geo.country().toUpperCase());
    }

    private boolean matchesRegion(Targeting t, Geo geo) {
        if (t.regions() == null || t.regions().isEmpty()) return true;
        if (geo == null || geo.region() == null) return false;
        return t.regions().contains(geo.region().toUpperCase());
    }

    // -------------------------------------------------------------------------
    // Device targeting
    // -------------------------------------------------------------------------

    private boolean matchesDeviceType(Targeting t, Device device) {
        if (t.deviceTypes() == null || t.deviceTypes().isEmpty()) return true;
        if (device == null || device.deviceType() == null) return false;
        return t.deviceTypes().contains(device.deviceType());
    }

    // -------------------------------------------------------------------------
    // Category targeting (match against site/app categories)
    // -------------------------------------------------------------------------

    private boolean matchesCategories(Targeting t, BidRequest request) {
        if (t.categories() == null || t.categories().isEmpty()) return true;

        List<String> requestCats = getRequestCategories(request);
        if (requestCats.isEmpty()) return false;

        return requestCats.stream().anyMatch(t.categories()::contains);
    }

    private List<String> getRequestCategories(BidRequest request) {
        if (request.site() != null && request.site().cat() != null) {
            return request.site().cat();
        }
        if (request.app() != null && request.app().cat() != null) {
            return request.app().cat();
        }
        return List.of();
    }

    // -------------------------------------------------------------------------
    // Keyword targeting
    // -------------------------------------------------------------------------

    private boolean matchesKeywords(Targeting t, BidRequest request) {
        if (t.keywords() == null || t.keywords().isEmpty()) return true;

        String requestKeywords = getRequestKeywords(request);
        if (requestKeywords == null || requestKeywords.isBlank()) return false;

        Set<String> reqKwSet = Arrays.stream(requestKeywords.split("[,\\s]+"))
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        return t.keywords().stream()
                .map(String::toLowerCase)
                .anyMatch(reqKwSet::contains);
    }

    private String getRequestKeywords(BidRequest request) {
        if (request.site() != null && request.site().keywords() != null) {
            return request.site().keywords();
        }
        if (request.app() != null && request.app().keywords() != null) {
            return request.app().keywords();
        }
        if (request.user() != null && request.user().keywords() != null) {
            return request.user().keywords();
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // External DSP bid validation
    // -------------------------------------------------------------------------

    /**
     * Validates a bid returned by an external DSP before it enters the auction.
     * External DSPs handle their own targeting; we only enforce the publisher-side
     * constraints that are non-negotiable from our perspective:
     * <ol>
     *   <li>Price must be positive</li>
     *   <li>impId must be present</li>
     *   <li>Price must meet the impression bid floor</li>
     *   <li>Ad categories must not appear in the request's blocked-category list</li>
     *   <li>Advertiser domains must not appear in the request's blocked-advertiser list</li>
     * </ol>
     */
    public boolean isExternalBidEligible(Bid bid, Imp imp, BidRequest request) {
        if (bid.price() == null || bid.price() <= 0) {
            log.debug("External bid {} rejected: invalid price {}", bid.id(), bid.price());
            return false;
        }
        if (bid.impId() == null || bid.impId().isBlank()) {
            log.debug("External bid {} rejected: missing impId", bid.id());
            return false;
        }
        if (!meetsFloorPrice(bid.price(), imp)) {
            log.debug("External bid {} rejected: price {} below floor {}", bid.id(), bid.price(), imp.bidFloor());
            return false;
        }
        if (!passesBlockedCategoriesForBid(bid, request)) {
            log.debug("External bid {} rejected: blocked category", bid.id());
            return false;
        }
        if (!passesBlockedAdvertisersForBid(bid, request)) {
            log.debug("External bid {} rejected: blocked advertiser", bid.id());
            return false;
        }
        return true;
    }

    private boolean meetsFloorPrice(double price, Imp imp) {
        if (imp.bidFloor() == null || imp.bidFloor() <= 0) return true;
        return price >= imp.bidFloor();
    }

    private boolean passesBlockedCategoriesForBid(Bid bid, BidRequest request) {
        if (request.bcat() == null || request.bcat().isEmpty()) return true;
        if (bid.cat() == null || bid.cat().isEmpty()) return true;
        Set<String> blocked = Set.copyOf(request.bcat());
        return bid.cat().stream().noneMatch(blocked::contains);
    }

    private boolean passesBlockedAdvertisersForBid(Bid bid, BidRequest request) {
        if (request.badv() == null || request.badv().isEmpty()) return true;
        if (bid.adomain() == null || bid.adomain().isEmpty()) return true;
        Set<String> blocked = Set.copyOf(request.badv());
        return bid.adomain().stream().noneMatch(blocked::contains);
    }
}
