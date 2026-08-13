package com.preponderous.parpt.repo;

import com.preponderous.parpt.domain.Project;
import com.preponderous.parpt.export.MarkdownFormatter;
import com.preponderous.parpt.export.ProjectSorter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Component
public class ProjectMarkdownWriterImpl implements ProjectMarkdownWriter {

    private final String markdownFilePath;
    private final MarkdownFormatter markdownFormatter;
    private final ProjectSorter projectSorter;

    public ProjectMarkdownWriterImpl(
            @Value("${app.projects.markdown-file}") String markdownFilePath,
            MarkdownFormatter markdownFormatter,
            ProjectSorter projectSorter) {
        this.markdownFilePath = markdownFilePath;
        this.markdownFormatter = markdownFormatter;
        this.projectSorter = projectSorter;
    }

    @Override
    public void writeMarkdown(List<Project> projects, String sortKey) {
        if (projects == null) {
            throw new IllegalArgumentException("Projects list cannot be null");
        }

        // Sort up front so an unsupported sort key is rejected before the file is touched
        List<Project> sortedProjects = projectSorter.sortBy(projects, sortKey);

        try (FileWriter writer = new FileWriter(markdownFilePath)) {
            // Write header
            writer.write(markdownFormatter.formatHeader(sortKey));

            if (sortedProjects.isEmpty()) {
                writer.write(markdownFormatter.formatNoProjectsMessage());
                return;
            }

            for (int i = 0; i < sortedProjects.size(); i++) {
                Project project = sortedProjects.get(i);
                writer.write(markdownFormatter.formatProject(project, i + 1));
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to write projects to Markdown file", e);
        }
    }
}