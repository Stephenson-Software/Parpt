package com.preponderous.parpt.command;

import com.preponderous.parpt.domain.Project;
import com.preponderous.parpt.repo.ProjectMarkdownWriter;
import com.preponderous.parpt.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportProjectsCommandTest {

    @Mock
    private ProjectService projectService;
    
    @Mock
    private ProjectMarkdownWriter markdownWriter;
    
    private ExportProjectsCommand exportCommand;

    @BeforeEach
    void setUp() {
        exportCommand = new ExportProjectsCommand(projectService, markdownWriter);
    }

    @Test
    void execute_WithProjectsAndDefaultSort_ShouldExportWithICE() {
        // Given
        List<Project> projects = Arrays.asList(
                Project.builder().name("Project 1").description("Desc 1").build(),
                Project.builder().name("Project 2").description("Desc 2").build()
        );
        when(projectService.getProjects()).thenReturn(projects);

        // When
        String result = exportCommand.execute("ice");

        // Then
        verify(markdownWriter).writeMarkdown(projects, "ice");
        assertTrue(result.contains("Successfully exported 2 projects"));
        assertTrue(result.contains("sorted by ICE score."));
    }

    @Test
    void execute_WithRiceSort_ShouldExportWithRICE() {
        // Given
        List<Project> projects = List.of(
                Project.builder().name("Project 1").description("Desc 1").build()
        );
        when(projectService.getProjects()).thenReturn(projects);

        // When
        String result = exportCommand.execute("rice");

        // Then
        verify(markdownWriter).writeMarkdown(projects, "rice");
        assertTrue(result.contains("Successfully exported 1 projects"));
        assertTrue(result.contains("sorted by RICE score."));
    }

    @Test
    void execute_WithNonScoreSortKey_ShouldExportWithThatKey() {
        // Given
        List<Project> projects = List.of(
                Project.builder().name("Project 1").description("Desc 1").build()
        );
        when(projectService.getProjects()).thenReturn(projects);

        // When
        String result = exportCommand.execute("name");

        // Then
        verify(markdownWriter).writeMarkdown(projects, "name");
        assertTrue(result.contains("Successfully exported 1 projects"));
        assertTrue(result.contains("sorted by name."));
    }

    @Test
    void execute_WithNoProjects_ShouldReturnNoProjectsMessage() {
        // Given
        when(projectService.getProjects()).thenReturn(Collections.emptyList());

        // When
        String result = exportCommand.execute("ice");

        // Then
        verify(markdownWriter, never()).writeMarkdown(any(), anyString());
        assertEquals("No projects found to export.", result);
    }

    @Test
    void execute_WithInvalidSortOption_ShouldReturnErrorMessage() {
        // Given
        List<Project> projects = List.of(
                Project.builder().name("Project 1").description("Desc 1").build()
        );
        when(projectService.getProjects()).thenReturn(projects);

        // When
        String result = exportCommand.execute("invalid");

        // Then
        verify(markdownWriter, never()).writeMarkdown(any(), anyString());
        assertEquals("Invalid sort option. Use one of: name, impact, confidence, ease, reach, effort, ice, rice.", result);
    }

    @Test
    void execute_WhenMarkdownWriterThrowsException_ShouldReturnErrorMessage() {
        // Given
        List<Project> projects = List.of(
                Project.builder().name("Project 1").description("Desc 1").build()
        );
        when(projectService.getProjects()).thenReturn(projects);
        doThrow(new RuntimeException("Write failed")).when(markdownWriter).writeMarkdown(any(), anyString());

        // When
        String result = exportCommand.execute("ice");

        // Then
        assertTrue(result.contains("Failed to export projects: Write failed"));
    }

    @Test
    void execute_WithCaseInsensitiveSortOptions_ShouldWork() {
        // Given
        List<Project> projects = List.of(
                Project.builder().name("Project 1").description("Desc 1").build()
        );
        when(projectService.getProjects()).thenReturn(projects);

        // When & Then
        // The key is passed through as given; ProjectSorter and MarkdownFormatter normalize it
        String upperCaseIceResult = exportCommand.execute("ICE");
        verify(markdownWriter).writeMarkdown(projects, "ICE");
        assertTrue(upperCaseIceResult.contains("sorted by ICE score."));

        String mixedCaseRiceResult = exportCommand.execute("Rice");
        verify(markdownWriter).writeMarkdown(projects, "Rice");
        assertTrue(mixedCaseRiceResult.contains("sorted by RICE score."));

        String mixedCaseNameResult = exportCommand.execute("NaMe");
        verify(markdownWriter).writeMarkdown(projects, "NaMe");
        assertTrue(mixedCaseNameResult.contains("sorted by name."));
    }
}