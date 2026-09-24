package com.preponderous.parpt.trace;

import com.preponderous.parpt.ParptApplication;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * Reports that Parpt was used, to the trace service, and never gets in the way of the shell.
 *
 * <p>Two events are sent, both off the calling thread through the vendored {@link TraceClient}:
 * {@code startup} once per process (tagged with the program version only) and
 * {@code project-created} when a project is saved, with no tags at all. Nothing about the
 * projects themselves is sent: no names, descriptions, scores, paths or hostnames.
 *
 * <p>Reporting is on by default and switched off with {@code usage-reporting.enabled=false}
 * (as a {@code -D} system property, in an {@code application.yaml} next to the JAR, or as
 * {@code USAGE_REPORTING_ENABLED=false} in the environment), or with the environment variables
 * every trace client honours, {@code TRACE_USAGE_REPORTING=off} and {@code DO_NOT_TRACK=1},
 * which the client checks before anything this class passes it. The first time reporting runs
 * on a machine one notice is printed saying so; a marker file under the user's config
 * directory ({@code ~/.config/parpt/}) keeps it from being repeated, because Parpt has no
 * settings file of its own on disk. Details: {@value #DETAILS_URL}
 *
 * <p>Every path through this class is exception-safe: a bad endpoint, an unwritable home
 * directory or an unreachable trace server leave the shell untouched.
 */
@Component
public class UsageReporter {

    private static final Logger log = LoggerFactory.getLogger(UsageReporter.class);

    /** The name the program key was issued for; the {@code application} field of every event. */
    static final String APPLICATION = "Parpt";
    static final String STARTUP_EVENT = "startup";
    static final String PROJECT_CREATED_EVENT = "project-created";
    static final String NOTICE_MARKER_FILE = "usage-reporting-notice-shown";
    /** The public page describing what trace collects and every way to turn it off. */
    static final String DETAILS_URL = "https://github.com/Stephenson-Software/trace#usage-reporting";

    private final TraceClient client;
    private final String version;
    private final Path noticeMarker;

    @Autowired
    public UsageReporter(
            @Value("${usage-reporting.enabled:true}") String enabled,
            @Value("${usage-reporting.endpoint:https://trace.danielstephenson.dev}") String endpoint,
            @Value("${usage-reporting.key:}") String key) {
        this(enabled, endpoint, key, resolveVersion(), defaultNoticeMarker());
    }

    UsageReporter(String enabled, String endpoint, String key, String version, Path noticeMarker) {
        this.client = buildClient(enabled, endpoint, key);
        this.version = version;
        this.noticeMarker = noticeMarker;
    }

    private static TraceClient buildClient(String enabled, String endpoint, String key) {
        // A blank value (an empty environment variable, say) means "default", i.e. on.
        boolean on = enabled == null || enabled.isBlank() || !"false".equalsIgnoreCase(enabled.trim());
        try {
            // The program's own switch goes to the builder rather than short-circuiting here, so
            // the client applies its precedence (environment first).
            return TraceClient.builder(endpoint, APPLICATION)
                    .key(key)
                    .enabled(on)
                    .logger(java.util.logging.Logger.getLogger(UsageReporter.class.getName()))
                    .build();
        } catch (RuntimeException badConfiguration) {
            log.debug("Usage reporting disabled: {}", badConfiguration.getMessage());
            return TraceClient.disabled();
        }
    }

    /** {@code ~/.config/parpt/usage-reporting-notice-shown}; null if there is no usable home. */
    static Path defaultNoticeMarker() {
        String home = System.getProperty("user.home");
        if (home == null || home.isBlank()) {
            return null;
        }
        return Paths.get(home, ".config", "parpt", NOTICE_MARKER_FILE);
    }

    /** The version from the JAR manifest, or null when not run from a built JAR. */
    static String resolveVersion() {
        Package pkg = ParptApplication.class.getPackage();
        String fromManifest = pkg == null ? null : pkg.getImplementationVersion();
        return fromManifest == null || fromManifest.isBlank() ? null : fromManifest.trim();
    }

    /** Whether events are actually sent. */
    public boolean isEnabled() {
        return client.isEnabled();
    }

    /** Why nothing is sent, in Parpt's terms, or null while reporting is on. */
    public String disabledReason() {
        String reason = client.disabledReason();
        return TraceClient.REASON_CONFIG.equals(reason) ? "usage-reporting.enabled" : reason;
    }

    @PostConstruct
    void start() {
        if (!client.isEnabled()) {
            log.debug("Usage reporting is off ({}).", disabledReason());
            return;
        }
        showFirstRunNoticeOnce();
        if (version == null) {
            client.report(STARTUP_EVENT);
        } else {
            client.report(STARTUP_EVENT, null, Collections.singletonMap("version", version));
        }
    }

    /** Reports that a project was saved. Carries nothing about the project. */
    public void projectCreated() {
        client.report(PROJECT_CREATED_EVENT);
    }

    /**
     * Stops the sending thread, waiting briefly (at most the client's read timeout) for a report
     * in flight, so a short run does not exit before its startup event has left the machine.
     */
    @PreDestroy
    public void close() {
        client.close();
    }

    /** The text shown once, the first time reporting runs on a machine. */
    static String firstRunNotice() {
        return "Usage reporting is on: Parpt sends its name and version (a startup event) and a "
                + "project-created event (nothing else) to https://trace.danielstephenson.dev - nothing "
                + "about your projects or this machine. Turn it off with -Dusage-reporting.enabled=false, "
                + "USAGE_REPORTING_ENABLED=false or TRACE_USAGE_REPORTING=off. Details: " + DETAILS_URL;
    }

    private void showFirstRunNoticeOnce() {
        if (noticeMarker == null) {
            return;
        }
        try {
            if (Files.exists(noticeMarker)) {
                return;
            }
            // Printed to the console rather than logged: the shell's log output is not what a
            // user of the interactive prompt is looking at.
            System.out.println(firstRunNotice());
            Files.createDirectories(noticeMarker.getParent());
            Files.write(noticeMarker,
                    "The usage-reporting notice was shown once; delete this file to see it again.\n"
                            .getBytes(StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException cannotPersist) {
            // The notice is shown again next run; that is the worst case, and it is harmless.
            log.debug("Could not record that the usage-reporting notice was shown: {}", cannotPersist.getMessage());
        }
    }
}
