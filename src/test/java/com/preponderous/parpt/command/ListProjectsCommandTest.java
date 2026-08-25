package com.preponderous.parpt.command;

import com.preponderous.parpt.export.ProjectSorter;
import com.preponderous.parpt.repo.ProjectRepository;
import com.preponderous.parpt.score.ScoreCalculator;
import com.preponderous.parpt.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ListProjectsCommandTest {

    ListProjectsCommand listProjectsCommand;

    @Autowired
    ProjectService projectService;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    ScoreCalculator scoreCalculator;

    @Autowired
    ProjectSorter projectSorter;

    @BeforeEach
    void setUp() {
        // Initialize the command with the project service
        listProjectsCommand = new ListProjectsCommand(projectService, scoreCalculator, projectSorter);

        // Clear any existing projects in the repository before each test
        projectRepository.clear();
    }

    @Test
    void shouldReturnEmptyListWhenNoProjectsExist() {
        // Given no projects exist

        // When the command is executed
        var result = listProjectsCommand.execute(ListProjectsCommand.SORT_NONE);

        // Then the result should be an empty list
        assertTrue(result.contains("No projects found."));
    }

    @Test
    void shouldReturnListOfProjectsWhenProjectsExist() throws ProjectRepository.NameTakenException {
        // Given some projects exist
        projectService.createProject("Project A", "Description A", 5, 4, 3, 2, 1);
        projectService.createProject("Project B", "Description B", 4, 3, 2, 1, 5);

        // When the command is executed
        var result = listProjectsCommand.execute(ListProjectsCommand.SORT_NONE);

        // Then the result should contain the projects
        assertTrue(result.contains("Projects:"));
        assertTrue(result.contains("Project A: Description A"));
        assertTrue(result.contains("Project B: Description B"));
    }

    @Test
    void shouldPreserveCreationOrderWhenNoSortOptionIsGiven() throws ProjectRepository.NameTakenException {
        // Given projects are created out of alphabetical and score order
        projectService.createProject("Zebra", "Created first", 1, 1, 1, 1, 1);
        projectService.createProject("Apple", "Created second", 5, 5, 5, 5, 1);

        // When the command is executed without a sort option
        var result = listProjectsCommand.execute(ListProjectsCommand.SORT_NONE);

        // Then the projects appear in creation order and the header is unqualified
        assertTrue(result.contains("Projects:"));
        assertTrue(result.indexOf("Zebra") < result.indexOf("Apple"));
    }

    @Test
    void shouldSortByNameWhenNameSortIsRequested() throws ProjectRepository.NameTakenException {
        // Given projects are created out of alphabetical order
        projectService.createProject("Zebra", "Created first", 1, 1, 1, 1, 1);
        projectService.createProject("Apple", "Created second", 5, 5, 5, 5, 1);

        // When the command is executed with the name sort option
        var result = listProjectsCommand.execute("name");

        // Then the projects are listed alphabetically
        assertTrue(result.contains("Projects (sorted by name):"));
        assertTrue(result.indexOf("Apple") < result.indexOf("Zebra"));
    }

    @Test
    void shouldSortByIceWhenIceSortIsRequested() throws ProjectRepository.NameTakenException {
        // Given a low-scoring project is created before a high-scoring one
        projectService.createProject("Low", "Low ICE", 1, 1, 1, 1, 1);
        projectService.createProject("High", "High ICE", 5, 5, 5, 1, 1);

        // When the command is executed with the ice sort option
        var result = listProjectsCommand.execute("ice");

        // Then the highest ICE score is listed first
        assertTrue(result.contains("Projects (sorted by ice):"));
        assertTrue(result.indexOf("High") < result.indexOf("Low"));
    }

    @Test
    void shouldAcceptSortKeyRegardlessOfCase() throws ProjectRepository.NameTakenException {
        // Given projects are created out of alphabetical order
        projectService.createProject("Zebra", "Created first", 1, 1, 1, 1, 1);
        projectService.createProject("Apple", "Created second", 5, 5, 5, 5, 1);

        // When the command is executed with an upper-case sort option
        var result = listProjectsCommand.execute("NAME");

        // Then the sort is applied and the header reports the key in lower case
        assertTrue(result.contains("Projects (sorted by name):"));
        assertTrue(result.indexOf("Apple") < result.indexOf("Zebra"));
    }

    @Test
    void shouldRejectUnsupportedSortKey() throws ProjectRepository.NameTakenException {
        // Given a project exists
        projectService.createProject("Project A", "Description A", 5, 4, 3, 2, 1);

        // When the command is executed with an unsupported sort option
        var result = listProjectsCommand.execute("bogus");

        // Then an error naming the supported options is returned and nothing is listed
        assertTrue(result.contains("Invalid sort option."));
        assertTrue(result.contains("name, impact, confidence, ease, reach, effort, ice, rice"));
        assertFalse(result.contains("Project A: Description A"));
    }

    @Test
    void shouldExposeLsAsAnAliasForTheListCommand() throws NoSuchMethodException {
        // Given the shell method backing the list command
        ShellMethod shellMethod = ListProjectsCommand.class
                .getMethod("execute", String.class)
                .getAnnotation(ShellMethod.class);

        // Then both the primary key and the alias are registered
        assertEquals(2, shellMethod.key().length);
        assertTrue(Arrays.asList(shellMethod.key()).contains("list"));
        assertTrue(Arrays.asList(shellMethod.key()).contains("ls"));
    }

    @Test
    void shouldDocumentTheSupportedSortKeysAndTheDefaultInTheSortOptionHelpText() throws NoSuchMethodException {
        // Given the help text of the --sort option
        String help = ListProjectsCommand.class
                .getMethod("execute", String.class)
                .getParameters()[0]
                .getAnnotation(ShellOption.class)
                .help();

        // Then it names every supported sort key and reports the list command's default
        assertTrue(help.contains(ProjectSorter.SUPPORTED_SORT_KEYS_HELP));
        assertTrue(help.contains("default: creation order"));
    }
}
