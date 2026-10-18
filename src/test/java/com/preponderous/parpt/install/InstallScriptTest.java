package com.preponderous.parpt.install;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs install.sh against a stub gradlew, so the copying and launcher logic is exercised
 * without starting a nested Gradle build.
 */
@EnabledOnOs({OS.LINUX, OS.MAC})
class InstallScriptTest {

    @TempDir
    Path sourceDir;

    @TempDir
    Path home;

    @BeforeEach
    void copyScriptIntoSourceDir() throws IOException {
        Files.copy(Path.of("install.sh"), sourceDir.resolve("install.sh"), StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    void installsTheRunnableJarAndALauncherThatStartsIt() throws Exception {
        stubGradlew(true);
        Path installDir = home.resolve("share");
        Path binDir = home.resolve("bin");

        Result result = runInstall(Map.of("PARPT_INSTALL_DIR", installDir.toString(),
                "PARPT_BIN_DIR", binDir.toString()));

        assertEquals(0, result.exitCode, result.output);
        assertEquals("runnable", Files.readString(installDir.resolve("parpt.jar")));
        Path launcher = binDir.resolve("parpt");
        assertTrue(Files.isExecutable(launcher));
        assertEquals("#!/bin/sh\nexec java -jar \"" + installDir.resolve("parpt.jar") + "\" \"$@\"\n",
                Files.readString(launcher));
    }

    @Test
    void installsUnderTheUsersLocalDirectoriesByDefault() throws Exception {
        stubGradlew(true);

        Result result = runInstall(Map.of());

        assertEquals(0, result.exitCode, result.output);
        assertEquals("runnable", Files.readString(home.resolve(".local/share/parpt/parpt.jar")));
        assertTrue(Files.isExecutable(home.resolve(".local/bin/parpt")));
    }

    @Test
    void explainsHowToAddTheBinDirectoryWhenItIsNotOnPath() throws Exception {
        stubGradlew(true);
        Path binDir = home.resolve("not-on-path");

        Result result = runInstall(Map.of("PARPT_BIN_DIR", binDir.toString()));

        assertEquals(0, result.exitCode, result.output);
        assertTrue(result.output.contains(binDir + " is not on your PATH."), result.output);
        assertTrue(result.output.contains("export PATH=\"" + binDir + ":$PATH\""), result.output);
    }

    @Test
    void failsWithoutInstallingWhenTheBuildLeavesOnlyThePlainJar() throws Exception {
        stubGradlew(false);

        Result result = runInstall(Map.of());

        assertEquals(1, result.exitCode, result.output);
        assertTrue(result.output.contains("No runnable jar was found"), result.output);
        assertFalse(Files.exists(home.resolve(".local/share/parpt/parpt.jar")));
        assertFalse(Files.exists(home.resolve(".local/bin/parpt")));
    }

    private void stubGradlew(boolean writesRunnableJar) throws IOException {
        String script = "#!/bin/sh\n"
                + "mkdir -p \"" + sourceDir.resolve("build/libs") + "\"\n"
                + "printf plain > \"" + sourceDir.resolve("build/libs/Parpt-1.0-plain.jar") + "\"\n"
                + (writesRunnableJar
                        ? "printf runnable > \"" + sourceDir.resolve("build/libs/Parpt-1.0.jar") + "\"\n"
                        : "");
        Path gradlew = sourceDir.resolve("gradlew");
        Files.writeString(gradlew, script);
        Files.setPosixFilePermissions(gradlew, PosixFilePermissions.fromString("rwxr-xr-x"));
    }

    private Result runInstall(Map<String, String> extraEnv) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("sh", sourceDir.resolve("install.sh").toString())
                .redirectErrorStream(true);
        Map<String, String> env = builder.environment();
        env.remove("PARPT_INSTALL_DIR");
        env.remove("PARPT_BIN_DIR");
        env.put("HOME", home.toString());
        env.putAll(extraEnv);
        Process process = builder.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(process.waitFor(30, TimeUnit.SECONDS), "install.sh did not finish");
        return new Result(process.exitValue(), output);
    }

    private record Result(int exitCode, String output) {
    }
}
