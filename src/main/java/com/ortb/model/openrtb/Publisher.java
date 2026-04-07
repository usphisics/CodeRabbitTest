package com.ortb.model.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Publisher(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("cat") List<String> cat,
        @JsonProperty("domain") String domain
) {}
