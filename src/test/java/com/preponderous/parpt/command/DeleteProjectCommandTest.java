package com.preponderous.parpt.command;

import com.preponderous.parpt.domain.Project;
import com.preponderous.parpt.repo.ProjectJsonReaderWriter;
import com.preponderous.parpt.repo.ProjectRepository;
import com.preponderous.parpt.service.ProjectService;
import com.preponderous.parpt.trace.UsageReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class DeleteProjectCommandTest {

    DeleteProjectCommand deleteProjectCommand;

    @Autowired
    ProjectService projectService;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    ProjectJsonReaderWriter projectJsonReaderWriter;

    @BeforeEach
    void setUp() {
        // Initialize the command with the project service
        deleteProjectCommand = new DeleteProjectCommand(projectService);

        // Clear any existing projects in the repository before each test
        projectRepository.clear();
    }

    @Test
    void shouldReturnProjectNotFoundWhenProjectDoesNotExist() {
        // Given no projects exist

        // When the command is executed with a non-existing project name
        var result = deleteProjectCommand.execute("NonExistingProject");

        // Then the result should indicate that the project was not found
        assertEquals("Project not found: NonExistingProject", result);
    }

    @Test
    void shouldDeleteProjectWhenProjectExists() throws ProjectRepository.NameTakenException {
        // Given a project exists
        projectService.createProject("Test Project", "This is a test project", 5, 4, 3, 2, 1);

        // When the command is executed with the existing project name
        var result = deleteProjectCommand.execute("Test Project");

        // Then the result should confirm the deletion and the project should be gone
        assertEquals("Project deleted: Test Project", result);
        assertFalse(projectService.isNameTaken("Test Project"));
        assertTrue(projectService.getProjects().isEmpty());
    }

    @Test
    void shouldOnlyDeleteTheNamedProject() throws ProjectRepository.NameTakenException {
        // Given two projects exist
        projectService.createProject("Keep Me", "This project stays", 5, 4, 3, 2, 1);
        projectService.createProject("Remove Me", "This project goes", 1, 2, 3, 4, 5);

        // When one of them is deleted
        deleteProjectCommand.execute("Remove Me");

        // Then only the other project should remain
        List<Project> remaining = projectService.getProjects();
        assertEquals(1, remaining.size());
        assertEquals("Keep Me", remaining.get(0).getName());
    }

    @Test
    void shouldPersistDeletionToStorage() throws ProjectRepository.NameTakenException {
        // Given a project has been saved to storage
        projectService.createProject("Persisted Project", "Saved to disk", 5, 4, 3, 2, 1);
        assertEquals(1, projectJsonReaderWriter.readJson().size());

        // When the project is deleted
        deleteProjectCommand.execute("Persisted Project");

        // Then re-reading storage should no longer contain it
        assertTrue(projectJsonReaderWriter.readJson().isEmpty());
    }

    @Test
    void shouldReturnProjectNotFoundWhenDeletingTwice() throws ProjectRepository.NameTakenException {
        // Given a project exists and has already been deleted once
        projectService.createProject("Once Only", "Deleted a single time", 5, 4, 3, 2, 1);
        deleteProjectCommand.execute("Once Only");

        // When the command is executed again with the same name
        var result = deleteProjectCommand.execute("Once Only");

        // Then the result should indicate that the project was not found
        assertEquals("Project not found: Once Only", result);
    }

    @Test
    void shouldReportFailedSaveWithoutRemovingProject() {
        // Given a project exists and every save fails
        ProjectJsonReaderWriter failingWriter = mock(ProjectJsonReaderWriter.class);
        when(failingWriter.readJson()).thenReturn(new ArrayList<>(List.of(Project.builder()
                .name("Test Project")
                .description("This project stays")
                .impact(5)
                .confidence(4)
                .ease(3)
                .reach(2)
                .effort(1)
                .build())));
        doThrow(new RuntimeException("Failed to write projects to JSON file")).when(failingWriter).writeJson(anyList());
        ProjectService failingService = new ProjectService(new ProjectRepository(failingWriter), mock(UsageReporter.class));
        DeleteProjectCommand command = new DeleteProjectCommand(failingService);

        // When the project is deleted
        var result = command.execute("Test Project");

        // Then the failure should be reported and the project should remain
        assertEquals("An error occurred while deleting the project: Failed to write projects to JSON file", result);
        assertTrue(failingService.isNameTaken("Test Project"));
    }
}
