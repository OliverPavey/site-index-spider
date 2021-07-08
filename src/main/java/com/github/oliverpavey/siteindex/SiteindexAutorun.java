package com.github.oliverpavey.siteindex;

import com.github.oliverpavey.siteindex.model.SiteScan;
import com.github.oliverpavey.siteindex.scanner.SiteScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.*;

import static com.github.oliverpavey.siteindex.tools.TextUtils.removeBlankLines;

/**
 * Starts the site scan, using the environment variable parameters to identify the site to scan
 * and the output file to write.  (See the launcher: 'siteindex.sh'.)
 */
@Slf4j
@Profile("!test")
@Component
public class SiteindexAutorun implements CommandLineRunner {

    @Value("#{environment.HOMEPAGE_URL}")
    String homepageUrl;

    @Value("#{environment.OUTPUT_FILE}")
    String outputFile;

    final SiteScanner siteScanner;

    final TemplateEngine templateEngine;

    /**
     * Constructor
     *
     * @param siteScanner    The site scanner component.
     * @param templateEngine The Thymeleaf template engine.
     */
    public SiteindexAutorun(SiteScanner siteScanner, TemplateEngine templateEngine) {
        this.siteScanner = siteScanner;
        this.templateEngine = templateEngine;
    }

    /**
     * The main method 'run()' from the Spring CommandLineRunner.
     * When this method terminates the program will exit.
     *
     * @param args The command line arguments. Not used.
     */
    @Override
    public void run(String... args) {

        syntax();
        try {
            runScanner(homepageUrl, outputFile);

        } catch (Exception e) {
            log.warn("An unexpected problem occurred during processing", e);
        }
    }

    /**
     * Print the usage syntax.
     */
    private void syntax() {
        log.info("Usage: siteindex.sh '<homepage-url>' '<output-file>'");
    }

    /**
     * Orchestrate running the site scanner, processing the output with our Thymeleaf template,
     * and writing the output to a file.
     *
     * @param homepageUrl The URL of the page from which the scan should start. (Typically the homepage.)
     * @param outputFile  The filename to which the output should be written.
     * @return The template output. This helps to make the method testable.
     * @throws IOException Any exception which occurs whilst writing the output to disk.
     */
    String runScanner(final String homepageUrl, final String outputFile) throws IOException {

        SiteScan siteScan = siteScanner.scan(homepageUrl).orElseThrow();
        String xhtml = removeBlankLines(applyReportTemplate(siteScan));
        if (outputFile != null)
            saveTextToFile(xhtml, outputFile);
        return xhtml;
    }

    /**
     * Use Thymeleaf to convert the generated model into a report.
     *
     * @param siteScan The model of the site built by the scan.
     * @return The content of the report.
     */
    public String applyReportTemplate(final SiteScan siteScan) {

        Context thymeleafContext = new Context();
        thymeleafContext.setVariable("siteScan", siteScan);
        StringWriter stringWriter = new StringWriter();
        templateEngine.process("report_template.html", thymeleafContext, stringWriter);
        return stringWriter.toString();
    }

    /**
     * Encapsulate the writing of text to a file.
     *
     * @param text       The text to write to the file.
     * @param outputFile The filename of the file to write.
     * @throws IOException Any exception which occurs whilst writing the output to disk.
     */
    private void saveTextToFile(String text, String outputFile) throws IOException {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
            writer.write(text);
        }
        log.info("Site index written to: {}", outputFile);
    }
}
