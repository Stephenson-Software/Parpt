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

    /**
     * Updates the named project, changing only the fields given; a null argument keeps the
     * project's current value.
     */
    public Project updateProject(String name, String description, Integer impact, Integer confidence, Integer ease, Integer reach, Integer effort) throws ProjectRepository.ProjectNotFoundException {
        Project existing = projectRepository.findByName(name);
        Project updated = Project.builder()
                .name(existing.getName())
                .description(description != null ? description : existing.getDescription())
                .impact(impact != null ? impact : existing.getImpact())
                .confidence(confidence != null ? confidence : existing.getConfidence())
                .ease(ease != null ? ease : existing.getEase())
                .reach(reach != null ? reach : existing.getReach())
                .effort(effort != null ? effort : existing.getEffort())
                .build();
        projectRepository.update(updated);
        return updated;
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
