package com.preponderous.parpt.service;

import com.preponderous.parpt.domain.Project;
import com.preponderous.parpt.repo.ProjectRepository;
import com.preponderous.parpt.trace.UsageReporter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final UsageReporter usageReporter;

    public ProjectService(ProjectRepository projectRepository, UsageReporter usageReporter) {
        this.projectRepository = projectRepository;
        this.usageReporter = usageReporter;
    }

    public Project createProject(String name, String description, int impact, int confidence, int ease, int reach, int effort) throws ProjectRepository.NameTakenException {
        Project project = Project.builder()
                .name(name)
                .description(description)
                .impact(impact)
                .confidence(confidence)
                .ease(ease)
                .reach(reach)
                .effort(effort)
                .build();
        projectRepository.add(project);
        usageReporter.projectCreated();
        return project;
    }

    public List<Project> getProjects() {
        return projectRepository.findAll();
    }

    public Project getProject(String projectName) throws ProjectRepository.ProjectNotFoundException {
        return projectRepository.findByName(projectName);
    }

    public void deleteProject(String projectName) throws ProjectRepository.ProjectNotFoundException {
        projectRepository.remove(projectName);
    }

    public boolean isNameTaken(String projectName) {
        try {
            getProject(projectName);
            return true;
        } catch (ProjectRepository.ProjectNotFoundException e) {
            return false;
        }
    }
}
