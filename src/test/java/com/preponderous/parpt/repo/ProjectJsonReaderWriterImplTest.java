
package com.preponderous.parpt.repo;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.preponderous.parpt.domain.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

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
    void readJson_WhenFileDoesNotExist_ShouldReturnEmptyListWithoutCreatingIt() {
        // Arrange
        Path projectsFile = tempDir.resolve("missing.json");
        ProjectJsonReaderWriterImpl missingFileReaderWriter =
                new ProjectJsonReaderWriterImpl(projectsFile.toString(), objectMapper);

        // Act
        List<Project> projects = missingFileReaderWriter.readJson();

        // Assert
        assertThat(projects)
                .isNotNull()
                .isEmpty();
        assertThat(projectsFile).doesNotExist();
    }

    @Test
    void readJson_WhenFileIsEmpty_ShouldReturnEmptyList() throws IOException {
        // Arrange
        Path projectsFile = Files.createFile(tempDir.resolve("empty.json"));
        ProjectJsonReaderWriterImpl emptyFileReaderWriter =
                new ProjectJsonReaderWriterImpl(projectsFile.toString(), objectMapper);

        // Act
        List<Project> projects = emptyFileReaderWriter.readJson();

        // Assert
        assertThat(projects)
                .isNotNull()
                .isEmpty();
    }

    @Test
    void readJson_WhenFileIsMalformed_ShouldThrowWithCause() throws IOException {
        // Arrange
        Path projectsFile = Files.writeString(tempDir.resolve("malformed.json"), "{not valid json");
        ProjectJsonReaderWriterImpl malformedFileReaderWriter =
                new ProjectJsonReaderWriterImpl(projectsFile.toString(), objectMapper);

        // Act
        RuntimeException exception = assertThrows(RuntimeException.class, malformedFileReaderWriter::readJson);

        // Assert
        assertThat(exception.getMessage())
                .startsWith("Failed to read projects from " + projectsFile + ": ")
                .contains(exception.getCause().getMessage());
        assertThat(exception.getCause()).isInstanceOf(IOException.class);
    }

    @Test
    void writeJson_WhenParentPathIsAFile_ShouldThrowNamingTheDirectory() throws IOException {
        // Arrange
        Path blockingFile = Files.writeString(tempDir.resolve("not-a-directory"), "occupied");
        Path projectsFile = blockingFile.resolve("projects.json");
        ProjectJsonReaderWriterImpl blockedReaderWriter =
                new ProjectJsonReaderWriterImpl(projectsFile.toString(), objectMapper);

        // Act
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> blockedReaderWriter.writeJson(List.of(project1)));

        // Assert
        assertThat(exception.getMessage())
                .isEqualTo("Failed to create directory for projects file: " + blockingFile);
        assertThat(blockingFile).isRegularFile().hasContent("occupied");
    }

    @Test
    void writeJson_WhenFilePathIsADirectory_ShouldThrowWithCause() throws IOException {
        // Arrange
        Path projectsDirectory = Files.createDirectory(tempDir.resolve("projects.json"));
        ProjectJsonReaderWriterImpl directoryReaderWriter =
                new ProjectJsonReaderWriterImpl(projectsDirectory.toString(), objectMapper);

        // Act
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> directoryReaderWriter.writeJson(List.of(project1)));

        // Assert
        assertThat(exception.getMessage())
                .startsWith("Failed to write projects to " + projectsDirectory + ": ")
                .contains(exception.getCause().getMessage());
        assertThat(exception.getCause()).isInstanceOf(IOException.class);
        assertThat(projectsDirectory).isDirectory();
        assertThat(tempDir).isDirectoryNotContaining("glob:**.tmp");
    }

    @Test
    void writeJson_WhenWriteFailsPartWay_ShouldLeaveExistingFileIntact() throws IOException {
        // Arrange
        Path projectsFile = tempDir.resolve("projects.json");
        new ProjectJsonReaderWriterImpl(projectsFile.toString(), objectMapper).writeJson(List.of(project1));
        String savedJson = Files.readString(projectsFile);

        ObjectMapper failingMapper = new ObjectMapper().registerModule(
                new SimpleModule().addSerializer(Project.class, new FailsPartWaySerializer()));
        ProjectJsonReaderWriterImpl failingReaderWriter =
                new ProjectJsonReaderWriterImpl(projectsFile.toString(), failingMapper);

        // Act
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> failingReaderWriter.writeJson(List.of(project1, project2)));

        // Assert
        assertThat(projectsFile).hasContent(savedJson);
        assertThat(tempDir).isDirectoryNotContaining("glob:**.tmp");
        assertThat(exception.getMessage())
                .isEqualTo("Failed to write projects to " + projectsFile + ": disk full");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void writeJson_WhenFileHasRestrictedPermissions_ShouldKeepThem() throws IOException {
        // Arrange
        Path projectsFile = Files.writeString(tempDir.resolve("projects.json"), "[]");
        Set<PosixFilePermission> ownerOnly = PosixFilePermissions.fromString("rw-------");
        Files.setPosixFilePermissions(projectsFile, ownerOnly);
        ProjectJsonReaderWriterImpl restrictedReaderWriter =
                new ProjectJsonReaderWriterImpl(projectsFile.toString(), objectMapper);

        // Act
        restrictedReaderWriter.writeJson(List.of(project1));

        // Assert
        assertThat(Files.getPosixFilePermissions(projectsFile)).isEqualTo(ownerOnly);
        assertThat(restrictedReaderWriter.readJson()).hasSize(1);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void writeJson_WhenFileIsASymbolicLink_ShouldWriteThroughTheLink() throws IOException {
        // Arrange
        Path realFile = Files.writeString(tempDir.resolve("real-projects.json"), "[]");
        Path link = Files.createSymbolicLink(tempDir.resolve("projects.json"), realFile);
        ProjectJsonReaderWriterImpl linkedReaderWriter =
                new ProjectJsonReaderWriterImpl(link.toString(), objectMapper);

        // Act
        linkedReaderWriter.writeJson(List.of(project1));

        // Assert
        assertThat(link).isSymbolicLink();
        assertThat(new ProjectJsonReaderWriterImpl(realFile.toString(), objectMapper).readJson())
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

    /**
     * Starts writing a project and then fails, the way a full disk fails a save after part of the
     * JSON has already gone out.
     */
    private static class FailsPartWaySerializer extends JsonSerializer<Project> {
        @Override
        public void serialize(Project project, JsonGenerator generator, SerializerProvider serializers)
                throws IOException {
            generator.writeStartObject();
            generator.writeStringField("name", project.getName());
            generator.flush();
            throw new IOException("disk full");
        }
    }
}