package com.github.oliverpavey.siteindex.model;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.TreeSet; // a sorted set

/**
 * Data model class for scan of a single web page.
 */
@Slf4j
@Data
public class PageScan implements Comparable<PageScan> {

    private String uri;
    private int references;
    private Set<PageScan> links;
    private Set<String> externalLinks;
    private Set<ResourceScan> resources;

    /**
     * Constructor, creating empty collections.
     *
     * @param uri URI of the page being scanned.
     */
    public PageScan(String uri) {
        this.uri = uri;
        links = new TreeSet<>();
        externalLinks = new TreeSet<>();
        resources = new TreeSet<>();
    }

    /**
     * Record a count of references to this web page from the site being scanned.
     */
    public void incReferences() {
        references = references + 1;
    }

    /**
     * Allow default sort order to be URI alphabetically. (e.g. when using TreeSet.)
     *
     * @param other The other PageScan object to compare with.
     * @return A numeric value indicating the sort order. See java.lang.Comparable.
     */
    @Override
    public int compareTo(PageScan other) {
        return uri.compareTo(other.uri);
    }
}
