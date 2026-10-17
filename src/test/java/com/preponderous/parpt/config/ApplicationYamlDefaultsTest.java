package com.preponderous.parpt.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the defaults shipped in src/main/resources/application.yaml. The test classpath has
 * its own application.yaml that shadows the shipped one, so the file is loaded from disk.
 */
class ApplicationYamlDefaultsTest {

    private static final String SHIPPED_CONFIG = "src/main/resources/application.yaml";

    @Test
    void projectsFile_ShouldDefaultToParptDirectoryUnderUserHome() throws IOException {
        StandardEnvironment environment = loadShippedConfig();

        String projectsFile = environment.getProperty("app.projects.file");

        String expected = Paths.get(System.getProperty("user.home"), ".parpt", "projects.json").toString();
        assertThat(Paths.get(projectsFile).toString()).isEqualTo(expected);
    }

    @Test
    void markdownFile_ShouldStayRelativeToWorkingDirectory() throws IOException {
        StandardEnvironment environment = loadShippedConfig();

        assertThat(environment.getProperty("app.projects.markdown-file")).isEqualTo("projects.md");
    }

    private StandardEnvironment loadShippedConfig() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("shipped-application", new FileSystemResource(SHIPPED_CONFIG));
        StandardEnvironment environment = new StandardEnvironment();
        sources.forEach(source -> environment.getPropertySources().addLast(source));
        return environment;
    }
}
