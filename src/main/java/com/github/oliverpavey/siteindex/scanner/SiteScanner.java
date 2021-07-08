package com.github.oliverpavey.siteindex.scanner;

import com.github.oliverpavey.siteindex.function.TriStringConsumer;
import com.github.oliverpavey.siteindex.model.PageScan;
import com.github.oliverpavey.siteindex.model.ResourceScan;
import com.github.oliverpavey.siteindex.model.SiteScan;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.oliverpavey.siteindex.tools.TextUtils.extractDomain;

/**
 * Component for launching a Site Scan (with SiteScannerTask inner class)
 */
@Slf4j
@Component
public class SiteScanner {

    @Value("${siteindex.sitescanner.links}")
    private String linkFinderTemplates;

    @Value("${siteindex.sitescanner.resources}")
    private String resourceFinderTemplates;

    /**
     * Launch a scan.
     *
     * @param homepageUrl The page from which the scan should commence, on the site to scan.
     * @return An optional containing the site scan (or an empty optional).
     */
    public Optional<SiteScan> scan(String homepageUrl) {

        final SiteScannerTask scanner = new SiteScannerTask(homepageUrl);
        return scanner.scan();
    }

    /**
     * Inner class which orchestrates the scan of a site.
     */
    class SiteScannerTask {

        final static String REGEX_COMMA = ",";
        final static String REGEX_DOT = "\\.";

        private final List<String> knownNonPageUris;

        private final String homepageUrl;
        private final SiteScan siteScan;

        /**
         * Constructor. Initializes collections and determines site domain.
         *
         * @param homepageUrl The page from which the scan should commence, on the site to scan.
         */
        public SiteScannerTask(String homepageUrl) {

            knownNonPageUris = new ArrayList<>();

            this.homepageUrl = homepageUrl;
            siteScan = new SiteScan();
            siteScan.setDomain(extractDomain(homepageUrl));
        }

        /**
         * Run the scan.
         *
         * @return An optional containing the model built by the scan, or an empty optional if there is a problem.
         */
        public Optional<SiteScan> scan() {

            try {
                siteScan.clear();
                log.info("Scan commenced: {}", homepageUrl);
                final PageScan homePageScan = scan(homepageUrl);
                siteScan.setHomepage(homePageScan);
                log.info("Scan completed: {}", homepageUrl);
                return Optional.of(siteScan);

            } catch (IOException e) {
                log.warn("Problem running JSoup: {}", e.getMessage(), e);
                return Optional.empty();
            }
        }

        /**
         * Scan a single page within a website, and recursively scan any new pages found.
         *
         * @param url The URL of the page to scan.
         * @return A model representing the page, which may include links to models of other pages.
         * @throws IOException Any exception thrown when JSoup attempts to retrieve the page.
         */
        private PageScan scan(String url) throws IOException {

            // Checking a URL which does not respond can be time consuming - avoid checking a URL twice.
            if (knownNonPageUris.contains(url)) {
                log.debug("Cannot retrieve page '{}'. Uri known not to contain HTML page.", url);
                return null;
            }

            // If the page has already been scanned then we also don't need to scan it again. Return existing scan.
            final PageScan existingPage = siteScan.getUriToPageScan().get(url);
            if (existingPage != null) {
                existingPage.incReferences();
                return existingPage;
            }

            // Use JSoup to retrieve the document for processing.
            final Document doc;
            try {
                doc = Jsoup.connect(url).followRedirects(true).get();
            } catch (HttpStatusException se) {
                log.debug("Could not retrieve page '{}'. Status Code: {}", url, se.getStatusCode());
                knownNonPageUris.add(url);
                return null;
            } catch (UnsupportedMimeTypeException mte) {
                log.debug("Could not retrieve page '{}'. with mimetype: {}", url, mte.getMimeType());
                knownNonPageUris.add(url);
                return null;
            }

            // Create a new model object to populate with the data of the page retrieved.
            final PageScan pageScan = new PageScan(url);

            log.info("Scanning page: {}", url);

            // Create an array of links to scan later.
            List<String> linksToScan = new ArrayList<>();

            final String docUri = doc.baseUri();
            log.debug("doc uri: {}", docUri);

            final String base = url.substring(0, url.lastIndexOf('/') + 1);
            log.debug("base: {}", base);

            final String title = doc.title();
            log.debug("title: {}", title);

            // Build list of external links, and internal links to scan later.
            final AbsoluteRef uriRef = new AbsoluteRef(url, "");
            forEachTagWithAttribute(doc, linkFinderTemplates, (tagName, attrName, attrValue) -> {
                final AbsoluteRef ref = new AbsoluteRef(base, attrValue);
                log.debug("{}.{}: {} {}", tagName, attrName, ref.siteReferenceDescription(), ref.getAbsoluteRef());
                if (!ref.isSiteReference()) {
                    pageScan.getExternalLinks().add(ref.getAbsoluteRef());
                } else {
                    if (!uriRef.equals(ref))
                        linksToScan.add(ref.getAbsoluteRef());
                }
            });

            // Build list of resource references.
            forEachTagWithAttribute(doc, resourceFinderTemplates, (tagName, attrName, attrValue) -> {
                final AbsoluteRef ref = new AbsoluteRef(base, attrValue);
                log.debug("{}.{}: {} {}", tagName, attrName, ref.siteReferenceDescription(), ref.getAbsoluteRef());

                final String resourceUri = ref.getAbsoluteRef();
                final ResourceScan resourceScan = siteScan.getUriToResourceScan()
                        .getOrDefault(resourceUri, new ResourceScan(resourceUri));
                siteScan.getUriToResourceScan().putIfAbsent(resourceUri, resourceScan);
                pageScan.getResources().add(resourceScan);
                resourceScan.incReferences();
            });

            // Scan internal links found earlier (in this method) and build list of associated models.
            siteScan.getUriToPageScan().putIfAbsent(url, pageScan);
            for (String link : linksToScan) {
                if (!link.isBlank()) {
                    final PageScan existingLinkScan = siteScan.getUriToPageScan().get(link);
                    if (existingLinkScan != null) {
                        pageScan.getLinks().add(existingLinkScan);
                    } else {
                        final PageScan linkScan = scan(link); // Recursive call to this method.
                        if (linkScan != null) // linkScan==null indicates an unreadable page
                            pageScan.getLinks().add(linkScan);
                    }
                }
            }

            // Return the model constructed.
            return pageScan;
        }

        /**
         * Simplify the iteration of all instances of (sets of) tag and attribute with found
         * values.  This method may be called with a lambda which will be invoked for each
         * found tag and attribute.
         *
         * @param doc                     The document to process.
         * @param resourceFinderTemplates A comma separated list, of dot separated pairs, of tag and attribute names.
         * @param tagProcessor            The lambda (or class) to process each found tag, attribute and value.
         */
        private void forEachTagWithAttribute(Document doc, String resourceFinderTemplates,
                                             TriStringConsumer tagProcessor) {

            for (String resourceFinder : resourceFinderTemplates.split(REGEX_COMMA)) {
                final String[] split = resourceFinder.split(REGEX_DOT);
                final String tagName = split[0];
                final String attrName = split[1];
                final Elements tags = doc.select(tagName);
                for (Element tag : tags) {
                    final String attrValue = tag.attr(attrName);
                    tagProcessor.accept(tagName, attrName, attrValue);
                }
            }
        }
    }
}
