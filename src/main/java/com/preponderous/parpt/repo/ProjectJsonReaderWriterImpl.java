
package com.preponderous.parpt.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.preponderous.parpt.domain.Project;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Component
public class ProjectJsonReaderWriterImpl implements ProjectJsonReaderWriter {

    private final String projectsFilePath;
    private final ObjectMapper objectMapper;

    public ProjectJsonReaderWriterImpl(
            @Value("${app.projects.file}") String projectsFilePath,
            ObjectMapper objectMapper) {
        this.projectsFilePath = projectsFilePath;
        this.objectMapper = objectMapper;
    }

    @Override
    public void writeJson(List<Project> projects) {
        if (projects == null) {
            throw new IllegalArgumentException("Projects list cannot be null");
        }

        File file = new File(projectsFilePath);
        createParentDirectory(file);

        try {
            writeAtomically(file, projects);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write projects to " + projectsFilePath + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Project> readJson() {
        File file = new File(projectsFilePath);

        if (!file.exists() || file.length() == 0) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(file,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Project.class));
        } catch (IOException e) {
            throw new ProjectsFileReadException(projectsFilePath, e);
        }
    }

    /**
     * Writes the projects to a temporary file beside the projects file and then moves it into
     * place, because writing to the projects file directly empties it first, and a full disk or a
     * killed process would then lose every saved project and leave a file that cannot be read.
     */
    private void writeAtomically(File file, List<Project> projects) throws IOException {
        // A symbolic link is written through rather than replaced by the move
        Path target = file.exists() ? file.toPath().toRealPath() : file.toPath();
        Path temporaryFile = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            if (Files.isRegularFile(target)) {
                // Starting from a copy keeps the file's permissions once the move replaces it
                Files.copy(target, temporaryFile, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
            }
            objectMapper.writeValue(temporaryFile.toFile(), projects);
            Files.move(temporaryFile, target, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    /**
     * Creates the directory holding the projects file when it does not already exist, so a
     * configured path such as {@code ~/.parpt/projects.json} works on a first run.
     */
    private void createParentDirectory(File file) {
        File parentDirectory = file.getParentFile();

        // mkdirs() returns false both when the directory already exists and when creation failed,
        // so the isDirectory() check distinguishes the two
        if (parentDirectory != null && !parentDirectory.mkdirs() && !parentDirectory.isDirectory()) {
            throw new RuntimeException(
                    "Failed to create directory for projects file: " + parentDirectory.getPath());
        }
    }
}