package com.ortb.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ortb.model.auction.AuctionCandidate;
import com.ortb.model.openrtb.*;
import com.ortb.service.DspClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BidControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    /** Mock the DSP client so integration tests are not affected by external network calls. */
    @MockBean
    DspClientService dspClientService;

    @BeforeEach
    void stubDspClientToReturnEmpty() {
        // Default: DSPs return no bids — each test that needs DSP bids overrides this
        when(dspClientService.fetchExternalCandidates(any())).thenReturn(List.of());
    }

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /openrtb/bid returns 200 with winning bid for matching 300x250 banner")
    void bidReturnsWinningBid() throws Exception {
        BidRequest request = BidRequest.builder()
                .id("test-req-001")
                .imp(List.of(Imp.builder()
                        .id("imp1")
                        .banner(Banner.builder().w(300).h(250).build())
                        .build()))
                .device(Device.builder()
                        .ip("1.2.3.4")
                        .deviceType(2) // PC
                        .geo(Geo.builder().country("USA").build())
                        .build())
                .site(Site.builder()
                        .id("site-1")
                        .name("Test Site")
                        .domain("test.com")
                        .cat(List.of("IAB1"))
                        .build())
                .at(1)
                .build();

        MvcResult result = mockMvc.perform(post("/openrtb/bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        BidResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), BidResponse.class);

        assertThat(response.id()).isEqualTo("test-req-001");
        assertThat(response.seatBid()).isNotEmpty();
        assertThat(response.seatBid().getFirst().bid()).isNotEmpty();

        Bid winner = response.seatBid().getFirst().bid().getFirst();
        assertThat(winner.price()).isPositive();
        assertThat(winner.adm()).isNotBlank();
        assertThat(winner.w()).isEqualTo(300);
        assertThat(winner.h()).isEqualTo(250);
    }

    @Test
    @DisplayName("POST /openrtb/bid returns no-bid for size with no matching ads (1x1)")
    void noBidForUnmatchedSize() throws Exception {
        BidRequest request = BidRequest.builder()
                .id("test-req-002")
                .imp(List.of(Imp.builder()
                        .id("imp1")
                        .banner(Banner.builder().w(1).h(1).build())
                        .build()))
                .build();

        MvcResult result = mockMvc.perform(post("/openrtb/bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        BidResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), BidResponse.class);

        assertThat(response.seatBid()).isEmpty();
        assertThat(response.nbr()).isEqualTo(0);
    }

    @Test
    @DisplayName("POST /openrtb/bid returns 400 for malformed JSON")
    void returns400ForMalformedJson() throws Exception {
        mockMvc.perform(post("/openrtb/bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /openrtb/ads returns pre-loaded inventory")
    void listAdsReturnsInventory() throws Exception {
        mockMvc.perform(get("/openrtb/ads"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    @DisplayName("Second-price auction: winner pays second + 0.01")
    void secondPriceAuction() throws Exception {
        // Include site with IAB19 category + tech keywords so ad-003 (4.20) is also eligible.
        // Eligible 300x250 ads: ad-001 (5.50, unrestricted) and ad-003 (4.20, IAB19/tech keywords).
        // Winner: ad-001 at 5.50, clears at second price 4.20 + 0.01 = 4.21
        BidRequest request = BidRequest.builder()
                .id("test-req-sp")
                .imp(List.of(Imp.builder()
                        .id("imp1")
                        .banner(Banner.builder().w(300).h(250).build())
                        .build()))
                .site(Site.builder()
                        .cat(List.of("IAB19"))
                        .keywords("technology, gadgets")
                        .build())
                .at(2) // second-price
                .build();

        MvcResult result = mockMvc.perform(post("/openrtb/bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        BidResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), BidResponse.class);

        Bid winner = response.seatBid().getFirst().bid().getFirst();
        assertThat(winner.adId()).isEqualTo("ad-001");
        assertThat(winner.price()).isEqualTo(4.21); // second price (4.20) + 0.01
    }

    @Test
    @DisplayName("High-floor filters out cheaper ads")
    void highFloorFiltersCheapAds() throws Exception {
        BidRequest request = BidRequest.builder()
                .id("test-req-floor")
                .imp(List.of(Imp.builder()
                        .id("imp1")
                        .banner(Banner.builder().w(300).h(250).build())
                        .bidFloor(10.0) // floor above all 300x250 ads
                        .build()))
                .build();

        MvcResult result = mockMvc.perform(post("/openrtb/bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        BidResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), BidResponse.class);

        assertThat(response.seatBid()).isEmpty();
    }

    @Test
    @DisplayName("EU-only ad wins for EU request with matching category")
    void euAdWinsForEuRequest() throws Exception {
        BidRequest request = BidRequest.builder()
                .id("test-req-eu")
                .imp(List.of(Imp.builder()
                        .id("imp1")
                        .banner(Banner.builder().w(300).h(250).build())
                        .build()))
                .device(Device.builder()
                        .geo(Geo.builder().country("GBR").build())
                        .build())
                .site(Site.builder()
                        .cat(List.of("IAB13"))
                        .build())
                .build();

        MvcResult result = mockMvc.perform(post("/openrtb/bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        BidResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), BidResponse.class);

        assertThat(response.seatBid()).isNotEmpty();
        // ad-006 (EU finance, 6.00) should win
        assertThat(response.seatBid().getFirst().bid().getFirst().adId()).isEqualTo("ad-006");
    }

    // -------------------------------------------------------------------------
    // DSP integration tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("External DSP bid wins when its price is higher than all local ads")
    void externalDspBidWinsOverLocalAds() throws Exception {
        // DSP offers 20.00 CPM for a 300x250 — higher than any local ad
        AuctionCandidate dspCandidate = new AuctionCandidate(
                "dsp-ad-999", "<div>DSP Ad</div>", 20.00,
                List.of("dsp-winner.com"), "dsp-crid-999",
                List.of("IAB1"), 300, 250,
                "https://dsp.example.com/win", "imp1", "seat-alpha");

        when(dspClientService.fetchExternalCandidates(any())).thenReturn(List.of(dspCandidate));

        BidRequest request = BidRequest.builder()
                .id("test-dsp-wins")
                .imp(List.of(Imp.builder()
                        .id("imp1")
                        .banner(Banner.builder().w(300).h(250).build())
                        .build()))
                .at(1)
                .build();

        MvcResult result = mockMvc.perform(post("/openrtb/bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        BidResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), BidResponse.class);

        Bid winner = response.seatBid().getFirst().bid().getFirst();
        assertThat(winner.adId()).isEqualTo("dsp-ad-999");
        assertThat(winner.price()).isEqualTo(20.00);
    }

    @Test
    @DisplayName("Local ad wins when its price is higher than external DSP bids")
    void localAdWinsOverDspBid() throws Exception {
        // DSP offers only 1.00 CPM — local ad-001 at 5.50 wins
        AuctionCandidate cheapDspCandidate = new AuctionCandidate(
                "dsp-cheap", "<div>Cheap DSP Ad</div>", 1.00,
                List.of("dsp-cheap.com"), "dsp-crid-cheap",
                List.of("IAB1"), 300, 250,
                null, "imp1", "seat-beta");

        when(dspClientService.fetchExternalCandidates(any())).thenReturn(List.of(cheapDspCandidate));

        BidRequest request = BidRequest.builder()
                .id("test-local-wins")
                .imp(List.of(Imp.builder()
                        .id("imp1")
                        .banner(Banner.builder().w(300).h(250).build())
                        .build()))
                .at(1)
                .build();

        MvcResult result = mockMvc.perform(post("/openrtb/bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        BidResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), BidResponse.class);

        Bid winner = response.seatBid().getFirst().bid().getFirst();
        // ad-001 (5.50) beats the 1.00 DSP bid
        assertThat(winner.adId()).isEqualTo("ad-001");
        assertThat(winner.price()).isEqualTo(5.50);
    }

    @Test
    @DisplayName("Second-price auction with external DSP: winner pays DSP price + 0.01 when DSP is runner-up")
    void secondPriceWithDspAsRunnerUp() throws Exception {
        // Local ad-001 wins at 5.50; DSP offers 4.80 → clearing price = 4.81
        AuctionCandidate dspRunnerUp = new AuctionCandidate(
                "dsp-runner", "<div>Runner Up DSP</div>", 4.80,
                List.of("dsp-runner.com"), "crid-runner",
                List.of("IAB1"), 300, 250,
                null, "imp1", "seat-gamma");

        when(dspClientService.fetchExternalCandidates(any())).thenReturn(List.of(dspRunnerUp));

        BidRequest request = BidRequest.builder()
                .id("test-sp-dsp-runnerup")
                .imp(List.of(Imp.builder()
                        .id("imp1")
                        .banner(Banner.builder().w(300).h(250).build())
                        .build()))
                .at(2) // second-price
                .build();

        MvcResult result = mockMvc.perform(post("/openrtb/bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        BidResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), BidResponse.class);

        Bid winner = response.seatBid().getFirst().bid().getFirst();
        assertThat(winner.adId()).isEqualTo("ad-001"); // local 5.50 wins
        assertThat(winner.price()).isEqualTo(4.81);    // 4.80 (DSP) + 0.01
    }

    @Test
    @DisplayName("DSP wins second-price auction with local ad as runner-up")
    void secondPriceWithDspWinnerLocalRunnerUp() throws Exception {
        // DSP bids 10.00; highest local 300x250 is ad-001 at 5.50 → clearing = 5.51
        AuctionCandidate dspWinner = new AuctionCandidate(
                "dsp-top", "<div>Premium DSP Ad</div>", 10.00,
                List.of("dsp-premium.com"), "crid-top",
                List.of("IAB1"), 300, 250,
                "https://dsp.example.com/win", "imp1", "seat-alpha");

        when(dspClientService.fetchExternalCandidates(any())).thenReturn(List.of(dspWinner));

        BidRequest request = BidRequest.builder()
                .id("test-sp-dsp-wins")
                .imp(List.of(Imp.builder()
                        .id("imp1")
                        .banner(Banner.builder().w(300).h(250).build())
                        .build()))
                .at(2) // second-price
                .build();

        MvcResult result = mockMvc.perform(post("/openrtb/bid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        BidResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), BidResponse.class);

        Bid winner = response.seatBid().getFirst().bid().getFirst();
        assertThat(winner.adId()).isEqualTo("dsp-top");
        assertThat(winner.price()).isEqualTo(5.51); // local runner-up 5.50 + 0.01
    }
}
