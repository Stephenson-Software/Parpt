package com.preponderous.parpt.export;

import com.preponderous.parpt.domain.Project;
import com.preponderous.parpt.score.ScoreCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectSorterTest {

    @Mock
    private ScoreCalculator scoreCalculator;
    
    private ProjectSorter projectSorter;

    @BeforeEach
    void setUp() {
        projectSorter = new ProjectSorter(scoreCalculator);
    }

    @Test
    void sortByScore_WithNullProjects_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> projectSorter.sortByScore(null, false));
    }

    @Test
    void sortByScore_WithEmptyList_ShouldReturnEmptyList() {
        // Given
        List<Project> emptyList = Collections.emptyList();

        // When
        List<Project> result = projectSorter.sortByScore(emptyList, false);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void sortByScore_WithICESorting_ShouldSortByICEScore() {
        // Given
        Project project1 = Project.builder().name("Low ICE").build();
        Project project2 = Project.builder().name("High ICE").build();
        Project project3 = Project.builder().name("Medium ICE").build();
        
        List<Project> projects = Arrays.asList(project1, project2, project3);
        
        when(scoreCalculator.ice(project1)).thenReturn(10.0);
        when(scoreCalculator.ice(project2)).thenReturn(50.0);
        when(scoreCalculator.ice(project3)).thenReturn(30.0);

        // When
        List<Project> result = projectSorter.sortByScore(projects, false);

        // Then
        assertEquals(3, result.size());
        assertEquals("High ICE", result.get(0).getName());
        assertEquals("Medium ICE", result.get(1).getName());
        assertEquals("Low ICE", result.get(2).getName());
        
        verify(scoreCalculator, atLeastOnce()).ice(project1);
        verify(scoreCalculator, atLeastOnce()).ice(project2);
        verify(scoreCalculator, atLeastOnce()).ice(project3);
        verify(scoreCalculator, never()).rice(any());
    }

    @Test
    void sortByScore_WithRICESorting_ShouldSortByRICEScore() {
        // Given
        Project project1 = Project.builder().name("Low RICE").build();
        Project project2 = Project.builder().name("High RICE").build();
        Project project3 = Project.builder().name("Medium RICE").build();
        
        List<Project> projects = Arrays.asList(project1, project2, project3);
        
        when(scoreCalculator.rice(project1)).thenReturn(5.0);
        when(scoreCalculator.rice(project2)).thenReturn(25.0);
        when(scoreCalculator.rice(project3)).thenReturn(15.0);

        // When
        List<Project> result = projectSorter.sortByScore(projects, true);

        // Then
        assertEquals(3, result.size());
        assertEquals("High RICE", result.get(0).getName());
        assertEquals("Medium RICE", result.get(1).getName());
        assertEquals("Low RICE", result.get(2).getName());
        
        verify(scoreCalculator, atLeastOnce()).rice(project1);
        verify(scoreCalculator, atLeastOnce()).rice(project2);
        verify(scoreCalculator, atLeastOnce()).rice(project3);
        verify(scoreCalculator, never()).ice(any());
    }

    @Test
    void sortByScore_WithEqualScores_ShouldMaintainStableOrder() {
        // Given
        Project project1 = Project.builder().name("First").build();
        Project project2 = Project.builder().name("Second").build();
        
        List<Project> projects = Arrays.asList(project1, project2);
        
        when(scoreCalculator.ice(project1)).thenReturn(20.0);
        when(scoreCalculator.ice(project2)).thenReturn(20.0);

        // When
        List<Project> result = projectSorter.sortByScore(projects, false);

        // Then
        assertEquals(2, result.size());
        // Should maintain original order when scores are equal
        assertEquals("First", result.get(0).getName());
        assertEquals("Second", result.get(1).getName());
    }

    @Test
    void sortByScore_ShouldNotModifyOriginalList() {
        // Given
        Project project1 = Project.builder().name("Project 1").build();
        Project project2 = Project.builder().name("Project 2").build();
        
        List<Project> originalProjects = Arrays.asList(project1, project2);
        
        when(scoreCalculator.ice(project1)).thenReturn(10.0);
        when(scoreCalculator.ice(project2)).thenReturn(20.0);

        // When
        List<Project> sortedProjects = projectSorter.sortByScore(originalProjects, false);

        // Then
        assertEquals("Project 1", originalProjects.get(0).getName());
        assertEquals("Project 2", originalProjects.get(1).getName());
        assertEquals("Project 2", sortedProjects.get(0).getName());
        assertEquals("Project 1", sortedProjects.get(1).getName());
        assertNotSame(originalProjects, sortedProjects);
    }

    @Test
    void sortByIce_ShouldDelegateToSortByScoreWithFalse() {
        // Given
        Project project1 = Project.builder().name("Project 1").build();
        Project project2 = Project.builder().name("Project 2").build();
        List<Project> projects = List.of(project1, project2);
        when(scoreCalculator.ice(project1)).thenReturn(25.0);
        when(scoreCalculator.ice(project2)).thenReturn(15.0);

        // When
        List<Project> result = projectSorter.sortByIce(projects);

        // Then
        assertEquals(2, result.size());
        assertEquals("Project 1", result.get(0).getName());
        assertEquals("Project 2", result.get(1).getName());
        verify(scoreCalculator, atLeastOnce()).ice(project1);
        verify(scoreCalculator, atLeastOnce()).ice(project2);
        verify(scoreCalculator, never()).rice(any());
    }

    @Test
    void sortByRice_ShouldDelegateToSortByScoreWithTrue() {
        // Given
        Project project1 = Project.builder().name("Project 1").build();
        Project project2 = Project.builder().name("Project 2").build();
        List<Project> projects = List.of(project1, project2);
        when(scoreCalculator.rice(project1)).thenReturn(12.5);
        when(scoreCalculator.rice(project2)).thenReturn(8.5);

        // When
        List<Project> result = projectSorter.sortByRice(projects);

        // Then
        assertEquals(2, result.size());
        assertEquals("Project 1", result.get(0).getName());
        assertEquals("Project 2", result.get(1).getName());
        verify(scoreCalculator, atLeastOnce()).rice(project1);
        verify(scoreCalculator, atLeastOnce()).rice(project2);
        verify(scoreCalculator, never()).ice(any());
    }

    @Test
    void sortByScore_WithSingleProject_ShouldReturnSingleProjectList() {
        // Given
        Project project = Project.builder().name("Only Project").build();
        List<Project> projects = List.of(project);

        // When
        List<Project> result = projectSorter.sortByScore(projects, false);

        // Then
        assertEquals(1, result.size());
        assertEquals("Only Project", result.get(0).getName());
    }
}