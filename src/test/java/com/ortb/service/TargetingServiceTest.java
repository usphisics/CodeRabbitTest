package com.ortb.service;

import com.ortb.model.ad.Ad;
import com.ortb.model.ad.AdFormat;
import com.ortb.model.ad.Targeting;
import com.ortb.model.openrtb.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TargetingServiceTest {

    private TargetingService targetingService;

    @BeforeEach
    void setUp() {
        targetingService = new TargetingService();
    }

    // -------------------------------------------------------------------------
    // Format matching
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Banner ad matches impression with banner object")
    void bannerAdMatchesBannerImp() {
        Ad ad = bannerAd(300, 250, Targeting.unrestricted(), 1.0);
        Imp imp = Imp.builder().id("imp1").banner(Banner.builder().w(300).h(250).build()).build();
        BidRequest req = minimalRequest(imp);

        assertThat(targetingService.isEligible(ad, imp, req)).isTrue();
    }

    @Test
    @DisplayName("Banner ad does NOT match impression with video only")
    void bannerAdDoesNotMatchVideoImp() {
        Ad ad = bannerAd(300, 250, Targeting.unrestricted(), 1.0);
        Imp imp = Imp.builder().id("imp1")
                .video(new Video(List.of("video/mp4"), 5, 30, null, 640, 480, null, 1, null, null, null, null, null, null))
                .build();
        BidRequest req = minimalRequest(imp);

        assertThat(targetingService.isEligible(ad, imp, req)).isFalse();
    }

    // -------------------------------------------------------------------------
    // Size matching
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Ad size matches exact banner dimensions")
    void adSizeMatchesExact() {
        Ad ad = bannerAd(300, 250, Targeting.unrestricted(), 1.0);
        Imp imp = Imp.builder().id("imp1").banner(Banner.builder().w(300).h(250).build()).build();
        assertThat(targetingService.isEligible(ad, imp, minimalRequest(imp))).isTrue();
    }

    @Test
    @DisplayName("Ad size does NOT match different banner dimensions")
    void adSizeMismatch() {
        Ad ad = bannerAd(300, 250, Targeting.unrestricted(), 1.0);
        Imp imp = Imp.builder().id("imp1").banner(Banner.builder().w(728).h(90).build()).build();
        assertThat(targetingService.isEligible(ad, imp, minimalRequest(imp))).isFalse();
    }

    @Test
    @DisplayName("Ad size matches one of the banner format list")
    void adSizeMatchesFormatList() {
        Ad ad = bannerAd(300, 250, Targeting.unrestricted(), 1.0);
        var formats = List.of(new Banner.Format(728, 90), new Banner.Format(300, 250));
        Imp imp = Imp.builder().id("imp1").banner(Banner.builder().format(formats).build()).build();
        assertThat(targetingService.isEligible(ad, imp, minimalRequest(imp))).isTrue();
    }

    // -------------------------------------------------------------------------
    // Floor price
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Ad price above floor is eligible")
    void aboveFloorEligible() {
        Ad ad = bannerAd(300, 250, Targeting.unrestricted(), 3.0);
        Imp imp = Imp.builder().id("imp1").banner(Banner.builder().w(300).h(250).build())
                .bidFloor(2.0).build();
        assertThat(targetingService.isEligible(ad, imp, minimalRequest(imp))).isTrue();
    }

    @Test
    @DisplayName("Ad price below floor is NOT eligible")
    void belowFloorNotEligible() {
        Ad ad = bannerAd(300, 250, Targeting.unrestricted(), 1.0);
        Imp imp = Imp.builder().id("imp1").banner(Banner.builder().w(300).h(250).build())
                .bidFloor(2.0).build();
        assertThat(targetingService.isEligible(ad, imp, minimalRequest(imp))).isFalse();
    }

    // -------------------------------------------------------------------------
    // Blocked categories
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Ad with blocked category is NOT eligible")
    void blockedCategoryFiltered() {
        Ad ad = Ad.builder().id("a").format(AdFormat.BANNER).w(300).h(250).price(1.0)
                .cat(List.of("IAB25")).targeting(Targeting.unrestricted()).build();
        Imp imp = Imp.builder().id("imp1").banner(Banner.builder().w(300).h(250).build()).build();
        BidRequest req = BidRequest.builder().id("r1").imp(List.of(imp))
                .bcat(List.of("IAB25")).build();
        assertThat(targetingService.isEligible(ad, imp, req)).isFalse();
    }

    // -------------------------------------------------------------------------
    // Geo targeting
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Ad targeting USA matches US request")
    void countryTargetMatch() {
        Targeting t = Targeting.builder().countries(Set.of("USA")).build();
        Ad ad = bannerAd(300, 250, t, 1.0);
        Imp imp = Imp.builder().id("imp1").banner(Banner.builder().w(300).h(250).build()).build();
        Device device = Device.builder().geo(Geo.builder().country("USA").build()).build();
        BidRequest req = BidRequest.builder().id("r1").imp(List.of(imp)).device(device).build();
        assertThat(targetingService.isEligible(ad, imp, req)).isTrue();
    }

    @Test
    @DisplayName("Ad targeting USA does NOT match UK request")
    void countryTargetNoMatch() {
        Targeting t = Targeting.builder().countries(Set.of("USA")).build();
        Ad ad = bannerAd(300, 250, t, 1.0);
        Imp imp = Imp.builder().id("imp1").banner(Banner.builder().w(300).h(250).build()).build();
        Device device = Device.builder().geo(Geo.builder().country("GBR").build()).build();
        BidRequest req = BidRequest.builder().id("r1").imp(List.of(imp)).device(device).build();
        assertThat(targetingService.isEligible(ad, imp, req)).isFalse();
    }

    // -------------------------------------------------------------------------
    // Device type targeting
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Ad targeting mobile matches mobile device")
    void deviceTypeMatch() {
        Targeting t = Targeting.builder().deviceTypes(Set.of(4, 5)).build();
        Ad ad = bannerAd(300, 250, t, 1.0);
        Imp imp = Imp.builder().id("imp1").banner(Banner.builder().w(300).h(250).build()).build();
        Device device = Device.builder().deviceType(4).build();
        BidRequest req = BidRequest.builder().id("r1").imp(List.of(imp)).device(device).build();
        assertThat(targetingService.isEligible(ad, imp, req)).isTrue();
    }

    @Test
    @DisplayName("Ad targeting mobile does NOT match desktop device")
    void deviceTypeNoMatch() {
        Targeting t = Targeting.builder().deviceTypes(Set.of(4, 5)).build();
        Ad ad = bannerAd(300, 250, t, 1.0);
        Imp imp = Imp.builder().id("imp1").banner(Banner.builder().w(300).h(250).build()).build();
        Device device = Device.builder().deviceType(2).build(); // PC
        BidRequest req = BidRequest.builder().id("r1").imp(List.of(imp)).device(device).build();
        assertThat(targetingService.isEligible(ad, imp, req)).isFalse();
    }

    // -------------------------------------------------------------------------
    // Keyword targeting
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Ad keyword matches site keywords")
    void keywordMatch() {
        Targeting t = Targeting.builder().keywords(List.of("technology", "gadgets")).build();
        Ad ad = bannerAd(300, 250, t, 1.0);
        Imp imp = Imp.builder().id("imp1").banner(Banner.builder().w(300).h(250).build()).build();
        Site site = Site.builder().keywords("sports, technology, health").build();
        BidRequest req = BidRequest.builder().id("r1").imp(List.of(imp)).site(site).build();
        assertThat(targetingService.isEligible(ad, imp, req)).isTrue();
    }

    @Test
    @DisplayName("Ad keyword does NOT match when no overlap")
    void keywordNoMatch() {
        Targeting t = Targeting.builder().keywords(List.of("technology", "gadgets")).build();
        Ad ad = bannerAd(300, 250, t, 1.0);
        Imp imp = Imp.builder().id("imp1").banner(Banner.builder().w(300).h(250).build()).build();
        Site site = Site.builder().keywords("sports, health, fitness").build();
        BidRequest req = BidRequest.builder().id("r1").imp(List.of(imp)).site(site).build();
        assertThat(targetingService.isEligible(ad, imp, req)).isFalse();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Ad bannerAd(int w, int h, Targeting targeting, double price) {
        return Ad.builder()
                .id("test-ad")
                .format(AdFormat.BANNER)
                .w(w).h(h)
                .price(price)
                .targeting(targeting)
                .build();
    }

    private BidRequest minimalRequest(Imp imp) {
        return BidRequest.builder().id("req1").imp(List.of(imp)).build();
    }
}
