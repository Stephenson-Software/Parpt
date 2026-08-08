package com.preponderous.parpt.command;

import com.preponderous.parpt.domain.Project;
import com.preponderous.parpt.export.ProjectSorter;
import com.preponderous.parpt.score.ScoreCalculator;
import com.preponderous.parpt.service.ProjectService;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.List;
import java.util.Locale;

@ShellComponent
public class ListProjectsCommand {

    /**
     * The sort option that leaves projects in the order they were created.
     */
    static final String SORT_NONE = "none";

    private final ProjectService projectService;
    private final ScoreCalculator scoreCalculator;
    private final ProjectSorter projectSorter;

    public ListProjectsCommand(ProjectService projectService, ScoreCalculator scoreCalculator, ProjectSorter projectSorter) {
        this.projectService = projectService;
        this.scoreCalculator = scoreCalculator;
        this.projectSorter = projectSorter;
    }

    @ShellMethod(key = {"list", "ls"}, value = "Lists all projects, optionally sorted by a chosen field.")
    public String execute(
            @ShellOption(value = {"-s", "--sort"}, help = "Sort by 'name', 'impact', 'confidence', 'ease', 'reach', 'effort', 'ice' or 'rice' (default: creation order)", defaultValue = SORT_NONE) String sortBy
    ) {
        List<Project> projects = projectService.getProjects();

        if (projects.isEmpty()) {
            return "No projects found.";
        }

        boolean sorted = !SORT_NONE.equalsIgnoreCase(sortBy);

        if (sorted) {
            if (!ProjectSorter.isSupportedSortKey(sortBy)) {
                return String.format("Invalid sort option. Use one of: %s (or omit --sort to keep creation order).",
                        String.join(", ", ProjectSorter.getSupportedSortKeys()));
            }
            projects = projectSorter.sortBy(projects, sortBy);
        }

        StringBuilder result = new StringBuilder(sorted
                ? String.format("Projects (sorted by %s):\n", sortBy.toLowerCase(Locale.ROOT))
                : "Projects:\n");
        for (Project project : projects) {
            result.append(String.format("- %s: %s (ICE: %s | RICE: %s)\n", project.getName(), project.getDescription(), scoreCalculator.ice(project), scoreCalculator.rice(project)));
        }

        return result.toString();
    }
}
