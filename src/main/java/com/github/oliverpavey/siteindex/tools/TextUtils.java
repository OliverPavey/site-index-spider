package com.github.oliverpavey.siteindex.tools;

import com.google.common.base.CharMatcher;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Common routines used for text manipulation.
 * <p>
 * Extracting these routines to a separate unit helps to make code elsewhere easier to read.
 */
public class TextUtils {

    static final char URL_PATH_SEPARATOR = '/';

    /**
     * Remove any series of a leading character from a string.
     *
     * @param ch  Character to remove.
     * @param str String to process.
     * @return String with leading characters removed.
     */
    public static String removeLeadingChar(char ch, String str) {
        return CharMatcher.is(ch).trimLeadingFrom(str);
    }

    /**
     * Remove any series of a trailing character from a string.
     *
     * @param ch  Character to remove.
     * @param str String to process.
     * @return String with trailing characters removed.
     */
    public static String removeTrailingChar(char ch, String str) {
        return CharMatcher.is(ch).trimTrailingFrom(str);
    }

    /**
     * Remove any series of a leading URL path character from a string.
     *
     * @param str String to process.
     * @return String with leading URL path characters removed.
     */
    public static String removeLeadingPath(String str) {
        return removeLeadingChar(URL_PATH_SEPARATOR, str);
    }

    /**
     * Remove any series of a trailing URL path character from a string.
     *
     * @param str String to process.
     * @return String with trailing URL path characters removed.
     */
    public static String removeTrailingPath(String str) {
        return removeTrailingChar(URL_PATH_SEPARATOR, str);
    }

    /**
     * Join two parts of a URL ensuring that there is exactly one path separator joining the parts.
     *
     * @param base First (or proceeding) URL part to join.
     * @param ref  Second (or succeeding) URL prat to join.
     * @return URL parts joined together.
     */
    public static String joinUrl(String base, String ref) {
        final String plainBase = removeTrailingPath(base);
        final String plainRef = removeTrailingPath(removeLeadingPath(ref));
        return plainBase + URL_PATH_SEPARATOR + plainRef;
    }

    /**
     * Extract a string representing the web-site domain from an absolute URL.
     *
     * @param url The absolute URL containing the domain information.
     * @return The domain string, e.g. 'http://mysite.com/'
     */
    public static String extractDomain(String url) {

        final String URL_DOMAIN_EXTRACTOR_REGEX = "^(https?://.*?/).*$";

        if (url == null)
            return "";

        Pattern pattern = Pattern.compile(URL_DOMAIN_EXTRACTOR_REGEX);
        Matcher matcher = pattern.matcher(url);
        if (matcher.matches())
            return matcher.group(1);
        else
            return "";
    }

    /**
     * Remove blank lines from a multi-line string.
     * <p>
     * N.B. The returned string will use Posix style line breaks.
     *
     * @param str Multi line string.
     * @return The same string with any blank lines removed.
     */
    public static String removeBlankLines(final String str) {

        final String LINE_BREAK_REGEX = "[\r\n]";
        final String NEW_LINE = "\n";

        return Arrays.stream(str.split(LINE_BREAK_REGEX))
                .filter(line -> !line.isBlank())
                .collect(Collectors.joining(NEW_LINE));
    }
}
