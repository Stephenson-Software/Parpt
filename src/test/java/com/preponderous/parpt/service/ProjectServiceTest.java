package com.preponderous.parpt.service;

import com.preponderous.parpt.domain.Project;
import com.preponderous.parpt.repo.ProjectRepository;
import com.preponderous.parpt.trace.UsageReporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UsageReporter usageReporter;

    private ProjectService service;

    @BeforeEach
    void setUp() {
        service = new ProjectService(projectRepository, usageReporter);
    }

    @Test
    void createProject_ShouldBuildProjectFromArgumentsAddItAndReturnIt() throws Exception {
        Project created = service.createProject("Alpha", "First", 1, 2, 3, 4, 5);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository, times(1)).add(captor.capture());
        Project added = captor.getValue();
        assertThat(created).isSameAs(added);
        // Distinct score values catch any argument transposed between the signature and the builder
        assertThat(added.getName()).isEqualTo("Alpha");
        assertThat(added.getDescription()).isEqualTo("First");
        assertThat(added.getImpact()).isEqualTo(1);
        assertThat(added.getConfidence()).isEqualTo(2);
        assertThat(added.getEase()).isEqualTo(3);
        assertThat(added.getReach()).isEqualTo(4);
        assertThat(added.getEffort()).isEqualTo(5);
    }

    @Test
    void createProject_ShouldReportUsageOnlyAfterProjectIsAdded() throws Exception {
        service.createProject("Alpha", "First", 1, 2, 3, 4, 5);

        InOrder inOrder = inOrder(projectRepository, usageReporter);
        inOrder.verify(projectRepository).add(any(Project.class));
        inOrder.verify(usageReporter).projectCreated();
        verifyNoMoreInteractions(usageReporter);
    }

    @Test
    void createProject_WithTakenName_ShouldPropagateExceptionAndNotReportUsage() throws Exception {
        doThrow(new ProjectRepository.NameTakenException("Project with the same name already exists"))
                .when(projectRepository).add(any(Project.class));

        assertThrows(ProjectRepository.NameTakenException.class,
                () -> service.createProject("Alpha", "First", 1, 2, 3, 4, 5));

        verifyNoInteractions(usageReporter);
    }

    @Test
    void getProjects_ShouldReturnRepositoryFindAll() {
        Project alpha = Project.builder().name("Alpha").build();
        Project beta = Project.builder().name("Beta").build();
        when(projectRepository.findAll()).thenReturn(List.of(alpha, beta));

        assertThat(service.getProjects()).containsExactly(alpha, beta);
    }

    @Test
    void getProject_ShouldReturnRepositoryFindByName() throws Exception {
        Project alpha = Project.builder().name("Alpha").build();
        when(projectRepository.findByName("Alpha")).thenReturn(alpha);

        assertThat(service.getProject("Alpha")).isSameAs(alpha);
    }

    @Test
    void getProject_WithUnknownName_ShouldPropagateNotFound() throws Exception {
        when(projectRepository.findByName("Missing"))
                .thenThrow(new ProjectRepository.ProjectNotFoundException("Project not found: Missing"));

        ProjectRepository.ProjectNotFoundException exception = assertThrows(
                ProjectRepository.ProjectNotFoundException.class, () -> service.getProject("Missing"));

        assertThat(exception.getMessage()).isEqualTo("Project not found: Missing");
    }

    @Test
    void updateProject_ShouldChangeOnlyGivenFieldsAndPersistViaRepository() throws Exception {
        Project alpha = Project.builder().name("Alpha").description("First").impact(1).confidence(2).ease(3).reach(4).effort(5).build();
        when(projectRepository.findByName("Alpha")).thenReturn(alpha);

        Project updated = service.updateProject("Alpha", "Changed", null, 4, null, 1, null);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository, times(1)).update(captor.capture());
        assertThat(updated).isSameAs(captor.getValue());
        assertThat(updated.getName()).isEqualTo("Alpha");
        assertThat(updated.getDescription()).isEqualTo("Changed");
        assertThat(updated.getImpact()).isEqualTo(1);
        assertThat(updated.getConfidence()).isEqualTo(4);
        assertThat(updated.getEase()).isEqualTo(3);
        assertThat(updated.getReach()).isEqualTo(1);
        assertThat(updated.getEffort()).isEqualTo(5);
        verifyNoInteractions(usageReporter);
    }

    @Test
    void updateProject_WithAllFieldsGiven_ShouldApplyEachToItsOwnField() throws Exception {
        when(projectRepository.findByName("Alpha")).thenReturn(Project.builder().name("Alpha").description("First").build());

        Project updated = service.updateProject("Alpha", "Changed", 5, 4, 3, 2, 1);

        // Distinct score values catch any argument transposed between the signature and the builder
        assertThat(updated.getImpact()).isEqualTo(5);
        assertThat(updated.getConfidence()).isEqualTo(4);
        assertThat(updated.getEase()).isEqualTo(3);
        assertThat(updated.getReach()).isEqualTo(2);
        assertThat(updated.getEffort()).isEqualTo(1);
    }

    @Test
    void updateProject_WithUnknownName_ShouldPropagateNotFoundAndNotUpdate() throws Exception {
        when(projectRepository.findByName("Missing"))
                .thenThrow(new ProjectRepository.ProjectNotFoundException("Project not found: Missing"));

        assertThrows(ProjectRepository.ProjectNotFoundException.class,
                () -> service.updateProject("Missing", "Changed", 1, 1, 1, 1, 1));

        verify(projectRepository, never()).update(any(Project.class));
    }

    @Test
    void deleteProject_ShouldRemoveByNameAndNotReportUsage() throws Exception {
        service.deleteProject("Alpha");

        verify(projectRepository, times(1)).remove("Alpha");
        verifyNoInteractions(usageReporter);
    }

    @Test
    void deleteProject_WithUnknownName_ShouldPropagateNotFound() throws Exception {
        doThrow(new ProjectRepository.ProjectNotFoundException("Project not found: Missing"))
                .when(projectRepository).remove("Missing");

        assertThrows(ProjectRepository.ProjectNotFoundException.class, () -> service.deleteProject("Missing"));
    }

    @Test
    void isNameTaken_WithExistingProject_ShouldReturnTrue() throws Exception {
        when(projectRepository.findByName("Alpha")).thenReturn(Project.builder().name("Alpha").build());

        assertThat(service.isNameTaken("Alpha")).isTrue();
    }

    @Test
    void isNameTaken_WithUnknownProject_ShouldReturnFalse() throws Exception {
        when(projectRepository.findByName("Missing"))
                .thenThrow(new ProjectRepository.ProjectNotFoundException("Project not found: Missing"));

        assertThat(service.isNameTaken("Missing")).isFalse();
    }
}
