package com.ortb.service;

import com.ortb.model.ad.Ad;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory ad inventory. Ads are pre-loaded at startup via AdInventoryConfig.
 * Thread-safe via ConcurrentHashMap.
 */
@Service
public class AdInventoryService {

    private final Map<String, Ad> inventory = new ConcurrentHashMap<>();

    public void addAd(Ad ad) {
        inventory.put(ad.id(), ad);
    }

    public Optional<Ad> findById(String id) {
        return Optional.ofNullable(inventory.get(id));
    }

    public Collection<Ad> getAllAds() {
        return Collections.unmodifiableCollection(inventory.values());
    }

    public void removeAd(String id) {
        inventory.remove(id);
    }

    public int size() {
        return inventory.size();
    }
}
