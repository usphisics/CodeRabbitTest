package com.ortb.model.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Geo(
        @JsonProperty("lat") Double lat,
        @JsonProperty("lon") Double lon,
        @JsonProperty("type") Integer type,
        @JsonProperty("country") String country,
        @JsonProperty("region") String region,
        @JsonProperty("regionfips104") String regionFips104,
        @JsonProperty("metro") String metro,
        @JsonProperty("city") String city,
        @JsonProperty("zip") String zip,
        @JsonProperty("utcoffset") Integer utcOffset
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Double lat, lon;
        private Integer type;
        private String country, region, regionFips104, metro, city, zip;
        private Integer utcOffset;

        public Builder lat(Double lat) { this.lat = lat; return this; }
        public Builder lon(Double lon) { this.lon = lon; return this; }
        public Builder country(String country) { this.country = country; return this; }
        public Builder region(String region) { this.region = region; return this; }
        public Builder city(String city) { this.city = city; return this; }
        public Builder zip(String zip) { this.zip = zip; return this; }
        public Geo build() {
            return new Geo(lat, lon, type, country, region, regionFips104, metro, city, zip, utcOffset);
        }
    }
}
