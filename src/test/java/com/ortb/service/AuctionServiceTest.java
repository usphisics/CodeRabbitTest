package com.ortb.service;

import com.ortb.model.ad.Ad;
import com.ortb.model.ad.AdFormat;
import com.ortb.model.ad.Targeting;
import com.ortb.model.auction.AuctionCandidate;
import com.ortb.model.openrtb.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    private AdInventoryService inventoryService;
    private AuctionService auctionService;

    @Mock
    private DspClientService dspClientService;

    @BeforeEach
    void setUp() {
        inventoryService = new AdInventoryService();
        // DSPs return no external bids by default — unit tests focus on local auction logic
        when(dspClientService.fetchExternalCandidates(any())).thenReturn(List.of());
        auctionService = new AuctionService(inventoryService, new TargetingService(), dspClientService);
    }

    @Test
    @DisplayName("Returns no-bid when inventory is empty")
    void noBidWhenEmpty() {
        BidRequest request = requestWith300x250Banner();
        BidResponse response = auctionService.process(request);

        assertThat(response.seatBid()).isEmpty();
        assertThat(response.nbr()).isEqualTo(0);
    }

    @Test
    @DisplayName("Returns winning bid for matching ad")
    void returnsWinningBid() {
        inventoryService.addAd(ad("ad-1", 300, 250, 3.0));
        BidRequest request = requestWith300x250Banner();
        BidResponse response = auctionService.process(request);

        assertThat(response.seatBid()).hasSize(1);
        assertThat(response.seatBid().getFirst().bid()).hasSize(1);
        assertThat(response.seatBid().getFirst().bid().getFirst().adId()).isEqualTo("ad-1");
        assertThat(response.seatBid().getFirst().bid().getFirst().price()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("First-price auction: winner pays own bid")
    void firstPriceAuction() {
        inventoryService.addAd(ad("ad-high", 300, 250, 5.0));
        inventoryService.addAd(ad("ad-low",  300, 250, 2.0));

        BidRequest request = BidRequest.builder()
                .id("req-fp")
                .imp(List.of(Imp.builder().id("imp1")
                        .banner(Banner.builder().w(300).h(250).build()).build()))
                .at(1) // first-price
                .build();

        BidResponse response = auctionService.process(request);
        Bid winner = response.seatBid().getFirst().bid().getFirst();

        assertThat(winner.adId()).isEqualTo("ad-high");
        assertThat(winner.price()).isEqualTo(5.0); // pays own price
    }

    @Test
    @DisplayName("Second-price auction: winner pays second-price + 0.01")
    void secondPriceAuction() {
        inventoryService.addAd(ad("ad-high", 300, 250, 5.0));
        inventoryService.addAd(ad("ad-low",  300, 250, 2.0));

        BidRequest request = BidRequest.builder()
                .id("req-sp")
                .imp(List.of(Imp.builder().id("imp1")
                        .banner(Banner.builder().w(300).h(250).build()).build()))
                .at(2) // second-price
                .build();

        BidResponse response = auctionService.process(request);
        Bid winner = response.seatBid().getFirst().bid().getFirst();

        assertThat(winner.adId()).isEqualTo("ad-high");
        assertThat(winner.price()).isEqualTo(2.01); // second price + 0.01
    }

    @Test
    @DisplayName("Highest priced eligible ad wins")
    void highestPriceWins() {
        inventoryService.addAd(ad("ad-3", 300, 250, 3.0));
        inventoryService.addAd(ad("ad-7", 300, 250, 7.0));
        inventoryService.addAd(ad("ad-5", 300, 250, 5.0));

        BidResponse response = auctionService.process(requestWith300x250Banner());
        Bid winner = response.seatBid().getFirst().bid().getFirst();

        assertThat(winner.adId()).isEqualTo("ad-7");
    }

    @Test
    @DisplayName("Ad below floor price is excluded from auction")
    void adBelowFloorExcluded() {
        inventoryService.addAd(ad("ad-cheap", 300, 250, 0.50));
        inventoryService.addAd(ad("ad-ok",    300, 250, 2.00));

        BidRequest request = BidRequest.builder()
                .id("req-floor")
                .imp(List.of(Imp.builder().id("imp1")
                        .banner(Banner.builder().w(300).h(250).build())
                        .bidFloor(1.50)
                        .build()))
                .build();

        BidResponse response = auctionService.process(request);
        Bid winner = response.seatBid().getFirst().bid().getFirst();

        assertThat(winner.adId()).isEqualTo("ad-ok");
    }

    @Test
    @DisplayName("Multiple impressions each get their own winning bid")
    void multipleImpressionsGetSeparateBids() {
        inventoryService.addAd(ad("ad-300", 300, 250, 3.0));
        inventoryService.addAd(ad("ad-728", 728, 90,  4.0));

        Imp imp1 = Imp.builder().id("imp1").banner(Banner.builder().w(300).h(250).build()).build();
        Imp imp2 = Imp.builder().id("imp2").banner(Banner.builder().w(728).h(90).build()).build();

        BidRequest request = BidRequest.builder()
                .id("req-multi")
                .imp(List.of(imp1, imp2))
                .build();

        BidResponse response = auctionService.process(request);
        List<Bid> bids = response.seatBid().getFirst().bid();

        assertThat(bids).hasSize(2);
        assertThat(bids.stream().map(Bid::adId)).containsExactlyInAnyOrder("ad-300", "ad-728");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Ad ad(String id, int w, int h, double price) {
        return Ad.builder()
                .id(id)
                .format(AdFormat.BANNER)
                .w(w).h(h)
                .price(price)
                .targeting(Targeting.unrestricted())
                .build();
    }

    private BidRequest requestWith300x250Banner() {
        return BidRequest.builder()
                .id("req-test")
                .imp(List.of(Imp.builder().id("imp1")
                        .banner(Banner.builder().w(300).h(250).build()).build()))
                .build();
    }
}
