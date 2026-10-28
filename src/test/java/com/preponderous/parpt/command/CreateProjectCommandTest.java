package com.preponderous.parpt.command;

import com.preponderous.parpt.config.PromptProperties;
import com.preponderous.parpt.domain.Project;
import com.preponderous.parpt.repo.ProjectRepository;
import com.preponderous.parpt.score.ScoreCalculator;
import com.preponderous.parpt.service.ProjectService;
import com.preponderous.parpt.util.ConsoleInputProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest
class CreateProjectCommandTest {

    @Autowired
    ProjectService projectService;

    @Autowired
    ProjectRepository projectRepository;

    @Mock
    ConsoleInputProvider inputProvider;

    @Autowired
    ScoreCalculator scoreCalculator;

    @Autowired
    PromptProperties promptProperties;

    private CreateProjectCommand command;

    @BeforeEach
    void setUp() {
        command = new CreateProjectCommand(projectService, inputProvider, scoreCalculator, promptProperties);

        // Clear any existing projects in the repository before each test
        projectRepository.clear();
    }

    @Test
    void testExecuteWithAllArgumentsProvided() {
        String result = command.execute(
                "Test Project",
                "Test Description",
                5,
                5,
                5,
                5,
                5
        );

        assertEquals("Project created successfully: Test Project\nICE Score: 125.00\nRICE Score: 25.00", result);
        verifyNoInteractions(inputProvider);
    }

    @Test
    void testValidationFailsWithInvalidScores() {
        String result = command.execute(
                "Test Project",
                "Test Description",
                0,  // Invalid: below range
                11, // Invalid: above range
                5,
                5,
                5
        );

        assertEquals("All scores must be between 1 and 5.", result);
    }

    @Test
    void testInteractiveInputWithAllFieldsMissing() {
        // Setup mock responses with test prompts from application.yaml
        when(inputProvider.readLine(promptProperties.getProjectName())).thenReturn("Interactive Project");
        when(inputProvider.readLine(promptProperties.getProjectDescription())).thenReturn("Interactive Description");

        // Mock responses for all score prompts with "4"
        for (String[] prompts : new String[][]{
                promptProperties.getImpact(),
                promptProperties.getConfidence(),
                promptProperties.getEase(),
                promptProperties.getReach(),
                promptProperties.getEffort()
        }) {
            for (String prompt : prompts) {
                when(inputProvider.readLine(prompt)).thenReturn("4");
            }
        }

        String result = command.execute(null, null, null, null, null, null, null);

        // Verify all prompts were called
        verify(inputProvider).readLine(promptProperties.getProjectName());
        verify(inputProvider).readLine(promptProperties.getProjectDescription());

        // Verify all score prompts were called
        for (String[] prompts : new String[][]{
                promptProperties.getImpact(),
                promptProperties.getConfidence(),
                promptProperties.getEase(),
                promptProperties.getReach(),
                promptProperties.getEffort()
        }) {
            for (String prompt : prompts) {
                verify(inputProvider).readLine(prompt);
            }
        }

        assertEquals("Project created successfully: Interactive Project\nICE Score: 64.00\nRICE Score: 16.00", result);
    }

    @Test
    void testPromptConfiguration() {
        // Verify test prompts are loaded correctly
        assertTrue(promptProperties.getProjectName().startsWith("[TEST]"));
        assertTrue(promptProperties.getProjectDescription().startsWith("[TEST]"));

        // Verify all score prompts are properly configured
        assertEquals(4, promptProperties.getImpact().length);
        assertEquals(4, promptProperties.getConfidence().length);
        assertEquals(4, promptProperties.getEase().length);
        assertEquals(4, promptProperties.getReach().length);
        assertEquals(4, promptProperties.getEffort().length);

        // Verify test prefix for each category
        assertTrue(promptProperties.getImpact()[0].startsWith("[TEST]"));
        assertTrue(promptProperties.getConfidence()[0].startsWith("[TEST]"));
        assertTrue(promptProperties.getEase()[0].startsWith("[TEST]"));
        assertTrue(promptProperties.getReach()[0].startsWith("[TEST]"));
        assertTrue(promptProperties.getEffort()[0].startsWith("[TEST]"));
    }

    @Test
    void testQuittingAtNamePromptCancelsCreation() {
        when(inputProvider.readLine(promptProperties.getProjectName())).thenReturn("q");

        String result = command.execute(null, null, null, null, null, null, null);

        assertEquals("Project creation cancelled.", result);
        verify(inputProvider).readLine(promptProperties.getProjectName());
        verify(inputProvider, never()).readLine(promptProperties.getProjectDescription());
    }

    @Test
    void testQuittingAtScorePromptCancelsCreation() {
        when(inputProvider.readLine(promptProperties.getImpact()[0])).thenReturn("quit");

        String result = command.execute(
                "Cancelled Project",
                "Cancelled Description",
                null,
                5,
                5,
                5,
                5
        );

        assertEquals("Project creation cancelled.", result);
        verify(inputProvider).readLine(promptProperties.getImpact()[0]);
        verify(inputProvider, never()).readLine(promptProperties.getImpact()[1]);
        assertFalse(projectService.isNameTaken("Cancelled Project"));
    }

    @Test
    void testEndOfInputCancelsCreation() {
        when(inputProvider.readLine(promptProperties.getProjectDescription())).thenReturn(null);

        String result = command.execute(
                "EOF Project",
                null,
                5,
                5,
                5,
                5,
                5
        );

        assertEquals("Project creation cancelled.", result);
        assertFalse(projectService.isNameTaken("EOF Project"));
    }

    @Test
    void testWhitespaceOnlyNameAtPromptIsRejected() {
        when(inputProvider.readLine(promptProperties.getProjectName())).thenReturn("   ");

        String result = command.execute(null, null, null, null, null, null, null);

        assertEquals("Project name cannot be empty.", result);
        verify(inputProvider, never()).readLine(promptProperties.getProjectDescription());
        assertFalse(projectService.isNameTaken("   "));
    }

    @Test
    void testWhitespaceOnlyDescriptionOptionIsRejected() {
        String result = command.execute(
                "Blank Description Project",
                " \t ",
                5,
                5,
                5,
                5,
                5
        );

        assertEquals("Project description cannot be empty.", result);
        assertFalse(projectService.isNameTaken("Blank Description Project"));
    }

    @Test
    void testInteractiveInputWithPartialArgumentsProvided() {
        // Setup mock responses for missing fields
        when(inputProvider.readLine(promptProperties.getProjectDescription()))
                .thenReturn("Interactive Description");

        // Mock responses for missing scores
        for (String prompt : promptProperties.getEase()) {
            when(inputProvider.readLine(prompt)).thenReturn("3");
        }
        for (String prompt : promptProperties.getEffort()) {
            when(inputProvider.readLine(prompt)).thenReturn("4");
        }

        String result = command.execute(
                "Partial Project",  // Name provided
                null,               // Description missing
                5,                  // Impact provided
                5,                  // Confidence provided
                null,               // Ease missing
                4,                  // Reach provided
                null                // Effort missing
        );

        // Verify only prompts for missing values were called
        verify(inputProvider, never()).readLine(promptProperties.getProjectName());
        verify(inputProvider).readLine(promptProperties.getProjectDescription());
        verify(inputProvider, never()).readLine(promptProperties.getImpact()[0]);
        verify(inputProvider, never()).readLine(promptProperties.getConfidence()[0]);
        verify(inputProvider).readLine(promptProperties.getEase()[0]);
        verify(inputProvider, never()).readLine(promptProperties.getReach()[0]);
        verify(inputProvider).readLine(promptProperties.getEffort()[0]);

        assertEquals("Project created successfully: Partial Project\nICE Score: 75.00\nRICE Score: 25.00", result);
    }

    @Test
    void testCategoryScoreIsTheAverageOfItsAnswersRoundedToTheNearestWholeNumber() throws Exception {
        answerCategory(promptProperties.getImpact(), "1", "2", "2", "2");      // 1.75 -> 2
        answerCategory(promptProperties.getConfidence(), "2", "3", "2", "3");  // 2.5 -> 3
        answerCategory(promptProperties.getEase(), "1", "1", "1", "2");        // 1.25 -> 1
        answerCategory(promptProperties.getReach(), "5", "5", "5", "5");       // 5
        answerCategory(promptProperties.getEffort(), "3", "3", "4", "4");      // 3.5 -> 4

        String result = command.execute("Averaged Project", "Averaged Description", null, null, null, null, null);

        assertEquals("Project created successfully: Averaged Project\nICE Score: 6.00\nRICE Score: 7.50", result);
        Project saved = projectService.getProject("Averaged Project");
        assertEquals(2, saved.getImpact());
        assertEquals(3, saved.getConfidence());
        assertEquals(1, saved.getEase());
        assertEquals(5, saved.getReach());
        assertEquals(4, saved.getEffort());
    }

    @Test
    void testOutOfRangeAnswerRepromptsTheWholeCategory() throws Exception {
        String[] impactPrompts = promptProperties.getImpact();
        when(inputProvider.readLine(impactPrompts[0])).thenReturn("6", "2");
        for (int i = 1; i < impactPrompts.length; i++) {
            when(inputProvider.readLine(impactPrompts[i])).thenReturn("2");
        }

        String output = captureStandardOutput(() -> assertEquals(
                "Project created successfully: Reprompted Project\nICE Score: 50.00\nRICE Score: 10.00",
                command.execute("Reprompted Project", "Reprompted Description", null, 5, 5, 5, 5)));

        assertTrue(output.contains("Invalid score. Must be between 1 and 5."));
        // The invalid first answer abandons the category, so every question is asked again
        verify(inputProvider, times(2)).readLine(impactPrompts[0]);
        for (int i = 1; i < impactPrompts.length; i++) {
            verify(inputProvider, times(1)).readLine(impactPrompts[i]);
        }
        assertEquals(2, projectService.getProject("Reprompted Project").getImpact());
    }

    @Test
    void testNonNumericAnswerReprompts() throws Exception {
        String[] effortPrompts = promptProperties.getEffort();
        when(inputProvider.readLine(effortPrompts[0])).thenReturn("four", "4");
        for (int i = 1; i < effortPrompts.length; i++) {
            when(inputProvider.readLine(effortPrompts[i])).thenReturn("4");
        }

        String output = captureStandardOutput(() -> assertEquals(
                "Project created successfully: Numeric Project\nICE Score: 125.00\nRICE Score: 31.25",
                command.execute("Numeric Project", "Numeric Description", 5, 5, 5, 5, null)));

        assertTrue(output.contains("Invalid score. Must be a number between 1 and 5."));
        verify(inputProvider, times(2)).readLine(effortPrompts[0]);
        assertEquals(4, projectService.getProject("Numeric Project").getEffort());
    }

    @Test
    void testTakenNameAtPromptIsRejectedBeforeAnyFurtherQuestion() throws Exception {
        projectService.createProject("Existing Project", "Original Description", 1, 1, 1, 1, 1);
        when(inputProvider.readLine(promptProperties.getProjectName())).thenReturn("Existing Project");

        String result = command.execute(null, null, null, null, null, null, null);

        assertEquals("Project name 'Existing Project' is already taken. Please choose a different name.", result);
        verify(inputProvider, never()).readLine(promptProperties.getProjectDescription());
        verify(inputProvider, never()).readLine(promptProperties.getImpact()[0]);
        assertEquals(1, projectService.getProjects().size());
        assertEquals("Original Description", projectService.getProject("Existing Project").getDescription());
    }

    @Test
    void testQuitTokenIgnoresCaseAndSurroundingWhitespace() {
        when(inputProvider.readLine(promptProperties.getProjectDescription())).thenReturn("  QuIt \t");

        String result = command.execute("Quit Project", null, 5, 5, 5, 5, 5);

        assertEquals("Project creation cancelled.", result);
        assertFalse(projectService.isNameTaken("Quit Project"));
    }

    private void answerCategory(String[] prompts, String... answers) {
        for (int i = 0; i < prompts.length; i++) {
            when(inputProvider.readLine(prompts[i])).thenReturn(answers[i]);
        }
    }

    private String captureStandardOutput(Runnable action) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setOut(originalOut);
        }
        return captured.toString(StandardCharsets.UTF_8);
    }
}