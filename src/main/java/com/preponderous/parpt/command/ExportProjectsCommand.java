package com.preponderous.parpt.command;

import com.preponderous.parpt.domain.Project;
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

    @ShellMethod(key = "export", value = "Exports all projects to Markdown format, sorted by score.")
    public String execute(
            @ShellOption(value = {"-s", "--sort"}, help = "Sort by 'ice' or 'rice' score (default: ice)", defaultValue = "ice") String sortBy
    ) {
        List<Project> projects = projectService.getProjects();

        if (projects.isEmpty()) {
            return "No projects found to export.";
        }

        boolean sortByRice = "rice".equalsIgnoreCase(sortBy);
        
        if (!sortByRice && !"ice".equalsIgnoreCase(sortBy)) {
            return "Invalid sort option. Use 'ice' or 'rice'.";
        }

        try {
            markdownWriter.writeMarkdown(projects, sortByRice);
            String scoreType = sortByRice ? "RICE" : "ICE";
            return String.format("Successfully exported %d projects to projects.md, sorted by %s score.", 
                    projects.size(), scoreType);
        } catch (Exception e) {
            return "Failed to export projects: " + e.getMessage();
        }
    }
}