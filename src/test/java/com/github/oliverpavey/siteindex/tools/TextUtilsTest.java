package com.github.oliverpavey.siteindex.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TextUtilsTest {

    /**
     * Check the domain can be extracted from an HTTP url.
     */
    @Test
    void extractDomainHttpUrl() {

        final String domain = TextUtils.extractDomain("http://sitename.com/section/page.html");
        assertEquals("http://sitename.com/", domain);
    }

    /**
     * Check the domain can be extracted from an HTTPS url.
     */
    @Test
    void extractDomainHttpsUrl() {

        final String domain = TextUtils.extractDomain("https://sitename.com/section/page.html");
        assertEquals("https://sitename.com/", domain);
    }
}