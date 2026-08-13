package com.preponderous.parpt.command;

import com.preponderous.parpt.domain.Project;
import com.preponderous.parpt.export.ProjectSorter;
import com.preponderous.parpt.repo.ProjectMarkdownWriter;
import com.preponderous.parpt.service.ProjectService;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.List;

@ShellComponent
public class ExportProjectsCommand {

    private final ProjectService projectService;
    private final ProjectMarkdownWriter markdownWriter;

    public ExportProjectsCommand(ProjectService projectService, ProjectMarkdownWriter markdownWriter) {
        this.projectService = projectService;
        this.markdownWriter = markdownWriter;
    }

    @ShellMethod(key = "export", value = "Exports all projects to Markdown format, sorted by a chosen field.")
    public String execute(
            @ShellOption(value = {"-s", "--sort"}, help = "Sort by 'name', 'impact', 'confidence', 'ease', 'reach', 'effort', 'ice' or 'rice' (default: ice)", defaultValue = ProjectSorter.DEFAULT_SORT_KEY) String sortBy
    ) {
        List<Project> projects = projectService.getProjects();

        if (projects.isEmpty()) {
            return "No projects found to export.";
        }

        if (!ProjectSorter.isSupportedSortKey(sortBy)) {
            return String.format("Invalid sort option. Use one of: %s.",
                    String.join(", ", ProjectSorter.getSupportedSortKeys()));
        }

        try {
            markdownWriter.writeMarkdown(projects, sortBy);
            return String.format("Successfully exported %d projects to projects.md, sorted by %s.",
                    projects.size(), ProjectSorter.describeSortKey(sortBy));
        } catch (Exception e) {
            return "Failed to export projects: " + e.getMessage();
        }
    }
}