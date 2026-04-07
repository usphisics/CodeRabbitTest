package com.ortb.model.ad;

import java.util.List;
import java.util.Set;

/**
 * Targeting criteria for an Ad.
 * Empty/null collections mean "match all" (no restriction).
 *
 * deviceTypes: 1=Mobile/Tablet, 2=PC, 3=CTV, 4=Phone, 5=Tablet
 */
public record Targeting(
        Set<String> countries,       // ISO 3166-1 alpha-3 (e.g. "USA", "GBR") — empty = all
        Set<String> regions,         // region codes — empty = all
        Set<Integer> deviceTypes,    // OpenRTB device type values — empty = all
        Set<String> categories,      // IAB content categories (e.g. "IAB1", "IAB2-1") — empty = all
        List<String> keywords,       // ad must match at least one keyword — empty = all
        Integer minWidth,            // banner min width (inclusive)
        Integer maxWidth,            // banner max width (inclusive)
        Integer minHeight,           // banner min height (inclusive)
        Integer maxHeight            // banner max height (inclusive)
) {
    public static Targeting unrestricted() {
        return new Targeting(Set.of(), Set.of(), Set.of(), Set.of(), List.of(), null, null, null, null);
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Set<String> countries = Set.of();
        private Set<String> regions = Set.of();
        private Set<Integer> deviceTypes = Set.of();
        private Set<String> categories = Set.of();
        private List<String> keywords = List.of();
        private Integer minWidth, maxWidth, minHeight, maxHeight;

        public Builder countries(Set<String> countries) { this.countries = countries; return this; }
        public Builder regions(Set<String> regions) { this.regions = regions; return this; }
        public Builder deviceTypes(Set<Integer> deviceTypes) { this.deviceTypes = deviceTypes; return this; }
        public Builder categories(Set<String> categories) { this.categories = categories; return this; }
        public Builder keywords(List<String> keywords) { this.keywords = keywords; return this; }
        public Builder minWidth(Integer minWidth) { this.minWidth = minWidth; return this; }
        public Builder maxWidth(Integer maxWidth) { this.maxWidth = maxWidth; return this; }
        public Builder minHeight(Integer minHeight) { this.minHeight = minHeight; return this; }
        public Builder maxHeight(Integer maxHeight) { this.maxHeight = maxHeight; return this; }
        public Targeting build() {
            return new Targeting(countries, regions, deviceTypes, categories, keywords, minWidth, maxWidth, minHeight, maxHeight);
        }
    }
}
