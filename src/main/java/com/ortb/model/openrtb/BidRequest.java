package com.ortb.model.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * OpenRTB 2.6 Bid Request.
 * at: auction type — 1=First Price, 2=Second Price Plus
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BidRequest(
        @JsonProperty("id") @NotBlank String id,
        @JsonProperty("imp") @NotEmpty List<Imp> imp,
        @JsonProperty("site") Site site,
        @JsonProperty("app") App app,
        @JsonProperty("device") Device device,
        @JsonProperty("user") User user,
        @JsonProperty("at") Integer at,
        @JsonProperty("tmax") Integer tmax,
        @JsonProperty("wseat") List<String> wseat,
        @JsonProperty("allimps") Integer allImps,
        @JsonProperty("cur") List<String> cur,
        @JsonProperty("bcat") List<String> bcat,
        @JsonProperty("badv") List<String> badv,
        @JsonProperty("test") Integer test
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id;
        private List<Imp> imp;
        private Site site;
        private App app;
        private Device device;
        private User user;
        private Integer at = 1; // default: first-price auction
        private Integer tmax;
        private List<String> wseat, cur, bcat, badv;
        private Integer allImps, test;

        public Builder id(String id) { this.id = id; return this; }
        public Builder imp(List<Imp> imp) { this.imp = imp; return this; }
        public Builder site(Site site) { this.site = site; return this; }
        public Builder app(App app) { this.app = app; return this; }
        public Builder device(Device device) { this.device = device; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder at(Integer at) { this.at = at; return this; }
        public Builder tmax(Integer tmax) { this.tmax = tmax; return this; }
        public Builder bcat(List<String> bcat) { this.bcat = bcat; return this; }
        public Builder badv(List<String> badv) { this.badv = badv; return this; }
        public BidRequest build() {
            return new BidRequest(id, imp, site, app, device, user, at, tmax, wseat, allImps, cur, bcat, badv, test);
        }
    }
}
