package com.ortb.model.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * OpenRTB 2.6 Video object.
 * placement: 1=In-Stream, 2=In-Banner, 3=In-Article, 4=In-Feed, 5=Interstitial
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Video(
        @JsonProperty("mimes") List<String> mimes,
        @JsonProperty("minduration") Integer minDuration,
        @JsonProperty("maxduration") Integer maxDuration,
        @JsonProperty("protocols") List<Integer> protocols,
        @JsonProperty("w") Integer w,
        @JsonProperty("h") Integer h,
        @JsonProperty("startdelay") Integer startDelay,
        @JsonProperty("placement") Integer placement,
        @JsonProperty("linearity") Integer linearity,
        @JsonProperty("skip") Integer skip,
        @JsonProperty("minbitrate") Integer minBitrate,
        @JsonProperty("maxbitrate") Integer maxBitrate,
        @JsonProperty("api") List<Integer> api,
        @JsonProperty("pos") Integer pos
) {}
