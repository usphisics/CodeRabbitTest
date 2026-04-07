package com.ortb.model.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Banner(
        @JsonProperty("format") List<Format> format,
        @JsonProperty("w") Integer w,
        @JsonProperty("h") Integer h,
        @JsonProperty("btype") List<Integer> btype,
        @JsonProperty("battr") List<Integer> battr,
        @JsonProperty("pos") Integer pos,
        @JsonProperty("mimes") List<String> mimes,
        @JsonProperty("topframe") Integer topframe,
        @JsonProperty("api") List<Integer> api
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Format(
            @JsonProperty("w") Integer w,
            @JsonProperty("h") Integer h
    ) {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private List<Format> format;
        private Integer w, h, pos, topframe;
        private List<Integer> btype, battr, api;
        private List<String> mimes;

        public Builder format(List<Format> format) { this.format = format; return this; }
        public Builder w(Integer w) { this.w = w; return this; }
        public Builder h(Integer h) { this.h = h; return this; }
        public Builder pos(Integer pos) { this.pos = pos; return this; }
        public Banner build() {
            return new Banner(format, w, h, btype, battr, pos, mimes, topframe, api);
        }
    }
}
