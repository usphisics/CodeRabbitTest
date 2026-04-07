package com.ortb.config;

import com.ortb.model.ad.Ad;
import com.ortb.model.ad.AdFormat;
import com.ortb.model.ad.Targeting;
import com.ortb.service.AdInventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

/**
 * Loads sample ad inventory at application startup.
 *
 * In production, this would pull from a database or external DSP feed.
 */
@Configuration
public class AdInventoryConfig {

    private static final Logger log = LoggerFactory.getLogger(AdInventoryConfig.class);

    @Bean
    ApplicationRunner loadSampleAds(AdInventoryService inventoryService) {
        return args -> {
            // --- Ad 1: High-CPM global banner (300x250) ---
            inventoryService.addAd(Ad.builder()
                    .id("ad-001")
                    .title("Premium Brand Banner")
                    .adm("<div style='width:300px;height:250px;background:#4A90D9;color:white;display:flex;" +
                         "align-items:center;justify-content:center;font-family:sans-serif;font-size:18px;'>" +
                         "Premium Brand Ad</div>")
                    .nurl("https://ad.example.com/win?adid=ad-001&price=${AUCTION_PRICE}")
                    .price(5.50)
                    .adomain(List.of("brandexample.com"))
                    .crid("crid-001")
                    .format(AdFormat.BANNER)
                    .w(300).h(250)
                    .cat(List.of("IAB1", "IAB2"))
                    .targeting(Targeting.unrestricted())
                    .build());

            // --- Ad 2: US-only mobile leaderboard (728x90) ---
            inventoryService.addAd(Ad.builder()
                    .id("ad-002")
                    .title("US Mobile Leaderboard")
                    .adm("<div style='width:728px;height:90px;background:#F5A623;color:white;display:flex;" +
                         "align-items:center;justify-content:center;font-family:sans-serif;font-size:14px;'>" +
                         "US Mobile Deal - Shop Now!</div>")
                    .nurl("https://ad.example.com/win?adid=ad-002&price=${AUCTION_PRICE}")
                    .price(3.80)
                    .adomain(List.of("usmobile.com"))
                    .crid("crid-002")
                    .format(AdFormat.BANNER)
                    .w(728).h(90)
                    .cat(List.of("IAB22"))
                    .targeting(Targeting.builder()
                            .countries(Set.of("USA"))
                            .deviceTypes(Set.of(1, 4, 5)) // Mobile/Tablet, Phone, Tablet
                            .build())
                    .build());

            // --- Ad 3: Tech category medium rectangle (300x250) ---
            inventoryService.addAd(Ad.builder()
                    .id("ad-003")
                    .title("Tech Product Banner")
                    .adm("<div style='width:300px;height:250px;background:#7ED321;color:white;display:flex;" +
                         "align-items:center;justify-content:center;font-family:sans-serif;font-size:16px;'>" +
                         "Latest Tech Gadgets</div>")
                    .nurl("https://ad.example.com/win?adid=ad-003&price=${AUCTION_PRICE}")
                    .price(4.20)
                    .adomain(List.of("techads.io"))
                    .crid("crid-003")
                    .format(AdFormat.BANNER)
                    .w(300).h(250)
                    .cat(List.of("IAB19"))
                    .targeting(Targeting.builder()
                            .categories(Set.of("IAB19", "IAB19-1", "IAB19-2"))
                            .keywords(List.of("technology", "gadgets", "software", "tech"))
                            .build())
                    .build());

            // --- Ad 4: Low-price fallback (160x600) wide skyscraper ---
            inventoryService.addAd(Ad.builder()
                    .id("ad-004")
                    .title("Fallback Wide Skyscraper")
                    .adm("<div style='width:160px;height:600px;background:#9B59B6;color:white;display:flex;" +
                         "align-items:center;justify-content:center;writing-mode:vertical-rl;" +
                         "font-family:sans-serif;font-size:14px;'>Special Offer!</div>")
                    .nurl("https://ad.example.com/win?adid=ad-004&price=${AUCTION_PRICE}")
                    .price(1.00)
                    .adomain(List.of("fallbackads.net"))
                    .crid("crid-004")
                    .format(AdFormat.BANNER)
                    .w(160).h(600)
                    .cat(List.of("IAB1"))
                    .targeting(Targeting.unrestricted())
                    .build());

            // --- Ad 5: Video pre-roll (640x480) ---
            inventoryService.addAd(Ad.builder()
                    .id("ad-005")
                    .title("Video Pre-roll Ad")
                    .adm("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                         "<VAST version=\"4.0\"><Ad id=\"ad-005\"><InLine>" +
                         "<AdSystem>OrtbTest</AdSystem><AdTitle>Video Pre-roll</AdTitle>" +
                         "<Impression><![CDATA[https://ad.example.com/imp?adid=ad-005]]></Impression>" +
                         "<Creatives><Creative><Linear>" +
                         "<Duration>00:00:30</Duration>" +
                         "<MediaFiles><MediaFile type=\"video/mp4\" width=\"640\" height=\"480\">" +
                         "<![CDATA[https://cdn.example.com/video/ad-005.mp4]]>" +
                         "</MediaFile></MediaFiles>" +
                         "</Linear></Creative></Creatives>" +
                         "</InLine></Ad></VAST>")
                    .nurl("https://ad.example.com/win?adid=ad-005&price=${AUCTION_PRICE}")
                    .price(8.00)
                    .adomain(List.of("videoads.example.com"))
                    .crid("crid-005")
                    .format(AdFormat.VIDEO)
                    .w(640).h(480)
                    .cat(List.of("IAB1", "IAB9"))
                    .targeting(Targeting.unrestricted())
                    .build());

            // --- Ad 6: EU-only finance banner (300x250) ---
            inventoryService.addAd(Ad.builder()
                    .id("ad-006")
                    .title("EU Finance Banner")
                    .adm("<div style='width:300px;height:250px;background:#1ABC9C;color:white;display:flex;" +
                         "align-items:center;justify-content:center;font-family:sans-serif;font-size:16px;'>" +
                         "EU Finance Offer</div>")
                    .nurl("https://ad.example.com/win?adid=ad-006&price=${AUCTION_PRICE}")
                    .price(6.00)
                    .adomain(List.of("eufinance.eu"))
                    .crid("crid-006")
                    .format(AdFormat.BANNER)
                    .w(300).h(250)
                    .cat(List.of("IAB13"))
                    .targeting(Targeting.builder()
                            .countries(Set.of("GBR", "DEU", "FRA", "ESP", "ITA", "NLD", "PRT"))
                            .categories(Set.of("IAB13", "IAB13-1", "IAB13-2", "IAB13-3"))
                            .build())
                    .build());

            log.info("Loaded {} sample ads into inventory", inventoryService.size());
        };
    }
}
