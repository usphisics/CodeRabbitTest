package com.ortb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ortb.config.DspProperties;
import com.ortb.model.auction.AuctionCandidate;
import com.ortb.model.openrtb.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class DspClientServiceTest {

    private static final String DSP_URL = "http://dsp-test.example.com/bid";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private DspClientService dspClientService;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.createServer(restTemplate);

        DspProperties props = dspPropertiesWith(DSP_URL, 200, "seat-test");

        dspClientService = new DspClientService(
                props,
                restTemplate,
                Executors.newVirtualThreadPerTaskExecutor(),
                new TargetingService());
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Returns candidates from DSP that responds with a valid bid")
    void returnsCandidatesFromSuccessfulDsp() throws Exception {
        BidRequest request = minimalRequest("imp1", 300, 250);
        BidResponse dspResponse = makeBidResponse("imp1", 4.50, 300, 250);

        mockServer.expect(requestTo(DSP_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(MAPPER.writeValueAsString(dspResponse), MediaType.APPLICATION_JSON));

        List<AuctionCandidate> candidates = dspClientService.fetchExternalCandidates(request);

        mockServer.verify();
        assertThat(candidates).hasSize(1);
        AuctionCandidate c = candidates.getFirst();
        assertThat(c.price()).isEqualTo(4.50);
        assertThat(c.impId()).isEqualTo("imp1");
        assertThat(c.source()).isEqualTo("seat-test");
        assertThat(c.w()).isEqualTo(300);
        assertThat(c.h()).isEqualTo(250);
    }

    @Test
    @DisplayName("Returns empty list when DSP sends no-bid (empty seatbid)")
    void returnsEmptyWhenDspNoBid() throws Exception {
        BidRequest request = minimalRequest("imp1", 300, 250);
        BidResponse noBid = BidResponse.noBid("req-1", 0);

        mockServer.expect(requestTo(DSP_URL))
                .andRespond(withSuccess(MAPPER.writeValueAsString(noBid), MediaType.APPLICATION_JSON));

        List<AuctionCandidate> candidates = dspClientService.fetchExternalCandidates(request);

        assertThat(candidates).isEmpty();
    }

    @Test
    @DisplayName("Discards DSP bid with price below impression floor")
    void discardsBidBelowFloorPrice() throws Exception {
        BidRequest request = BidRequest.builder()
                .id("req-1")
                .imp(List.of(Imp.builder().id("imp1")
                        .banner(Banner.builder().w(300).h(250).build())
                        .bidFloor(5.0)
                        .build()))
                .build();

        BidResponse dspResponse = makeBidResponse("imp1", 2.0, 300, 250);

        mockServer.expect(requestTo(DSP_URL))
                .andRespond(withSuccess(MAPPER.writeValueAsString(dspResponse), MediaType.APPLICATION_JSON));

        List<AuctionCandidate> candidates = dspClientService.fetchExternalCandidates(request);

        assertThat(candidates).isEmpty();
    }

    @Test
    @DisplayName("Discards DSP bid belonging to a blocked advertiser")
    void discardsBlockedAdvertiserBid() throws Exception {
        BidRequest request = BidRequest.builder()
                .id("req-1")
                .imp(List.of(Imp.builder().id("imp1")
                        .banner(Banner.builder().w(300).h(250).build()).build()))
                .badv(List.of("blocked-dsp.com"))
                .build();

        BidResponse dspResponse = bidResponseWithAdomain("imp1", 4.0, List.of("blocked-dsp.com"));

        mockServer.expect(requestTo(DSP_URL))
                .andRespond(withSuccess(MAPPER.writeValueAsString(dspResponse), MediaType.APPLICATION_JSON));

        List<AuctionCandidate> candidates = dspClientService.fetchExternalCandidates(request);

        assertThat(candidates).isEmpty();
    }

    @Test
    @DisplayName("Discards DSP bid for unknown impression ID")
    void discardsUnknownImpId() throws Exception {
        BidRequest request = minimalRequest("imp1", 300, 250);
        BidResponse dspResponse = makeBidResponse("unknown-imp", 4.0, 300, 250);

        mockServer.expect(requestTo(DSP_URL))
                .andRespond(withSuccess(MAPPER.writeValueAsString(dspResponse), MediaType.APPLICATION_JSON));

        List<AuctionCandidate> candidates = dspClientService.fetchExternalCandidates(request);

        assertThat(candidates).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Failure handling
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Returns empty list when DSP returns 5xx error")
    void returnsEmptyOnDspServerError() throws Exception {
        BidRequest request = minimalRequest("imp1", 300, 250);

        mockServer.expect(requestTo(DSP_URL))
                .andRespond(withServerError());

        List<AuctionCandidate> candidates = dspClientService.fetchExternalCandidates(request);

        assertThat(candidates).isEmpty();
    }

    @Test
    @DisplayName("Returns empty list when no DSPs are configured")
    void returnsEmptyWhenNoDspsConfigured() {
        DspProperties emptyProps = new DspProperties(); // empty dsps list
        DspClientService serviceWithNoDsps = new DspClientService(
                emptyProps, restTemplate,
                Executors.newVirtualThreadPerTaskExecutor(),
                new TargetingService());

        List<AuctionCandidate> candidates =
                serviceWithNoDsps.fetchExternalCandidates(minimalRequest("imp1", 300, 250));

        assertThat(candidates).isEmpty();
    }

    @Test
    @DisplayName("Multiple valid bids from one DSP response are all returned")
    void multipleValidBidsReturnedFromOneDsp() throws Exception {
        BidRequest request = BidRequest.builder()
                .id("req-1")
                .imp(List.of(
                        Imp.builder().id("imp1").banner(Banner.builder().w(300).h(250).build()).build(),
                        Imp.builder().id("imp2").banner(Banner.builder().w(728).h(90).build()).build()
                ))
                .build();

        // DSP responds with two bids — one per impression
        Bid bid1 = Bid.builder().id(UUID.randomUUID().toString()).impId("imp1").price(3.0).adm("<div>Ad1</div>").w(300).h(250).build();
        Bid bid2 = Bid.builder().id(UUID.randomUUID().toString()).impId("imp2").price(5.0).adm("<div>Ad2</div>").w(728).h(90).build();
        BidResponse dspResponse = BidResponse.withBids("req-1", List.of(SeatBid.of(List.of(bid1, bid2))));

        mockServer.expect(requestTo(DSP_URL))
                .andRespond(withSuccess(MAPPER.writeValueAsString(dspResponse), MediaType.APPLICATION_JSON));

        List<AuctionCandidate> candidates = dspClientService.fetchExternalCandidates(request);

        assertThat(candidates).hasSize(2);
        assertThat(candidates.stream().map(AuctionCandidate::impId))
                .containsExactlyInAnyOrder("imp1", "imp2");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private DspProperties dspPropertiesWith(String endpoint, int timeoutMs, String seat) {
        DspProperties.DspEndpoint dsp = new DspProperties.DspEndpoint();
        dsp.setId("test-dsp");
        dsp.setName("Test DSP");
        dsp.setEndpoint(endpoint);
        dsp.setTimeoutMs(timeoutMs);
        dsp.setSeat(seat);

        DspProperties props = new DspProperties();
        props.setDsps(List.of(dsp));
        return props;
    }

    private BidRequest minimalRequest(String impId, int w, int h) {
        return BidRequest.builder()
                .id("req-1")
                .imp(List.of(Imp.builder().id(impId)
                        .banner(Banner.builder().w(w).h(h).build()).build()))
                .build();
    }

    private BidResponse makeBidResponse(String impId, double price, int w, int h) {
        Bid bid = Bid.builder()
                .id(UUID.randomUUID().toString())
                .impId(impId)
                .price(price)
                .adm("<div>External Ad</div>")
                .w(w).h(h)
                .build();
        return BidResponse.withBids("req-1", List.of(SeatBid.of(List.of(bid))));
    }

    private BidResponse bidResponseWithAdomain(String impId, double price, List<String> adomain) {
        Bid bid = Bid.builder()
                .id(UUID.randomUUID().toString())
                .impId(impId)
                .price(price)
                .adomain(adomain)
                .adm("<div>External Ad</div>")
                .w(300).h(250)
                .build();
        return BidResponse.withBids("req-1", List.of(SeatBid.of(List.of(bid))));
    }
}
