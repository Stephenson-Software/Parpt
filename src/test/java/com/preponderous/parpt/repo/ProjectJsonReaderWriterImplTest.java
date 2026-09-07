
package com.preponderous.parpt.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.preponderous.parpt.domain.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@TestPropertySource(properties = {
        "app.projects.file=test-projects.json"
})
class ProjectJsonReaderWriterImplTest {

    @Autowired
    private ProjectJsonReaderWriterImpl readerWriter;

    @Autowired
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    private Project project1;
    private Project project2;
    private Project project3;

    @BeforeEach
    void setUp() {
        project1 = Project.builder()
                .name("Project 1")
                .description("First test project")
                .impact(4)
                .confidence(5)
                .ease(3)
                .reach(4)
                .effort(2)
                .build();

        project2 = Project.builder()
                .name("Project 2")
                .description("Second test project")
                .impact(3)
                .confidence(4)
                .ease(4)
                .reach(3)
                .effort(3)
                .build();

        project3 = Project.builder()
                .name("Project 3")
                .description("Third test project")
                .impact(5)
                .confidence(3)
                .ease(2)
                .reach(5)
                .effort(4)
                .build();

        // wipe file
        readerWriter.writeJson(List.of());
    }

    @Test
    void writeJson_ShouldStoreProjects() {
        // Arrange
        List<Project> projects = Arrays.asList(project1, project2, project3);

        // Act
        readerWriter.writeJson(projects);
        List<Project> readProjects = readerWriter.readJson();

        // Assert
        assertThat(readProjects)
                .isNotNull()
                .hasSize(3)
                .usingRecursiveComparison()
                .isEqualTo(projects);
    }

    @Test
    void readJson_WhenNoProjectsWritten_ShouldReturnEmptyList() {
        // Act
        List<Project> projects = readerWriter.readJson();

        // Assert
        assertThat(projects)
                .isNotNull()
                .isEmpty();
    }

    @Test
    void writeJson_WithNullList_ShouldThrowException() {
        // Assert
        assertThrows(IllegalArgumentException.class, () -> {
            // Act
            readerWriter.writeJson(null);
        });
    }

    @Test
    void writeJson_WithEmptyList_ShouldWriteEmptyList() {
        // Arrange
        List<Project> emptyList = List.of();

        // Act
        readerWriter.writeJson(emptyList);
        List<Project> readProjects = readerWriter.readJson();

        // Assert
        assertThat(readProjects)
                .isNotNull()
                .isEmpty();
    }

    @Test
    void writeJson_WhenParentDirectoryIsMissing_ShouldCreateIt() {
        // Arrange
        Path missingDirectory = tempDir.resolve("parpt-data").resolve("nested");
        Path projectsFile = missingDirectory.resolve("projects.json");
        ProjectJsonReaderWriterImpl nestedReaderWriter =
                new ProjectJsonReaderWriterImpl(projectsFile.toString(), objectMapper);

        // Act
        nestedReaderWriter.writeJson(List.of(project1));

        // Assert
        assertThat(missingDirectory).isDirectory();
        assertThat(projectsFile).isRegularFile();
        assertThat(nestedReaderWriter.readJson())
                .hasSize(1)
                .usingRecursiveComparison()
                .isEqualTo(List.of(project1));
    }

    @Test
    void writeJson_MultipleCalls_ShouldOverwritePreviousData() {
        // Arrange
        List<Project> firstProjects = Arrays.asList(project1, project2);
        List<Project> secondProjects = Arrays.asList(project2, project3);

        // Act
        readerWriter.writeJson(firstProjects);
        readerWriter.writeJson(secondProjects);
        List<Project> readProjects = readerWriter.readJson();

        // Assert
        assertThat(readProjects)
                .isNotNull()
                .hasSize(2)
                .usingRecursiveComparison()
                .isEqualTo(secondProjects);

        assertThat(readProjects)
                .extracting(Project::getName)
                .doesNotContain(project1.getName());
    }

    @Test
    void writeJson_ShouldPreserveAllProjectFields() {
        // Arrange
        List<Project> projects = List.of(project1);

        // Act
        readerWriter.writeJson(projects);
        List<Project> readProjects = readerWriter.readJson();

        // Assert
        assertThat(readProjects)
                .isNotNull()
                .hasSize(1);

        Project readProject = readProjects.get(0);
        assertThat(readProject)
                .usingRecursiveComparison()
                .isEqualTo(project1);

        assertThat(readProject.getName()).isEqualTo(project1.getName());
        assertThat(readProject.getDescription()).isEqualTo(project1.getDescription());
        assertThat(readProject.getImpact()).isEqualTo(project1.getImpact());
        assertThat(readProject.getConfidence()).isEqualTo(project1.getConfidence());
        assertThat(readProject.getEase()).isEqualTo(project1.getEase());
        assertThat(readProject.getReach()).isEqualTo(project1.getReach());
        assertThat(readProject.getEffort()).isEqualTo(project1.getEffort());
    }
}