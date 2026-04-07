package com.ortb.model.ad;

import java.util.List;

/**
 * Internal Ad representation stored in the inventory.
 *
 * @param id        Unique ad identifier
 * @param title     Ad title (human-readable label)
 * @param adm       Ad Markup (HTML/VAST/etc. for rendering)
 * @param nurl      Win notice URL (called when this ad wins the auction)
 * @param price     Bid price in CPM (USD), used for the auction
 * @param adomain   Advertiser domain(s)
 * @param crid      Creative ID
 * @param format    Ad format (BANNER, VIDEO, NATIVE)
 * @param w         Creative width
 * @param h         Creative height
 * @param targeting Targeting restrictions for this ad
 * @param cat       IAB categories this ad belongs to
 */
public record Ad(
        String id,
        String title,
        String adm,
        String nurl,
        double price,
        List<String> adomain,
        String crid,
        AdFormat format,
        int w,
        int h,
        Targeting targeting,
        List<String> cat
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String id, title, adm, nurl, crid;
        private double price;
        private List<String> adomain = List.of();
        private AdFormat format = AdFormat.BANNER;
        private int w, h;
        private Targeting targeting = Targeting.unrestricted();
        private List<String> cat = List.of();

        public Builder id(String id) { this.id = id; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder adm(String adm) { this.adm = adm; return this; }
        public Builder nurl(String nurl) { this.nurl = nurl; return this; }
        public Builder price(double price) { this.price = price; return this; }
        public Builder adomain(List<String> adomain) { this.adomain = adomain; return this; }
        public Builder crid(String crid) { this.crid = crid; return this; }
        public Builder format(AdFormat format) { this.format = format; return this; }
        public Builder w(int w) { this.w = w; return this; }
        public Builder h(int h) { this.h = h; return this; }
        public Builder targeting(Targeting targeting) { this.targeting = targeting; return this; }
        public Builder cat(List<String> cat) { this.cat = cat; return this; }
        public Ad build() {
            return new Ad(id, title, adm, nurl, price, adomain, crid, format, w, h, targeting, cat);
        }
    }
}
