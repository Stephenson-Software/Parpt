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
        verify(markdownWriter).writeMarkdown(projects, false);
        assertTrue(result.contains("Successfully exported 2 projects"));
        assertTrue(result.contains("ICE score"));
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
        verify(markdownWriter).writeMarkdown(projects, true);
        assertTrue(result.contains("Successfully exported 1 projects"));
        assertTrue(result.contains("RICE score"));
    }

    @Test
    void execute_WithNoProjects_ShouldReturnNoProjectsMessage() {
        // Given
        when(projectService.getProjects()).thenReturn(Collections.emptyList());

        // When
        String result = exportCommand.execute("ice");

        // Then
        verify(markdownWriter, never()).writeMarkdown(any(), anyBoolean());
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
        verify(markdownWriter, never()).writeMarkdown(any(), anyBoolean());
        assertEquals("Invalid sort option. Use 'ice' or 'rice'.", result);
    }

    @Test
    void execute_WhenMarkdownWriterThrowsException_ShouldReturnErrorMessage() {
        // Given
        List<Project> projects = List.of(
                Project.builder().name("Project 1").description("Desc 1").build()
        );
        when(projectService.getProjects()).thenReturn(projects);
        doThrow(new RuntimeException("Write failed")).when(markdownWriter).writeMarkdown(any(), anyBoolean());

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
        exportCommand.execute("ICE");
        verify(markdownWriter).writeMarkdown(projects, false);

        exportCommand.execute("Rice");
        verify(markdownWriter).writeMarkdown(projects, true);

        exportCommand.execute("RICE");
        verify(markdownWriter, times(2)).writeMarkdown(projects, true);
    }
}