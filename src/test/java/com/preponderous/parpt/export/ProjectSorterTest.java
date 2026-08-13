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
    void sortBy_WithEmptyList_ShouldReturnEmptyList() {
        // Given
        List<Project> emptyList = Collections.emptyList();

        // When
        List<Project> result = projectSorter.sortBy(emptyList, "ice");

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void sortBy_WithIce_ShouldSortByICEScore() {
        // Given
        Project project1 = Project.builder().name("Low ICE").build();
        Project project2 = Project.builder().name("High ICE").build();
        Project project3 = Project.builder().name("Medium ICE").build();
        
        List<Project> projects = Arrays.asList(project1, project2, project3);
        
        when(scoreCalculator.ice(project1)).thenReturn(10.0);
        when(scoreCalculator.ice(project2)).thenReturn(50.0);
        when(scoreCalculator.ice(project3)).thenReturn(30.0);

        // When
        List<Project> result = projectSorter.sortBy(projects, "ice");

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
    void sortBy_WithEqualScores_ShouldMaintainStableOrder() {
        // Given
        Project project1 = Project.builder().name("First").build();
        Project project2 = Project.builder().name("Second").build();
        
        List<Project> projects = Arrays.asList(project1, project2);
        
        when(scoreCalculator.ice(project1)).thenReturn(20.0);
        when(scoreCalculator.ice(project2)).thenReturn(20.0);

        // When
        List<Project> result = projectSorter.sortBy(projects, "ice");

        // Then
        assertEquals(2, result.size());
        // Should maintain original order when scores are equal
        assertEquals("First", result.get(0).getName());
        assertEquals("Second", result.get(1).getName());
    }

    @Test
    void sortByIce_ShouldDelegateToSortByWithIceKey() {
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
    void sortByRice_ShouldDelegateToSortByWithRiceKey() {
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
    void sortBy_WithNullProjects_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> projectSorter.sortBy(null, "name"));
    }

    @Test
    void sortBy_WithUnsupportedKey_ShouldThrowException() {
        // Given
        List<Project> projects = List.of(Project.builder().name("Project 1").build());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> projectSorter.sortBy(projects, "bogus"));
        assertTrue(exception.getMessage().contains("bogus"));
    }

    @Test
    void sortBy_WithName_ShouldSortAlphabeticallyIgnoringCase() {
        // Given
        Project project1 = Project.builder().name("zebra").build();
        Project project2 = Project.builder().name("Apple").build();
        Project project3 = Project.builder().name("mango").build();
        List<Project> projects = Arrays.asList(project1, project2, project3);

        // When
        List<Project> result = projectSorter.sortBy(projects, "name");

        // Then
        assertEquals("Apple", result.get(0).getName());
        assertEquals("mango", result.get(1).getName());
        assertEquals("zebra", result.get(2).getName());
        verify(scoreCalculator, never()).ice(any());
        verify(scoreCalculator, never()).rice(any());
    }

    @Test
    void sortBy_WithImpact_ShouldSortHighestFirst() {
        // Given
        Project project1 = Project.builder().name("Low impact").impact(1).build();
        Project project2 = Project.builder().name("High impact").impact(5).build();
        Project project3 = Project.builder().name("Medium impact").impact(3).build();
        List<Project> projects = Arrays.asList(project1, project2, project3);

        // When
        List<Project> result = projectSorter.sortBy(projects, "impact");

        // Then
        assertEquals("High impact", result.get(0).getName());
        assertEquals("Medium impact", result.get(1).getName());
        assertEquals("Low impact", result.get(2).getName());
    }

    @Test
    void sortBy_WithEffort_ShouldSortHighestFirst() {
        // Given
        Project project1 = Project.builder().name("Low effort").effort(2).build();
        Project project2 = Project.builder().name("High effort").effort(8).build();
        List<Project> projects = Arrays.asList(project1, project2);

        // When
        List<Project> result = projectSorter.sortBy(projects, "effort");

        // Then
        assertEquals("High effort", result.get(0).getName());
        assertEquals("Low effort", result.get(1).getName());
    }

    @Test
    void sortBy_WithRice_ShouldSortByRiceScore() {
        // Given
        Project project1 = Project.builder().name("Low RICE").build();
        Project project2 = Project.builder().name("High RICE").build();
        List<Project> projects = Arrays.asList(project1, project2);
        when(scoreCalculator.rice(project1)).thenReturn(5.0);
        when(scoreCalculator.rice(project2)).thenReturn(25.0);

        // When
        List<Project> result = projectSorter.sortBy(projects, "rice");

        // Then
        assertEquals("High RICE", result.get(0).getName());
        assertEquals("Low RICE", result.get(1).getName());
        verify(scoreCalculator, never()).ice(any());
    }

    @Test
    void sortBy_WithMixedCaseKey_ShouldSortAsIfLowerCase() {
        // Given
        Project project1 = Project.builder().name("Low ICE").build();
        Project project2 = Project.builder().name("High ICE").build();
        List<Project> projects = Arrays.asList(project1, project2);
        when(scoreCalculator.ice(project1)).thenReturn(10.0);
        when(scoreCalculator.ice(project2)).thenReturn(50.0);

        // When
        List<Project> result = projectSorter.sortBy(projects, "IcE");

        // Then
        assertEquals("High ICE", result.get(0).getName());
        assertEquals("Low ICE", result.get(1).getName());
    }

    @Test
    void sortBy_ShouldNotModifyOriginalList() {
        // Given
        Project project1 = Project.builder().name("Zebra").build();
        Project project2 = Project.builder().name("Apple").build();
        List<Project> originalProjects = Arrays.asList(project1, project2);

        // When
        List<Project> sortedProjects = projectSorter.sortBy(originalProjects, "name");

        // Then
        assertEquals("Zebra", originalProjects.get(0).getName());
        assertEquals("Apple", sortedProjects.get(0).getName());
        assertNotSame(originalProjects, sortedProjects);
    }

    @Test
    void isSupportedSortKey_ShouldAcceptEverySupportedKeyIgnoringCase() {
        // Given & When & Then
        for (String key : ProjectSorter.getSupportedSortKeys()) {
            assertTrue(ProjectSorter.isSupportedSortKey(key));
            assertTrue(ProjectSorter.isSupportedSortKey(key.toUpperCase()));
        }
    }

    @Test
    void isSupportedSortKey_WithUnknownOrNullKey_ShouldReturnFalse() {
        // When & Then
        assertFalse(ProjectSorter.isSupportedSortKey("bogus"));
        assertFalse(ProjectSorter.isSupportedSortKey(null));
        assertFalse(ProjectSorter.isSupportedSortKey("none"));
    }

    @Test
    void getSupportedSortKeys_ShouldListEveryScoringFieldAndScore() {
        // When
        List<String> keys = ProjectSorter.getSupportedSortKeys();

        // Then
        assertEquals(List.of("name", "impact", "confidence", "ease", "reach", "effort", "ice", "rice"), keys);
    }

    @Test
    void sortBy_WithSingleProject_ShouldReturnSingleProjectList() {
        // Given
        Project project = Project.builder().name("Only Project").build();
        List<Project> projects = List.of(project);

        // When
        List<Project> result = projectSorter.sortBy(projects, "ice");

        // Then
        assertEquals(1, result.size());
        assertEquals("Only Project", result.get(0).getName());
    }

    @Test
    void describeSortKey_ShouldDescribeEverySupportedKey() {
        // When & Then
        assertEquals("ICE score", ProjectSorter.describeSortKey("ice"));
        assertEquals("RICE score", ProjectSorter.describeSortKey("rice"));
        assertEquals("name", ProjectSorter.describeSortKey("name"));
        assertEquals("impact", ProjectSorter.describeSortKey("impact"));
        assertEquals("effort", ProjectSorter.describeSortKey("effort"));
    }

    @Test
    void describeSortKey_WithMixedCaseKey_ShouldDescribeAsIfLowerCase() {
        // When & Then
        assertEquals("ICE score", ProjectSorter.describeSortKey("IcE"));
        assertEquals("name", ProjectSorter.describeSortKey("NAME"));
    }

    @Test
    void describeSortKey_WithUnsupportedKey_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> ProjectSorter.describeSortKey("bogus"));
        assertThrows(IllegalArgumentException.class, () -> ProjectSorter.describeSortKey(null));
    }

    @Test
    void describeSortDirection_ShouldDescribeNameAlphabeticallyAndEveryOtherKeyDescending() {
        // When & Then
        assertEquals("A to Z", ProjectSorter.describeSortDirection("name"));
        for (String key : ProjectSorter.getSupportedSortKeys()) {
            if (!"name".equals(key)) {
                assertEquals("highest to lowest", ProjectSorter.describeSortDirection(key));
            }
        }
    }

    @Test
    void describeSortDirection_WithUnsupportedKey_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> ProjectSorter.describeSortDirection("bogus"));
    }

    @Test
    void defaultSortKey_ShouldBeASupportedKey() {
        // When & Then
        assertEquals("ice", ProjectSorter.DEFAULT_SORT_KEY);
        assertTrue(ProjectSorter.isSupportedSortKey(ProjectSorter.DEFAULT_SORT_KEY));
    }
}