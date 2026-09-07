package com.preponderous.parpt.command;

import com.preponderous.parpt.config.PromptProperties;
import com.preponderous.parpt.score.ScoreCalculator;
import com.preponderous.parpt.service.ProjectService;
import com.preponderous.parpt.util.ConsoleInputProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest
class CreateProjectCommandTest {

    @Autowired
    ProjectService projectService;

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
}