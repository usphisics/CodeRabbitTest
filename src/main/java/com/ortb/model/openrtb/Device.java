package com.ortb.model.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OpenRTB 2.6 Device object.
 * devicetype: 1=Mobile/Tablet, 2=Personal Computer, 3=Connected TV,
 *             4=Phone, 5=Tablet, 6=Connected Device, 7=Set Top Box
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Device(
        @JsonProperty("ua") String ua,
        @JsonProperty("geo") Geo geo,
        @JsonProperty("dnt") Integer dnt,
        @JsonProperty("lmt") Integer lmt,
        @JsonProperty("ip") String ip,
        @JsonProperty("ipv6") String ipv6,
        @JsonProperty("devicetype") Integer deviceType,
        @JsonProperty("make") String make,
        @JsonProperty("model") String model,
        @JsonProperty("os") String os,
        @JsonProperty("osv") String osv,
        @JsonProperty("hwv") String hwv,
        @JsonProperty("h") Integer h,
        @JsonProperty("w") Integer w,
        @JsonProperty("ppi") Integer ppi,
        @JsonProperty("js") Integer js,
        @JsonProperty("language") String language,
        @JsonProperty("carrier") String carrier,
        @JsonProperty("connectiontype") Integer connectionType
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String ua, ip, ipv6, make, model, os, osv, hwv, language, carrier;
        private Geo geo;
        private Integer dnt, lmt, deviceType, h, w, ppi, js, connectionType;

        public Builder ua(String ua) { this.ua = ua; return this; }
        public Builder geo(Geo geo) { this.geo = geo; return this; }
        public Builder ip(String ip) { this.ip = ip; return this; }
        public Builder deviceType(Integer deviceType) { this.deviceType = deviceType; return this; }
        public Builder make(String make) { this.make = make; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder os(String os) { this.os = os; return this; }
        public Builder language(String language) { this.language = language; return this; }
        public Device build() {
            return new Device(ua, geo, dnt, lmt, ip, ipv6, deviceType, make, model, os, osv, hwv, h, w, ppi, js, language, carrier, connectionType);
        }
    }
}
