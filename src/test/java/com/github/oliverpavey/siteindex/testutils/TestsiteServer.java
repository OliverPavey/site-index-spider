package com.github.oliverpavey.siteindex.testutils;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple web server for static site kept in (test) resources, for Unit Tests.
 * <p>
 * This class does not make use of any Spring libraries to reduce the likelihood that
 * it interferes with the software under test.
 * <p>
 * By default it hosts on the first available port between 8000 and 8099. A different
 * range may be passed into the constructor. The test should read the port back with
 * the getPort() method, or (for convenience) with the getBaseUrl() method.
 * <p>
 * The class implements AutoClosable and should be used in a try-with-resources statement
 * to ensure that the server stops (and the port is closed).
 */
@Slf4j
public class TestsiteServer implements AutoCloseable {

    HttpServer server;

    @Getter
    int port;
    @Getter
    String baseUrl;

    /**
     * Constructor with default port range.
     */
    public TestsiteServer() {
        this(8000, 8099);
    }

    /**
     * Constructor with supplied port range.
     *
     * @param minPort The lowest port in the range to try.
     * @param maxPort The last port in the range to try. Must be greater than minPort.
     */
    public TestsiteServer(int minPort, int maxPort) {

        int candidatePort = minPort;
        int attemptsLeft = maxPort - minPort;
        while (server == null && attemptsLeft > 0) {
            try {
                server = startServer(candidatePort);

            } catch (IOException ex) {
                attemptsLeft--;
                if (attemptsLeft > 0) {
                    candidatePort++;
                } else {
                    log.warn("Could not start http server.", ex);
                }
            }
        }

        if (server != null) {
            port = candidatePort;
            baseUrl = String.format("http://localhost:%d/", port);
        }
    }

    /**
     * Attempt to start a sever on the candidatePort.
     *
     * @param candidatePort The port on which to attempt to start the server.
     * @return The started server (unless IOException is thrown).
     * @throws IOException Indicating that the server could not be started. Probably the port is in use.
     */
    private HttpServer startServer(int candidatePort) throws IOException {

        HttpServer server = HttpServer.create(new InetSocketAddress(candidatePort), 0);
        server.createContext("/", new TestsiteRequestHandler());
        server.setExecutor(null);
        server.start();
        return server;
    }

    /**
     * AutoClosable stop the server (only if it has been started).
     */
    @Override
    public void close() {
        if (server != null)
            server.stop(0);
    }

    /**
     * Handler for HTTP requests.
     * <p>
     * Only supports GET requests.
     * Attempts to resolve the GET requests by reading resources corresponding to the path supplied.
     */
    static class TestsiteRequestHandler implements HttpHandler {

        static final String HTTP_METHOD_GET = "GET";
        static final int HTTP_STATUS_OK = 200;
        static final int HTTP_STATUS_NOT_FOUND = 404;
        static final int HTTP_STATUS_INTERNAL_ERROR = 500;
        static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";

        /**
         * Exception to throw indicating processing cannot proceed, and indicating HTTP response to give.
         */
        public static class UnsupportedRequest extends Exception {

            @Getter
            private final int status;
            @Getter
            private final String message;

            public UnsupportedRequest(int status, String message) {
                this.status = status;
                this.message = message;
            }
        }

        /**
         * Handle a request and send back a response. Part of the HttpHandler interface.
         *
         * @param httpExchange The object via which the communication is executed.
         */
        @Override
        public void handle(HttpExchange httpExchange) {
            try {
                checkMethodIsGet(httpExchange);

                final String path = httpExchange.getRequestURI().getPath();
                log.info("Processing GET request for '{}'", path);
                byte[] responseContent = readResourceForPath(path);

                Map<String, String> headerMap = new LinkedHashMap<>();
                headerMap.put(HTTP_HEADER_CONTENT_TYPE, mimeTypeForExtension(path));

                replyWith(httpExchange, HTTP_STATUS_OK, headerMap, responseContent);

            } catch (UnsupportedRequest unsupportedRequest) {
                replyWith(httpExchange,
                        unsupportedRequest.getStatus(),
                        Map.of(HTTP_HEADER_CONTENT_TYPE, "text/plain"),
                        unsupportedRequest.getMessage().getBytes(StandardCharsets.UTF_8));
            }
        }

        /**
         * Encapsulate sending response status, headers and body.
         *
         * @param httpExchange    Exchange object to send the communication through.
         * @param status          The HTTP Status to return.
         * @param headerMap       A map of header keys and values to add to the response.
         * @param responseContent The response content as a byte array.
         */
        private void replyWith(HttpExchange httpExchange, int status, Map<String, String> headerMap, byte[] responseContent) {
            try {
                final Headers responseHeaders = httpExchange.getResponseHeaders();
                for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                    responseHeaders.add(entry.getKey(), entry.getValue());
                }

                httpExchange.sendResponseHeaders(status, responseContent.length);
                OutputStream os = httpExchange.getResponseBody();
                os.write(responseContent);
                os.close();

            } catch (IOException ex) {
                log.warn("Could not send sever response: status={}", status, ex);
            }
        }

        /**
         * Check that the request is a GET request. We only support GET requests.
         *
         * @param httpExchange The exchange object with the request method information.
         * @throws UnsupportedRequest Thrown if this is not a GET request.
         */
        private void checkMethodIsGet(HttpExchange httpExchange) throws UnsupportedRequest {

            String method = httpExchange.getRequestMethod();
            if (!HTTP_METHOD_GET.equals(method))
                throw new UnsupportedRequest(HTTP_STATUS_NOT_FOUND,
                        String.format("Only GET methods are supported: '%s'", method));
        }

        /**
         * Read a resource (from within the 'testsite' resource folder) and return it as a byte array.
         *
         * @param path The path from the HTTP request (which is also the last part of the resource name).
         * @return The loaded resource as a byte array.
         * @throws UnsupportedRequest Thrown if the resource cannot be loaded.
         */
        private byte[] readResourceForPath(String path) throws UnsupportedRequest {

            String resourceName = "testsite" + path;
            try {
                final ClassLoader classLoader = this.getClass().getClassLoader();
                final InputStream resourceAsStream = classLoader.getResourceAsStream(resourceName);
                if (resourceAsStream == null)
                    throw new UnsupportedRequest(HTTP_STATUS_NOT_FOUND,
                            String.format("Resource not found: %s", resourceName));
                final byte[] bytes = resourceAsStream.readAllBytes();
                log.info("Read {} bytes from resource '{}'", bytes.length, resourceName);
                return bytes;

            } catch (Exception e) {
                if (e instanceof UnsupportedRequest)
                    throw (UnsupportedRequest) e;

                log.warn("Exception reading resource.", e);
                throw new UnsupportedRequest(HTTP_STATUS_INTERNAL_ERROR, e.getMessage());
            }
        }

        /**
         * Convert a file extension type into a mime type to return with the resource.
         *
         * @param path The path which must end with the extension type.
         * @return The mimetype to add to the HTTP response headers.
         */
        private String mimeTypeForExtension(String path) {

            String ext = extensionFromPath(path).toLowerCase();
            switch (ext) {
                case "html":
                    return "text/html";
                case "css":
                    return "text/css";
                case "js":
                    return "text/javascript";
                case "jpg":
                    return "image/jpeg";
                case "svg":
                    return "image/svg+xml";
                default:
                    return "text/plain";
            }
        }

        /**
         * Extract a file extension from a path.
         *
         * @param path The path with the file extension.
         * @return The extracted file extension (or a blank string if no extension).
         */
        private String extensionFromPath(String path) {

            Pattern pattern = Pattern.compile("^.*\\.(.*?)$");
            Matcher matcher = pattern.matcher(path);
            if (matcher.matches())
                return matcher.group(1);
            else
                return "";
        }
    }

    /**
     * Utility program.
     * <p>
     * This allows the tests site to be hosted on the local machine, which can be useful for manually
     * checking the test site.
     *
     * @param args Command line arguments. Not used.
     */
    public static void main(String[] args) {

        try (final TestsiteServer server = new TestsiteServer()) {
            log.info("Started server: {}", server.getBaseUrl());
            while (true) {
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    log.warn("Sleep interrupted.");
                }
            }
        }
    }
}
