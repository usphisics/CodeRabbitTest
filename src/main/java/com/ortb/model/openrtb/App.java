package com.ortb.model.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record App(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("bundle") String bundle,
        @JsonProperty("domain") String domain,
        @JsonProperty("storeurl") String storeUrl,
        @JsonProperty("cat") List<String> cat,
        @JsonProperty("sectioncat") List<String> sectionCat,
        @JsonProperty("pagecat") List<String> pageCat,
        @JsonProperty("ver") String ver,
        @JsonProperty("privacypolicy") Integer privacyPolicy,
        @JsonProperty("paid") Integer paid,
        @JsonProperty("publisher") Publisher publisher,
        @JsonProperty("keywords") String keywords
) {}
