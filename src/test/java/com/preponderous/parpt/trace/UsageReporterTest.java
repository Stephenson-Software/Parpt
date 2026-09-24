package com.preponderous.parpt.trace;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises the reporter against a loopback stub; nothing here ever reaches the real service. */
class UsageReporterTest {

    private HttpServer stub;
    private final List<String> bodies = new CopyOnWriteArrayList<>();
    private String endpoint;

    @BeforeEach
    void startStub() throws Exception {
        stub = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        stub.createContext("/api/metrics", exchange -> {
            try (InputStream in = exchange.getRequestBody()) {
                bodies.add(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        stub.start();
        endpoint = "http://127.0.0.1:" + stub.getAddress().getPort();
    }

    @AfterEach
    void stopStub() {
        stub.stop(0);
    }

    private static boolean environmentOptsOut() {
        String trace = System.getenv(TraceClient.ENV_USAGE_REPORTING);
        String dnt = System.getenv(TraceClient.ENV_DO_NOT_TRACK);
        return (trace != null && !trace.isBlank()) || (dnt != null && !dnt.isBlank());
    }

    @Test
    void disabledSettingSendsNothingAndShowsNoNotice(@TempDir Path home) {
        Path marker = home.resolve("marker");
        UsageReporter reporter = new UsageReporter("false", endpoint, "a-key", "1.0", marker);
        reporter.start();
        reporter.projectCreated();
        reporter.close();

        assertFalse(reporter.isEnabled());
        assertTrue(bodies.isEmpty());
        assertFalse(Files.exists(marker));
    }

    @Test
    void blankKeySendsNothing(@TempDir Path home) {
        UsageReporter reporter = new UsageReporter("true", endpoint, "", "1.0", home.resolve("marker"));
        reporter.start();
        reporter.close();

        assertFalse(reporter.isEnabled());
        assertTrue(bodies.isEmpty());
    }

    @Test
    void sendsStartupWithVersionAndProjectCreatedWithNothingElse(@TempDir Path home) {
        if (environmentOptsOut()) {
            return; // the environment has switched reporting off; that path is the client's to test
        }
        Path marker = home.resolve("config").resolve("marker");
        UsageReporter reporter = new UsageReporter("true", endpoint, "a-key", "1.2.3", marker);
        reporter.start();
        reporter.projectCreated();
        reporter.close();

        assertEquals(2, bodies.size());
        assertTrue(bodies.get(0).contains("\"application\":\"Parpt\""), bodies.get(0));
        assertTrue(bodies.get(0).contains("\"name\":\"startup\""), bodies.get(0));
        assertTrue(bodies.get(0).contains("\"version\":\"1.2.3\""), bodies.get(0));
        assertTrue(bodies.get(1).contains("\"name\":\"project-created\""), bodies.get(1));
        assertFalse(bodies.get(1).contains("tags\":{\""), bodies.get(1));
        assertTrue(Files.exists(marker), "the first-run notice is recorded as shown");
    }

    @Test
    void noticeMentionsEveryOptOutAndTheDetailsLink() {
        String notice = UsageReporter.firstRunNotice();
        assertTrue(notice.contains("usage-reporting.enabled=false"));
        assertTrue(notice.contains("USAGE_REPORTING_ENABLED=false"));
        assertTrue(notice.contains("TRACE_USAGE_REPORTING=off"));
        assertTrue(notice.contains(UsageReporter.DETAILS_URL));
    }
}
