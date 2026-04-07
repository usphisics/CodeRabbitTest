package com.ortb.model.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OpenRTB 2.6 Impression object.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Imp(
        @JsonProperty("id") String id,
        @JsonProperty("banner") Banner banner,
        @JsonProperty("video") Video video,
        @JsonProperty("instl") Integer instl,
        @JsonProperty("tagid") String tagId,
        @JsonProperty("bidfloor") Double bidFloor,
        @JsonProperty("bidfloorcur") String bidFloorCur,
        @JsonProperty("secure") Integer secure,
        @JsonProperty("iframebuster") List<String> iframeBuster,
        @JsonProperty("pmp") Pmp pmp
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pmp(
            @JsonProperty("private_auction") Integer privateAuction,
            @JsonProperty("deals") List<Deal> deals
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Deal(
            @JsonProperty("id") String id,
            @JsonProperty("bidfloor") Double bidFloor,
            @JsonProperty("bidfloorcur") String bidFloorCur,
            @JsonProperty("at") Integer at,
            @JsonProperty("wseat") List<String> wseat,
            @JsonProperty("wadomain") List<String> wadomain
    ) {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id, tagId, bidFloorCur;
        private Banner banner;
        private Video video;
        private Integer instl, secure;
        private Double bidFloor;
        private List<String> iframeBuster;
        private Pmp pmp;

        public Builder id(String id) { this.id = id; return this; }
        public Builder banner(Banner banner) { this.banner = banner; return this; }
        public Builder video(Video video) { this.video = video; return this; }
        public Builder tagId(String tagId) { this.tagId = tagId; return this; }
        public Builder bidFloor(Double bidFloor) { this.bidFloor = bidFloor; return this; }
        public Builder bidFloorCur(String bidFloorCur) { this.bidFloorCur = bidFloorCur; return this; }
        public Builder secure(Integer secure) { this.secure = secure; return this; }
        public Imp build() {
            return new Imp(id, banner, video, instl, tagId, bidFloor, bidFloorCur, secure, iframeBuster, pmp);
        }
    }
}
