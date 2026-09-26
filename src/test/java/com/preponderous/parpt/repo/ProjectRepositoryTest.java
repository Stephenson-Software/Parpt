package com.preponderous.parpt.repo;

import com.preponderous.parpt.domain.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectRepositoryTest {

    @Mock
    private ProjectJsonReaderWriter projectJsonReaderWriter;

    private ProjectRepository repository;

    // writeJson receives the repository's own mutable list, so each call's contents are snapshotted here
    private final List<List<String>> writtenNames = new ArrayList<>();

    private Project alpha;
    private Project beta;

    @BeforeEach
    void setUp() {
        alpha = Project.builder().name("Alpha").description("First").impact(1).confidence(2).ease(3).reach(4).effort(5).build();
        beta = Project.builder().name("Beta").description("Second").impact(5).confidence(4).ease(3).reach(2).effort(1).build();

        when(projectJsonReaderWriter.readJson()).thenReturn(new ArrayList<>(List.of(alpha)));
        lenient().doAnswer(invocation -> {
            List<Project> written = invocation.getArgument(0);
            writtenNames.add(written.stream().map(Project::getName).toList());
            return null;
        }).when(projectJsonReaderWriter).writeJson(anyList());

        repository = new ProjectRepository(projectJsonReaderWriter);
    }

    @Test
    void constructor_ShouldSeedProjectsFromReadJson() {
        assertThat(repository.findAll()).containsExactly(alpha);
        verify(projectJsonReaderWriter, times(1)).readJson();
        verify(projectJsonReaderWriter, never()).writeJson(anyList());
    }

    @Test
    void findAll_ShouldReturnDefensiveCopy() {
        List<Project> returned = repository.findAll();
        returned.clear();
        returned.add(beta);

        assertThat(repository.findAll()).containsExactly(alpha);
    }

    @Test
    void add_ShouldAppendProjectAndPersistOnce() throws Exception {
        repository.add(beta);

        assertThat(repository.findAll()).containsExactly(alpha, beta);
        verify(projectJsonReaderWriter, times(1)).writeJson(anyList());
        assertThat(writtenNames).containsExactly(List.of("Alpha", "Beta"));
    }

    @Test
    void add_WithDuplicateName_ShouldThrowAndLeaveStateUnchanged() {
        Project duplicate = Project.builder().name("Alpha").description("Other").build();

        assertThrows(ProjectRepository.NameTakenException.class, () -> repository.add(duplicate));

        assertThat(repository.findAll()).containsExactly(alpha);
        verify(projectJsonReaderWriter, never()).writeJson(anyList());
    }

    @Test
    void remove_ShouldDeleteProjectAndPersistOnce() throws Exception {
        repository.add(beta);
        writtenNames.clear();
        clearInvocations(projectJsonReaderWriter);

        repository.remove("Alpha");

        assertThat(repository.findAll()).containsExactly(beta);
        verify(projectJsonReaderWriter, times(1)).writeJson(anyList());
        assertThat(writtenNames).containsExactly(List.of("Beta"));
    }

    @Test
    void remove_WithUnknownName_ShouldThrowAndNotPersist() {
        ProjectRepository.ProjectNotFoundException exception = assertThrows(
                ProjectRepository.ProjectNotFoundException.class, () -> repository.remove("Missing"));

        assertThat(exception.getMessage()).isEqualTo("Project not found: Missing");
        assertThat(repository.findAll()).containsExactly(alpha);
        verify(projectJsonReaderWriter, never()).writeJson(anyList());
    }

    @Test
    void clear_ShouldEmptyProjectsAndPersistOnce() {
        repository.clear();

        assertThat(repository.findAll()).isEmpty();
        verify(projectJsonReaderWriter, times(1)).writeJson(anyList());
        assertThat(writtenNames).containsExactly(List.of());
    }

    @Test
    void findByName_ShouldReturnMatchingProject() throws Exception {
        assertThat(repository.findByName("Alpha")).isSameAs(alpha);
    }

    @Test
    void findByName_WithUnknownName_ShouldThrow() {
        assertThrows(ProjectRepository.ProjectNotFoundException.class, () -> repository.findByName("Missing"));
    }
}
