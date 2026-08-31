
package com.preponderous.parpt.repo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.preponderous.parpt.domain.Project;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
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
            objectMapper.writeValue(file, projects);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write projects to JSON file", e);
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
            throw new RuntimeException("Failed to read projects from JSON file", e);
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