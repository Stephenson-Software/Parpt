package com.preponderous.parpt.config;

import com.preponderous.parpt.ParptApplication;
import com.preponderous.parpt.repo.ProjectsFileReadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(OutputCaptureExtension.class)
class ProjectsFileReadFailureAnalyzerTest {

    private static final String HALF_WRITTEN_JSON = "[{\"name\": \"Half";

    @TempDir
    Path tempDir;

    @Test
    void analyze_ShouldNameTheFileAndReasonAndSayItWasNotChanged() {
        ProjectsFileReadException readFailure =
                new ProjectsFileReadException("/data/projects.json", new IOException("Unexpected end-of-input"));

        FailureAnalysis analysis = new ProjectsFileReadFailureAnalyzer()
                .analyze(new BeanCreationException("projectRepository", "Error creating bean", readFailure));

        assertThat(analysis).isNotNull();
        assertThat(analysis.getDescription())
                .isEqualTo("Failed to read projects from /data/projects.json: Unexpected end-of-input");
        assertThat(analysis.getAction())
                .contains("Repair /data/projects.json or move it aside")
                .contains("Parpt has not changed the file")
                .contains("-Dapp.projects.file=<path>");
        assertThat(analysis.getCause()).isSameAs(readFailure);
    }

    @Test
    void analyze_WhenFailureIsUnrelated_ShouldLeaveItToOtherAnalyzers() {
        FailureAnalysis analysis = new ProjectsFileReadFailureAnalyzer()
                .analyze(new IllegalStateException("something else"));

        assertThat(analysis).isNull();
    }

    @Test
    void startup_WhenProjectsFileIsUnreadable_ShouldReportItWithoutStackTrace(CapturedOutput output)
            throws IOException {
        Path projectsFile = Files.writeString(tempDir.resolve("projects.json"), HALF_WRITTEN_JSON);
        SpringApplication application = new SpringApplication(ParptApplication.class);

        assertThrows(Exception.class,
                () -> application.run("--app.projects.file=" + projectsFile).close());

        assertThat(output.getAll())
                .contains("APPLICATION FAILED TO START")
                .contains("Failed to read projects from " + projectsFile + ": ")
                .contains("Repair " + projectsFile + " or move it aside")
                .doesNotContain("Application run failed")
                .doesNotContain("Caused by:");
        assertThat(projectsFile).hasContent(HALF_WRITTEN_JSON);
    }
}
