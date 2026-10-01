package com.preponderous.parpt.command;

import com.preponderous.parpt.domain.Project;
import com.preponderous.parpt.repo.ProjectJsonReaderWriter;
import com.preponderous.parpt.repo.ProjectRepository;
import com.preponderous.parpt.score.ScoreCalculator;
import com.preponderous.parpt.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class UpdateProjectCommandTest {

    UpdateProjectCommand updateProjectCommand;

    @Autowired
    ProjectService projectService;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    ProjectJsonReaderWriter projectJsonReaderWriter;

    @Autowired
    ScoreCalculator scoreCalculator;

    @BeforeEach
    void setUp() {
        // Initialize the command with the project service and score calculator
        updateProjectCommand = new UpdateProjectCommand(projectService, scoreCalculator);

        // Clear any existing projects in the repository before each test
        projectRepository.clear();
    }

    @Test
    void shouldReturnProjectNotFoundWhenProjectDoesNotExist() {
        // Given no projects exist

        // When the command is executed with a non-existing project name
        var result = updateProjectCommand.execute("NonExistingProject", "New description", null, null, null, null, null);

        // Then the result should indicate that the project was not found
        assertEquals("Project not found: NonExistingProject", result);
    }

    @Test
    void shouldUpdateOnlyTheGivenFields() throws Exception {
        // Given a project exists
        projectService.createProject("Test Project", "Original description", 1, 2, 3, 4, 5);

        // When the command changes the description and two of the scores
        var result = updateProjectCommand.execute("Test Project", "Updated description", 5, null, null, 1, null);

        // Then the given fields should change and the rest should keep their values
        Project project = projectService.getProject("Test Project");
        assertEquals("Updated description", project.getDescription());
        assertEquals(5, project.getImpact());
        assertEquals(2, project.getConfidence());
        assertEquals(3, project.getEase());
        assertEquals(1, project.getReach());
        assertEquals(5, project.getEffort());
        assertEquals(String.format("Project updated: Test Project\nICE Score: %.2f\nRICE Score: %.2f",
                scoreCalculator.ice(project), scoreCalculator.rice(project)), result);
    }

    @Test
    void shouldKeepProjectPositionAmongOtherProjects() throws Exception {
        // Given three projects exist
        projectService.createProject("First", "One", 1, 1, 1, 1, 1);
        projectService.createProject("Second", "Two", 2, 2, 2, 2, 2);
        projectService.createProject("Third", "Three", 3, 3, 3, 3, 3);

        // When the middle project is updated
        updateProjectCommand.execute("Second", null, null, null, null, null, 5);

        // Then the projects should stay in creation order and only the middle one should change
        List<Project> projects = projectService.getProjects();
        assertEquals(List.of("First", "Second", "Third"), projects.stream().map(Project::getName).toList());
        assertEquals(5, projects.get(1).getEffort());
        assertEquals(1, projects.get(0).getEffort());
        assertEquals(3, projects.get(2).getEffort());
    }

    @Test
    void shouldPersistUpdateToStorage() throws Exception {
        // Given a project has been saved to storage
        projectService.createProject("Persisted Project", "Saved to disk", 1, 2, 3, 4, 5);

        // When the project is updated
        updateProjectCommand.execute("Persisted Project", "Changed on disk", null, null, 4, null, null);

        // Then re-reading storage should contain the updated values
        List<Project> stored = projectJsonReaderWriter.readJson();
        assertEquals(1, stored.size());
        assertEquals("Changed on disk", stored.get(0).getDescription());
        assertEquals(4, stored.get(0).getEase());
    }

    @Test
    void shouldRejectUpdateWithNothingToChange() throws Exception {
        // Given a project exists
        projectService.createProject("Test Project", "Original description", 1, 2, 3, 4, 5);

        // When the command is executed without any field to change
        var result = updateProjectCommand.execute("Test Project", null, null, null, null, null, null);

        // Then the result should explain what to provide and the project should be unchanged
        assertEquals("Nothing to update. Provide at least one of --description, --impact, --confidence, --ease, --reach or --effort.", result);
        assertEquals("Original description", projectService.getProject("Test Project").getDescription());
    }

    @Test
    void shouldRejectEmptyDescription() throws Exception {
        // Given a project exists
        projectService.createProject("Test Project", "Original description", 1, 2, 3, 4, 5);

        // When the command is executed with an empty description
        var result = updateProjectCommand.execute("Test Project", "", null, null, null, null, null);

        // Then the update should be rejected and the description kept
        assertEquals("Project description cannot be empty.", result);
        assertEquals("Original description", projectService.getProject("Test Project").getDescription());
    }

    @Test
    void shouldRejectOutOfRangeScoresWithoutChangingAnything() throws Exception {
        // Given a project exists
        projectService.createProject("Test Project", "Original description", 1, 2, 3, 4, 5);

        // When the command is executed with one valid and one out-of-range score
        var tooHigh = updateProjectCommand.execute("Test Project", null, 4, null, null, null, 6);
        var tooLow = updateProjectCommand.execute("Test Project", null, null, 0, null, null, null);

        // Then both updates should be rejected and no score should change
        assertEquals("All scores must be between 1 and 5.", tooHigh);
        assertEquals("All scores must be between 1 and 5.", tooLow);
        Project project = projectService.getProject("Test Project");
        assertEquals(1, project.getImpact());
        assertEquals(2, project.getConfidence());
        assertEquals(5, project.getEffort());
    }
}
