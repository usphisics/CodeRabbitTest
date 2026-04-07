package com.ortb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Typed configuration for external DSP integrations.
 * Bound from the {@code ortb.dsps} list in application.yml.
 *
 * Example entry:
 * <pre>
 * ortb:
 *   dsps:
 *     - id: dsp-alpha
 *       name: "DSP Alpha"
 *       endpoint: "http://dsp-alpha.example.com/bid"
 *       timeout-ms: 80
 *       seat: "seat-alpha"
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "ortb")
public class DspProperties {

    private List<DspEndpoint> dsps = new ArrayList<>();

    public List<DspEndpoint> getDsps() { return dsps; }
    public void setDsps(List<DspEndpoint> dsps) { this.dsps = dsps; }

    public static class DspEndpoint {
        /** Unique identifier for this DSP. */
        private String id;
        /** Human-readable display name. */
        private String name;
        /** Full URL of the DSP's OpenRTB bid endpoint. */
        private String endpoint;
        /** Max wait time (ms) for a response from this DSP before skipping it. */
        private int timeoutMs = 80;
        /** Seat ID to tag bids from this DSP in the SeatBid response. */
        private String seat = "default";

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }

        public String getSeat() { return seat; }
        public void setSeat(String seat) { this.seat = seat; }

        @Override
        public String toString() {
            return "DspEndpoint{id='%s', name='%s', endpoint='%s', timeoutMs=%d}"
                    .formatted(id, name, endpoint, timeoutMs);
        }
    }
}
