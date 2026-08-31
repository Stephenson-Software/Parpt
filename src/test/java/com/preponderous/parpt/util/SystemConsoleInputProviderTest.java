package com.preponderous.parpt.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
class SystemConsoleInputProviderTest {

    SystemConsoleInputProvider inputProvider = new SystemConsoleInputProvider();

    private final InputStream originalIn = System.in;

    @BeforeEach
    void requireTheStandardInputFallback() {
        // These tests cover the no-console fallback, which is the path taken under Gradle on the
        // JDK 21 toolchain this project targets. A JDK that hands back a Console for a redirected
        // stream would exercise the other branch instead, which System.setIn cannot drive.
        assumeTrue(System.console() == null, "requires an environment without an attached console");
    }

    @AfterEach
    void restoreStandardInput() {
        System.setIn(originalIn);
    }

    @Test
    void testReadLineReturnsTheEnteredLine() {
        System.setIn(new ByteArrayInputStream("an answer\n".getBytes(StandardCharsets.UTF_8)));

        assertEquals("an answer", inputProvider.readLine("[TEST] prompt: "));
    }

    @Test
    void testReadLineReturnsNullWhenInputIsExhausted() {
        System.setIn(new ByteArrayInputStream(new byte[0]));

        assertNull(inputProvider.readLine("[TEST] prompt: "));
    }
}
