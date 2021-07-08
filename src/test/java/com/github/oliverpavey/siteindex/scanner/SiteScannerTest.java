package com.github.oliverpavey.siteindex.scanner;

import com.github.oliverpavey.siteindex.model.PageScan;
import com.github.oliverpavey.siteindex.model.ResourceScan;
import com.github.oliverpavey.siteindex.model.SiteScan;
import com.github.oliverpavey.siteindex.testutils.TestsiteServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
class SiteScannerTest {

    @Autowired
    SiteScanner siteScanner;

    /**
     * Scan the test site, and check some of the model metrics match with the sites static data.
     */
    @Test
    void scanTest() {

        try (final TestsiteServer server = new TestsiteServer()) {
            final String baseUrl = server.getBaseUrl();
            final String homepageUrl = baseUrl + "index.html";
            final Optional<SiteScan> optSiteScan = siteScanner.scan(homepageUrl);

            assertTrue(optSiteScan.isPresent(), "Site scan did not return data");

            final SiteScan siteScan = optSiteScan.get();
            final Collection<PageScan> pageScans = siteScan.getUriToPageScan().values();
            final PageScan aboutScan = siteScan.getUriToPageScan().get(baseUrl + "about.html");
            final List<String> aboutScanLinkURIs = aboutScan.getLinks().stream()
                    .map(PageScan::getUri)
                    .collect(Collectors.toUnmodifiableList());
            final Collection<ResourceScan> resourceScans = siteScan.getUriToResourceScan().values();
            assertAll(
                    () -> assertEquals(6, pageScans.size(), "Number of pages found."),
                    () -> assertEquals(12, resourceScans.size(), "Number of resources found."),
                    () -> assertEquals(6, aboutScan.getLinks().size(), "About page links."),
                    () -> assertTrue(aboutScanLinkURIs.contains(homepageUrl)),
                    () -> assertTrue(aboutScanLinkURIs.contains(baseUrl + "homeware.html")),
                    () -> assertTrue(aboutScanLinkURIs.contains(baseUrl + "garden.html")),
                    () -> assertTrue(aboutScanLinkURIs.contains(baseUrl + "tools.html")),
                    () -> assertTrue(aboutScanLinkURIs.contains(baseUrl + "exercise.html")),
                    () -> assertTrue(aboutScanLinkURIs.contains(baseUrl + "about.html"))
            );
        }
    }
}