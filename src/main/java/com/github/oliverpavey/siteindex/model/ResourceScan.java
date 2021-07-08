package com.github.oliverpavey.siteindex.model;

import lombok.Data;

/**
 * Data model class for scan information relating to a non-page resource.
 */
@Data
public class ResourceScan implements Comparable<ResourceScan> {

    /**
     * Constructor.
     *
     * @param uri URI of the resource being scanned.
     */
    public ResourceScan(String uri) {
        this.uri = uri;
    }

    private String uri;
    private int references;

    /**
     * Record a count of references to this resource from the site being scanned.
     */
    public void incReferences() {
        references = references + 1;
    }

    /**
     * Allow default sort order to be URI alphabetically. (e.g. when using TreeSet.)
     *
     * @param other The other ResourceScan object to compare with.
     * @return A numeric value indicating the sort order. See java.lang.Comparable.
     */
    @Override
    public int compareTo(ResourceScan other) {
        return uri.compareTo(other.uri);
    }
}
