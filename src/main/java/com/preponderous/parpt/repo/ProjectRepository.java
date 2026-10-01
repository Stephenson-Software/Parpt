package com.preponderous.parpt.repo;

import com.preponderous.parpt.domain.Project;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ProjectRepository {
    private final List<Project> projects;
    private final ProjectJsonReaderWriter projectJsonReaderWriter;

    public ProjectRepository(ProjectJsonReaderWriter projectJsonReaderWriter) {
        this.projectJsonReaderWriter = projectJsonReaderWriter;
        this.projects = projectJsonReaderWriter.readJson();
    }

    public List<Project> findAll() {
        return new ArrayList<>(projects);
    }

    public void clear() {
        projects.clear();
        projectJsonReaderWriter.writeJson(projects);
    }

    public void add(Project project) throws NameTakenException {
        if (projects.stream().anyMatch(p -> p.getName().equals(project.getName()))) {
            throw new NameTakenException("Project with the same name already exists");
        }
        projects.add(project);
        projectJsonReaderWriter.writeJson(projects);
    }

    public void remove(String projectName) throws ProjectNotFoundException {
        Project project = findByName(projectName);
        projects.remove(project);
        projectJsonReaderWriter.writeJson(projects);
    }

    /**
     * Replaces the stored project that has the same name as the given one, keeping its position
     * in creation order.
     */
    public void update(Project project) throws ProjectNotFoundException {
        int index = projects.indexOf(findByName(project.getName()));
        projects.set(index, project);
        projectJsonReaderWriter.writeJson(projects);
    }

    public Project findByName(String projectName) throws ProjectNotFoundException {
        Project retrievedProject = projects.stream()
                .filter(project -> project.getName().equals(projectName))
                .findFirst()
                .orElse(null);
        if (retrievedProject == null) {
            throw new ProjectNotFoundException("Project not found: " + projectName);
        }
        return retrievedProject;
    }

    public static class NameTakenException extends Exception {
        public NameTakenException(String message) {
            super(message);
        }
    }

    public static class ProjectNotFoundException extends Exception {
        public ProjectNotFoundException(String message) {
            super(message);
        }
    }
}