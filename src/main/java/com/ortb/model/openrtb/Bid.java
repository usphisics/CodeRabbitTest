package com.ortb.model.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OpenRTB 2.6 Bid object — the individual bid for a given impression.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Bid(
        @JsonProperty("id") String id,
        @JsonProperty("impid") String impId,
        @JsonProperty("price") Double price,
        @JsonProperty("adid") String adId,
        @JsonProperty("nurl") String nurl,
        @JsonProperty("burl") String burl,
        @JsonProperty("lurl") String lurl,
        @JsonProperty("adm") String adm,
        @JsonProperty("adomain") List<String> adomain,
        @JsonProperty("bundle") String bundle,
        @JsonProperty("iurl") String iurl,
        @JsonProperty("cid") String cid,
        @JsonProperty("crid") String crid,
        @JsonProperty("cat") List<String> cat,
        @JsonProperty("attr") List<Integer> attr,
        @JsonProperty("api") Integer api,
        @JsonProperty("protocol") Integer protocol,
        @JsonProperty("qagmediarating") Integer qagMediaRating,
        @JsonProperty("language") String language,
        @JsonProperty("dealid") String dealId,
        @JsonProperty("w") Integer w,
        @JsonProperty("h") Integer h
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id, impId, adId, nurl, burl, lurl, adm, bundle, iurl, cid, crid, language, dealId;
        private Double price;
        private List<String> adomain, cat;
        private List<Integer> attr;
        private Integer api, protocol, qagMediaRating, w, h;

        public Builder id(String id) { this.id = id; return this; }
        public Builder impId(String impId) { this.impId = impId; return this; }
        public Builder price(Double price) { this.price = price; return this; }
        public Builder adId(String adId) { this.adId = adId; return this; }
        public Builder nurl(String nurl) { this.nurl = nurl; return this; }
        public Builder adm(String adm) { this.adm = adm; return this; }
        public Builder adomain(List<String> adomain) { this.adomain = adomain; return this; }
        public Builder crid(String crid) { this.crid = crid; return this; }
        public Builder cat(List<String> cat) { this.cat = cat; return this; }
        public Builder w(Integer w) { this.w = w; return this; }
        public Builder h(Integer h) { this.h = h; return this; }
        public Bid build() {
            return new Bid(id, impId, price, adId, nurl, burl, lurl, adm, adomain, bundle, iurl, cid, crid, cat, attr, api, protocol, qagMediaRating, language, dealId, w, h);
        }
    }
}
