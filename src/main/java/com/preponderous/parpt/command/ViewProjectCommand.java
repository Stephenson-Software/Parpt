package com.preponderous.parpt.command;

import com.preponderous.parpt.domain.Project;
import com.preponderous.parpt.export.ScoreDescriptionProvider;
import com.preponderous.parpt.repo.ProjectRepository;
import com.preponderous.parpt.score.ScoreCalculator;
import com.preponderous.parpt.service.ProjectService;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
public class ViewProjectCommand {

    private final ProjectService projectService;
    private final ScoreCalculator scoreCalculator;
    private final ScoreDescriptionProvider scoreDescriptionProvider;

    public ViewProjectCommand(ProjectService projectService, ScoreCalculator scoreCalculator, ScoreDescriptionProvider scoreDescriptionProvider) {
        this.projectService = projectService;
        this.scoreCalculator = scoreCalculator;
        this.scoreDescriptionProvider = scoreDescriptionProvider;
    }

    @ShellMethod(key = "view", value = "Views a specific project by name.")
    public String execute(String projectName) {
        Project project;
        try {
            project = projectService.getProject(projectName);
        } catch (ProjectRepository.ProjectNotFoundException e) {
            return "Project not found: " + projectName;
        }

        return String.format("Project: %s\n", project.getName()) +
                String.format("Description: %s\n", project.getDescription()) +
                String.format("Impact: %s\n", scoreDescriptionProvider.getDescription(project.getImpact())) +
                String.format("Confidence: %s\n", scoreDescriptionProvider.getDescription(project.getConfidence())) +
                String.format("Ease: %s\n", scoreDescriptionProvider.getDescription(project.getEase())) +
                String.format("Reach: %s\n", scoreDescriptionProvider.getDescription(project.getReach())) +
                String.format("Effort: %s\n", scoreDescriptionProvider.getDescription(project.getEffort())) +
                String.format("ICE Score: %.2f\n", scoreCalculator.ice(project)) +
                String.format("RICE Score: %.2f\n", scoreCalculator.rice(project));
    }
}
