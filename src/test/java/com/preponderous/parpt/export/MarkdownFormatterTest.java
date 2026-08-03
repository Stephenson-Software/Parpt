package com.preponderous.parpt.export;

import com.preponderous.parpt.domain.Project;
import com.preponderous.parpt.score.ScoreCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarkdownFormatterTest {

    @Mock
    private ScoreCalculator scoreCalculator;
    
    @Mock
    private ScoreDescriptionProvider scoreDescriptionProvider;
    
    private MarkdownFormatter markdownFormatter;

    @BeforeEach
    void setUp() {
        markdownFormatter = new MarkdownFormatter(scoreCalculator, scoreDescriptionProvider);
    }

    @Test
    void formatHeader_WithICESort_ShouldIncludeICEInHeader() {
        // When
        String header = markdownFormatter.formatHeader(false);

        // Then
        assertAll(
                () -> assertTrue(header.contains("# Project Priorities")),
                () -> assertTrue(header.contains("Generated on")),
                () -> assertTrue(header.contains("Sorted by ICE score")),
                () -> assertFalse(header.contains("Sorted by RICE score"))
        );
    }

    @Test
    void formatHeader_WithRICESort_ShouldIncludeRICEInHeader() {
        // When
        String header = markdownFormatter.formatHeader(true);

        // Then
        assertAll(
                () -> assertTrue(header.contains("# Project Priorities")),
                () -> assertTrue(header.contains("Generated on")),
                () -> assertTrue(header.contains("Sorted by RICE score")),
                () -> assertFalse(header.contains("Sorted by ICE score"))
        );
    }

    @Test
    void formatHeader_ShouldIncludeTimestamp() {
        // When
        String header = markdownFormatter.formatHeader(false);

        // Then
        assertTrue(header.contains("Generated on"));
        assertTrue(header.matches("(?s).*Generated on \\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.*"));
    }

    @Test
    void formatProject_ShouldIncludeAllProjectDetails() {
        // Given
        Project project = Project.builder()
                .name("Test Project")
                .description("A test project description")
                .impact(4)
                .confidence(3)
                .ease(5)
                .reach(2)
                .effort(3)
                .build();

        when(scoreCalculator.ice(project)).thenReturn(60.0);
        when(scoreCalculator.rice(project)).thenReturn(8.0);
        when(scoreDescriptionProvider.getDescription(4)).thenReturn("high");
        when(scoreDescriptionProvider.getDescription(3)).thenReturn("medium");
        when(scoreDescriptionProvider.getDescription(5)).thenReturn("very high");
        when(scoreDescriptionProvider.getDescription(2)).thenReturn("low");

        // When
        String result = markdownFormatter.formatProject(project, 1);

        // Then
        assertAll(
                () -> assertTrue(result.contains("## 1. Test Project")),
                () -> assertTrue(result.contains("**Description:** A test project description")),
                () -> assertTrue(result.contains("ICE Score:** 60.00")),
                () -> assertTrue(result.contains("RICE Score:** 8.00")),
                () -> assertTrue(result.contains("**Impact:** 4/5 (high)")),
                () -> assertTrue(result.contains("**Confidence:** 3/5 (medium)")),
                () -> assertTrue(result.contains("**Ease:** 5/5 (very high)")),
                () -> assertTrue(result.contains("**Reach:** 2/5 (low)")),
                () -> assertTrue(result.contains("**Effort:** 3/5 (medium)")),
                () -> assertTrue(result.contains("---"))
        );
    }

    @Test
    void formatProject_ShouldCallScoreCalculatorForBothScores() {
        // Given
        Project project = Project.builder()
                .name("Test Project")
                .description("Test description")
                .impact(1).confidence(1).ease(1).reach(1).effort(1)
                .build();

        when(scoreCalculator.ice(project)).thenReturn(1.0);
        when(scoreCalculator.rice(project)).thenReturn(0.2);
        when(scoreDescriptionProvider.getDescription(anyInt())).thenReturn("very low");

        // When
        markdownFormatter.formatProject(project, 2);

        // Then
        verify(scoreCalculator).ice(project);
        verify(scoreCalculator).rice(project);
    }

    @Test
    void formatProject_ShouldCallScoreDescriptionProviderForAllComponents() {
        // Given
        Project project = Project.builder()
                .name("Test Project")
                .description("Test description")
                .impact(1).confidence(2).ease(3).reach(4).effort(5)
                .build();

        when(scoreCalculator.ice(project)).thenReturn(6.0);
        when(scoreCalculator.rice(project)).thenReturn(1.6);
        when(scoreDescriptionProvider.getDescription(anyInt())).thenReturn("test");

        // When
        markdownFormatter.formatProject(project, 1);

        // Then
        verify(scoreDescriptionProvider).getDescription(1); // Impact
        verify(scoreDescriptionProvider).getDescription(2); // Confidence
        verify(scoreDescriptionProvider).getDescription(3); // Ease
        verify(scoreDescriptionProvider).getDescription(4); // Reach
        verify(scoreDescriptionProvider).getDescription(5); // Effort
    }

    @Test
    void formatProject_WithDifferentRanks_ShouldIncludeCorrectRank() {
        // Given
        Project project = Project.builder()
                .name("Ranked Project")
                .description("Test description")
                .impact(3).confidence(3).ease(3).reach(3).effort(3)
                .build();

        when(scoreCalculator.ice(project)).thenReturn(27.0);
        when(scoreCalculator.rice(project)).thenReturn(9.0);
        when(scoreDescriptionProvider.getDescription(anyInt())).thenReturn("medium");

        // When & Then
        String result1 = markdownFormatter.formatProject(project, 1);
        String result5 = markdownFormatter.formatProject(project, 5);

        assertTrue(result1.contains("## 1. Ranked Project"));
        assertTrue(result5.contains("## 5. Ranked Project"));
    }

    @Test
    void formatNoProjectsMessage_ShouldReturnStandardMessage() {
        // When
        String message = markdownFormatter.formatNoProjectsMessage();

        // Then
        assertEquals("No projects found.\n", message);
    }

    @Test
    void formatProject_ShouldFormatScoresWithTwoDecimalPlaces() {
        // Given
        Project project = Project.builder()
                .name("Decimal Test")
                .description("Test description")
                .impact(3).confidence(4).ease(2).reach(1).effort(2)
                .build();

        when(scoreCalculator.ice(project)).thenReturn(24.123456);
        when(scoreCalculator.rice(project)).thenReturn(6.789123);
        when(scoreDescriptionProvider.getDescription(anyInt())).thenReturn("test");

        // When
        String result = markdownFormatter.formatProject(project, 1);

        // Then
        assertAll(
                () -> assertTrue(result.contains("ICE Score:** 24.12")),
                () -> assertTrue(result.contains("RICE Score:** 6.79")),
                () -> assertFalse(result.contains("24.123456")),
                () -> assertFalse(result.contains("6.789123"))
        );
    }
}