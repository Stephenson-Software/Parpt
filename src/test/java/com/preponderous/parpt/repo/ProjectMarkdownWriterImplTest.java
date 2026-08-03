package com.preponderous.parpt.repo;

import com.preponderous.parpt.domain.Project;
import com.preponderous.parpt.export.MarkdownFormatter;
import com.preponderous.parpt.export.ProjectSorter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectMarkdownWriterImplTest {

    @Mock
    private MarkdownFormatter markdownFormatter;
    
    @Mock
    private ProjectSorter projectSorter;
    
    private ProjectMarkdownWriterImpl markdownWriter;
    private Path tempMarkdownFile;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        tempMarkdownFile = tempDir.resolve("test-projects.md");
        markdownWriter = new ProjectMarkdownWriterImpl(
                tempMarkdownFile.toString(),
                markdownFormatter,
                projectSorter
        );
    }

    @Test
    void writeMarkdown_ShouldCreateMarkdownFile() throws IOException {
        // Given
        Project project = Project.builder()
                .name("Test Project")
                .description("A test project")
                .impact(4).confidence(3).ease(5).reach(2).effort(3)
                .build();
        List<Project> projects = List.of(project);
        List<Project> sortedProjects = List.of(project);

        when(markdownFormatter.formatHeader(false)).thenReturn("# Project Priorities\n\n");
        when(projectSorter.sortByScore(projects, false)).thenReturn(sortedProjects);
        when(markdownFormatter.formatProject(project, 1)).thenReturn("## 1. Test Project\n");

        // When
        markdownWriter.writeMarkdown(projects, false);

        // Then
        assertTrue(Files.exists(tempMarkdownFile));
        String content = Files.readString(tempMarkdownFile);
        assertTrue(content.contains("# Project Priorities"));
        assertTrue(content.contains("## 1. Test Project"));
        
        verify(markdownFormatter).formatHeader(false);
        verify(projectSorter).sortByScore(projects, false);
        verify(markdownFormatter).formatProject(project, 1);
    }

    @Test
    void writeMarkdown_ShouldDelegateToSorterWithCorrectParameters() throws IOException {
        // Given
        Project project1 = Project.builder().name("Project 1").build();
        Project project2 = Project.builder().name("Project 2").build();
        List<Project> projects = Arrays.asList(project1, project2);
        List<Project> sortedProjects = Arrays.asList(project2, project1);

        when(markdownFormatter.formatHeader(true)).thenReturn("# Header\n");
        when(projectSorter.sortByScore(projects, true)).thenReturn(sortedProjects);
        when(markdownFormatter.formatProject(any(), anyInt())).thenReturn("## Project\n");

        // When
        markdownWriter.writeMarkdown(projects, true);

        // Then
        verify(projectSorter).sortByScore(projects, true);
        verify(markdownFormatter).formatHeader(true);
        verify(markdownFormatter).formatProject(project2, 1);
        verify(markdownFormatter).formatProject(project1, 2);
    }

    @Test
    void writeMarkdown_WithRiceSort_ShouldPassCorrectParameterToFormatter() throws IOException {
        // Given
        Project project = Project.builder().name("Test Project").build();
        List<Project> projects = List.of(project);
        List<Project> sortedProjects = List.of(project);

        when(markdownFormatter.formatHeader(true)).thenReturn("# Header RICE\n");
        when(projectSorter.sortByScore(projects, true)).thenReturn(sortedProjects);
        when(markdownFormatter.formatProject(project, 1)).thenReturn("## Project\n");

        // When
        markdownWriter.writeMarkdown(projects, true);

        // Then
        verify(markdownFormatter).formatHeader(true);
        verify(projectSorter).sortByScore(projects, true);
        
        String content = Files.readString(tempMarkdownFile);
        assertTrue(content.contains("# Header RICE"));
    }

    @Test
    void writeMarkdown_WithEmptyList_ShouldWriteNoProjectsMessage() throws IOException {
        // Given
        List<Project> projects = List.of();
        when(markdownFormatter.formatHeader(false)).thenReturn("# Header\n");
        when(markdownFormatter.formatNoProjectsMessage()).thenReturn("No projects found.\n");

        // When
        markdownWriter.writeMarkdown(projects, false);

        // Then
        String content = Files.readString(tempMarkdownFile);
        assertTrue(content.contains("# Header"));
        assertTrue(content.contains("No projects found"));
        
        verify(markdownFormatter).formatHeader(false);
        verify(markdownFormatter).formatNoProjectsMessage();
        verify(projectSorter, never()).sortByScore(any(), anyBoolean());
        verify(markdownFormatter, never()).formatProject(any(), anyInt());
    }

    @Test
    void writeMarkdown_WithNullList_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> markdownWriter.writeMarkdown(null, false));
        
        verifyNoInteractions(markdownFormatter);
        verifyNoInteractions(projectSorter);
    }

    @Test
    void writeMarkdown_ShouldUseCorrectRankingForMultipleProjects() throws IOException {
        // Given
        Project project1 = Project.builder().name("First").build();
        Project project2 = Project.builder().name("Second").build();
        Project project3 = Project.builder().name("Third").build();
        
        List<Project> projects = Arrays.asList(project1, project2, project3);
        List<Project> sortedProjects = Arrays.asList(project3, project1, project2);

        when(markdownFormatter.formatHeader(false)).thenReturn("# Header\n");
        when(projectSorter.sortByScore(projects, false)).thenReturn(sortedProjects);
        when(markdownFormatter.formatProject(any(), anyInt())).thenReturn("## Project\n");

        // When
        markdownWriter.writeMarkdown(projects, false);

        // Then
        verify(markdownFormatter).formatProject(project3, 1);
        verify(markdownFormatter).formatProject(project1, 2);
        verify(markdownFormatter).formatProject(project2, 3);
    }
}