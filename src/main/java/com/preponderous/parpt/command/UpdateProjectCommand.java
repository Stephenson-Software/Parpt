package com.preponderous.parpt.command;

import com.preponderous.parpt.domain.Project;
import com.preponderous.parpt.repo.ProjectRepository;
import com.preponderous.parpt.score.ScoreCalculator;
import com.preponderous.parpt.service.ProjectService;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.stream.Stream;

@ShellComponent
public class UpdateProjectCommand {

    private final ProjectService projectService;
    private final ScoreCalculator scoreCalculator;

    public UpdateProjectCommand(ProjectService projectService, ScoreCalculator scoreCalculator) {
        this.projectService = projectService;
        this.scoreCalculator = scoreCalculator;
    }

    private static boolean isOutOfRange(Integer score) {
        return score != null && (score < 1 || score > 5);
    }

    @ShellMethod(key = "update", value = "Updates the description or scores of a specific project by name.")
    public String execute(
            @ShellOption(help = "The name of the project to update") String projectName,
            @ShellOption(value = {"-d", "--description"}, help = "New description of the project", defaultValue = ShellOption.NULL) String description,
            @ShellOption(value = {"-i", "--impact"}, help = "New impact score (1-5)", defaultValue = ShellOption.NULL) Integer impact,
            @ShellOption(value = {"-c", "--confidence"}, help = "New confidence score (1-5)", defaultValue = ShellOption.NULL) Integer confidence,
            @ShellOption(value = {"-e", "--ease"}, help = "New ease score (1-5)", defaultValue = ShellOption.NULL) Integer ease,
            @ShellOption(value = {"-r", "--reach"}, help = "New reach score (1-5)", defaultValue = ShellOption.NULL) Integer reach,
            @ShellOption(value = {"-f", "--effort"}, help = "New effort score (1-5)", defaultValue = ShellOption.NULL) Integer effort
    ) {
        if (description == null && Stream.of(impact, confidence, ease, reach, effort).allMatch(score -> score == null)) {
            return "Nothing to update. Provide at least one of --description, --impact, --confidence, --ease, --reach or --effort.";
        }
        if (description != null && description.isEmpty()) {
            return "Project description cannot be empty.";
        }
        if (Stream.of(impact, confidence, ease, reach, effort).anyMatch(UpdateProjectCommand::isOutOfRange)) {
            return "All scores must be between 1 and 5.";
        }

        Project project;
        try {
            project = projectService.updateProject(projectName, description, impact, confidence, ease, reach, effort);
        } catch (ProjectRepository.ProjectNotFoundException e) {
            return "Project not found: " + projectName;
        }

        double iceScore = scoreCalculator.ice(project);
        double riceScore = scoreCalculator.rice(project);
        return String.format("Project updated: %s\nICE Score: %.2f\nRICE Score: %.2f", projectName, iceScore, riceScore);
    }
}
