package com.preponderous.parpt.command;

import com.preponderous.parpt.config.PromptProperties;
import com.preponderous.parpt.domain.Project;
import com.preponderous.parpt.repo.ProjectRepository;
import com.preponderous.parpt.score.ScoreCalculator;
import com.preponderous.parpt.service.ProjectService;
import com.preponderous.parpt.util.ConsoleInputProvider;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class CreateProjectCommand {
    private static final String QUIT_HINT = "Enter 'q' at any prompt to cancel project creation.";
    private static final String CANCELLED_MESSAGE = "Project creation cancelled.";

    private final ProjectService projectService;
    private final ConsoleInputProvider inputProvider;
    private final ScoreCalculator scoreCalculator;
    private final PromptProperties promptProperties;

    public CreateProjectCommand(
            ProjectService projectService,
            ConsoleInputProvider inputProvider,
            ScoreCalculator scoreCalculator,
            PromptProperties promptProperties) {
        this.projectService = projectService;
        this.inputProvider = inputProvider;
        this.scoreCalculator = scoreCalculator;
        this.promptProperties = promptProperties;
    }


    /**
     * Prompts the user and returns their answer, treating a quit token or the end of input as a
     * request to abandon project creation.
     */
    private String readInput(String prompt) throws CreationCancelledException {
        String input = inputProvider.readLine(prompt);
        if (input == null) {
            // No more input is available, e.g. the user pressed Ctrl-D
            throw new CreationCancelledException();
        }
        // Only the quit comparison ignores surrounding whitespace; the answer itself is returned
        // untouched so this change does not alter how any existing answer is interpreted
        String trimmedInput = input.trim();
        if (trimmedInput.equalsIgnoreCase("q") || trimmedInput.equalsIgnoreCase("quit")) {
            throw new CreationCancelledException();
        }
        return input;
    }

    private int getAverageScore(String[] prompts) throws InvalidScoreException, CreationCancelledException {
        int total = 0;
        for (String prompt : prompts) {
            try {
                int score = Integer.parseInt(readInput(prompt));
                if (score < 1 || score > 5) {
                    throw new InvalidScoreException("Invalid score. Must be between 1 and 5.");
                }
                total += score;
            } catch (NumberFormatException e) {
                throw new InvalidScoreException("Invalid score. Must be a number between 1 and 5.");
            }
        }
        return Math.round((float) total / prompts.length);
    }

    /**
     * Re-prompts for a category's scores until every answer is valid, or the user quits.
     */
    private int promptForScore(String[] prompts) throws CreationCancelledException {
        while (true) {
            try {
                return getAverageScore(prompts);
            } catch (InvalidScoreException e) {
                System.out.println(e.getMessage());
            }
        }
    }


    @ShellMethod(key = "create", value = "Creates a new project with the given parameters.")
    public String execute(
            @ShellOption(value = {"-n", "--name"}, help = "The name of the project", defaultValue = ShellOption.NULL) String projectName,
            @ShellOption(value = {"-d", "--description"}, help = "Description of the project", defaultValue = ShellOption.NULL) String projectDescription,
            @ShellOption(value = {"-i", "--impact"}, help = "Impact score (1-5)", defaultValue = ShellOption.NULL) Integer impact,
            @ShellOption(value = {"-c", "--confidence"}, help = "Confidence score (1-5)", defaultValue = ShellOption.NULL) Integer confidence,
            @ShellOption(value = {"-e", "--ease"}, help = "Ease score (1-5)", defaultValue = ShellOption.NULL) Integer ease,
            @ShellOption(value = {"-r", "--reach"}, help = "Reach score (1-5)", defaultValue = ShellOption.NULL) Integer reach,
            @ShellOption(value = {"-f", "--effort"}, help = "Effort score (1-5)", defaultValue = ShellOption.NULL) Integer effort
    ) {
        boolean interactive = projectName == null || projectDescription == null || impact == null
                || confidence == null || ease == null || reach == null || effort == null;
        if (interactive) {
            System.out.println(QUIT_HINT);
        }

        // Interactive input if parameters are not provided
        try {
            if (projectName == null) {
                projectName = readInput(promptProperties.getProjectName());
            }
            if (projectName.isEmpty()) {
                return "Project name cannot be empty.";
            }
            if (projectService.isNameTaken(projectName)) {
                return "Project name '" + projectName + "' is already taken. Please choose a different name.";
            }
            if (projectDescription == null) {
                projectDescription = readInput(promptProperties.getProjectDescription());
            }
            if (projectDescription.isEmpty()) {
                return "Project description cannot be empty.";
            }
            if (impact == null) {
                impact = promptForScore(promptProperties.getImpact());
            }
            if (confidence == null) {
                confidence = promptForScore(promptProperties.getConfidence());
            }
            if (ease == null) {
                ease = promptForScore(promptProperties.getEase());
            }
            if (reach == null) {
                reach = promptForScore(promptProperties.getReach());
            }
            if (effort == null) {
                effort = promptForScore(promptProperties.getEffort());
            }
        } catch (CreationCancelledException e) {
            return CANCELLED_MESSAGE;
        }

        if (impact < 1 || impact > 5 || confidence < 1 || confidence > 5 ||
                ease < 1 || ease > 5 || reach < 1 || reach > 5 || effort < 1 || effort > 5) {
            return "All scores must be between 1 and 5.";
        }

        // Create the project using the service
        Project project;
        try {
            project = projectService.createProject(projectName, projectDescription, impact, confidence, ease, reach, effort);
        } catch (ProjectRepository.NameTakenException e) {
            return "Project name '" + projectName + "' is already taken. Please choose a different name.";
        } catch (Exception e) {
            return "An error occurred while creating the project: " + e.getMessage();
        }

        // Calculate scores
        double iceScore = scoreCalculator.ice(project);
        double riceScore = scoreCalculator.rice(project);
        return String.format("Project created successfully: %s\nICE Score: %.2f\nRICE Score: %.2f", projectName, iceScore, riceScore);
    }

    public class InvalidScoreException extends Exception {
        public InvalidScoreException(String message) {
            super(message);
        }
    }

    /**
     * Signals that the user asked to stop before the project was created. Kept private and static
     * because it is an internal control signal rather than part of the command's surface.
     */
    private static class CreationCancelledException extends Exception {
    }
}