package com.github.oliverpavey.siteindex.scanner;

import lombok.Data;

import static com.github.oliverpavey.siteindex.tools.TextUtils.extractDomain;
import static com.github.oliverpavey.siteindex.tools.TextUtils.joinUrl;

/**
 * Construct an absolute reference from a page's URL and the value in the link.
 * (Typically the value from 'a.href'.)
 * <p>
 * This encapsulates the joining of the two parts, ensuring exactly one path
 * divider is used in the join; removing bookmarks from the URL; and determining
 * whether the link is to the site being scanned or an external site.
 */
@Data
public class AbsoluteRef {

    final boolean refIsAbsolute;
    final String absoluteRef;
    final boolean siteReference;

    /**
     * Constructor.  Joins the two URL parts to form a whole.
     *
     * @param base The URL of the page being scanned.
     * @param ref  The text from the link on the page.
     */
    public AbsoluteRef(String base, String ref) {

        final String ABSOLUTE_REFERENCE_REGEX = "^http(s?)://.*$";
        refIsAbsolute = ref.matches(ABSOLUTE_REFERENCE_REGEX);
        absoluteRef = withoutBookmark(refIsAbsolute ? ref : joinUrl(base, ref));
        siteReference = absoluteRef.startsWith(extractDomain(base));
    }

    /**
     * Strips a bookmark from the URL.
     *
     * @param url A URL which may (or may not) include a bookmark.
     * @return The URL without the bookmark.
     */
    private String withoutBookmark(String url) {

        final String BOOKMARK_START = "#";
        if (!url.contains(BOOKMARK_START))
            return url;
        return url.substring(0, url.indexOf(BOOKMARK_START));
    }

    /**
     * Check if this URL is part of the site being scanned. Intended for logging.
     *
     * @return Loggable string indicating whether this URL is part of the site being scanned.
     */
    public String siteReferenceDescription() {

        return siteReference ? "Site-Reference" : "Internet-Reference";
    }
}
