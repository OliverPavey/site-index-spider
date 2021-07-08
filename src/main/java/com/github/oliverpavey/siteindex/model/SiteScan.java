package com.github.oliverpavey.siteindex.model;

import lombok.Data;

import java.util.Map;
import java.util.TreeMap; // a sorted map

/**
 * Data model class for scan of a website from a given homepage.
 */
@Data
public class SiteScan {

    /**
     * Constructor. Initialize collections.
     */
    public SiteScan() {
        uriToPageScan = new TreeMap<>();
        uriToResourceScan = new TreeMap<>();
    }

    private String domain;
    private PageScan homepage;
    private Map<String, PageScan> uriToPageScan;
    private Map<String, ResourceScan> uriToResourceScan;

    /**
     * Clear out the scan, and its collections.
     */
    public void clear() {
        homepage = null;
        uriToPageScan.clear();
        uriToResourceScan.clear();
    }
}
