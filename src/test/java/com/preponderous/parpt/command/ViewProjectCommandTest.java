package com.preponderous.parpt.command;

import com.preponderous.parpt.export.ScoreDescriptionProvider;
import com.preponderous.parpt.repo.ProjectRepository;
import com.preponderous.parpt.score.ScoreCalculator;
import com.preponderous.parpt.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ViewProjectCommandTest {

    ViewProjectCommand viewProjectCommand;

    @Autowired
    ProjectService projectService;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    ScoreCalculator scoreCalculator;

    @Autowired
    ScoreDescriptionProvider scoreDescriptionProvider;

    @BeforeEach
    void setUp() {
        // Initialize the command with the project service
        viewProjectCommand = new ViewProjectCommand(projectService, scoreCalculator, scoreDescriptionProvider);

        // Clear any existing projects in the repository before each test
        projectRepository.clear();
    }

    @Test
    void shouldReturnProjectNotFoundWhenProjectDoesNotExist() {
        // Given no projects exist

        // When the command is executed with a non-existing project name
        var result = viewProjectCommand.execute("NonExistingProject");

        // Then the result should indicate that the project was not found
        assertTrue(result.contains("Project not found: NonExistingProject"));
    }

    @Test
    void shouldReturnProjectDetailsWhenProjectExists() throws ProjectRepository.NameTakenException {
        // Given a project exists
        projectService.createProject("Test Project", "This is a test project", 5, 4, 3, 2, 1);

        // When the command is executed with the existing project name
        var result = viewProjectCommand.execute("Test Project");

        // Then the result should contain the project details
        assertTrue(result.contains("Project: Test Project"));
        assertTrue(result.contains("Description: This is a test project"));
        assertTrue(result.contains("Impact: very high"));
        assertTrue(result.contains("Confidence: high"));
        assertTrue(result.contains("Ease: medium"));
        assertTrue(result.contains("Reach: low"));
        assertTrue(result.contains("Effort: very low"));
        assertTrue(result.contains("ICE Score: 60.00\n"));
        assertTrue(result.contains("RICE Score: 40.00\n"));
    }

    @Test
    void shouldShowRawNumberForScoreOutsideOneToFive() throws ProjectRepository.NameTakenException {
        // Given a project whose impact is outside the 1-5 range, as a hand-edited projects.json could hold
        projectService.createProject("Out Of Range", "Impact beyond the scale", 7, 4, 3, 2, 1);

        // When the command is executed
        var result = viewProjectCommand.execute("Out Of Range");

        // Then the impact is shown as its raw number rather than a description
        assertTrue(result.contains("Impact: 7\n"));
        assertTrue(result.contains("Confidence: high\n"));
    }
}