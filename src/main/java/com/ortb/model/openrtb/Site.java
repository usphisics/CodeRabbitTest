package com.ortb.model.openrtb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Site(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("domain") String domain,
        @JsonProperty("cat") List<String> cat,
        @JsonProperty("sectioncat") List<String> sectionCat,
        @JsonProperty("pagecat") List<String> pageCat,
        @JsonProperty("page") String page,
        @JsonProperty("ref") String ref,
        @JsonProperty("search") String search,
        @JsonProperty("mobile") Integer mobile,
        @JsonProperty("privacypolicy") Integer privacyPolicy,
        @JsonProperty("publisher") Publisher publisher,
        @JsonProperty("keywords") String keywords
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id, name, domain, page, ref, search, keywords;
        private List<String> cat, sectionCat, pageCat;
        private Integer mobile, privacyPolicy;
        private Publisher publisher;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder domain(String domain) { this.domain = domain; return this; }
        public Builder cat(List<String> cat) { this.cat = cat; return this; }
        public Builder page(String page) { this.page = page; return this; }
        public Builder ref(String ref) { this.ref = ref; return this; }
        public Builder keywords(String keywords) { this.keywords = keywords; return this; }
        public Site build() {
            return new Site(id, name, domain, cat, sectionCat, pageCat, page, ref, search, mobile, privacyPolicy, publisher, keywords);
        }
    }
}
