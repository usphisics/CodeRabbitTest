package com.ortb.model.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record User(
        @JsonProperty("id") String id,
        @JsonProperty("buyeruid") String buyerUid,
        @JsonProperty("yob") Integer yob,
        @JsonProperty("gender") String gender,
        @JsonProperty("keywords") String keywords,
        @JsonProperty("customdata") String customData,
        @JsonProperty("geo") Geo geo,
        @JsonProperty("data") List<Data> data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("segment") List<Segment> segment
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Segment(
            @JsonProperty("id") String id,
            @JsonProperty("name") String name,
            @JsonProperty("value") String value
    ) {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id, buyerUid, gender, keywords, customData;
        private Integer yob;
        private Geo geo;
        private List<Data> data;

        public Builder id(String id) { this.id = id; return this; }
        public Builder buyerUid(String buyerUid) { this.buyerUid = buyerUid; return this; }
        public Builder yob(Integer yob) { this.yob = yob; return this; }
        public Builder gender(String gender) { this.gender = gender; return this; }
        public Builder keywords(String keywords) { this.keywords = keywords; return this; }
        public Builder geo(Geo geo) { this.geo = geo; return this; }
        public User build() {
            return new User(id, buyerUid, yob, gender, keywords, customData, geo, data);
        }
    }
}
