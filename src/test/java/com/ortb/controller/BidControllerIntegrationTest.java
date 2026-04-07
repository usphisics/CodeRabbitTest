package com.ortb.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ortb.model.openrtb.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BidControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

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
}
