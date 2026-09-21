package com.preponderous.parpt.command;

import com.preponderous.parpt.repo.ProjectRepository;
import com.preponderous.parpt.service.ProjectService;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
public class DeleteProjectCommand {

    private final ProjectService projectService;

    public DeleteProjectCommand(ProjectService projectService) {
        this.projectService = projectService;
    }

    @ShellMethod(key = {"delete", "rm"}, value = "Deletes a specific project by name.")
    public String execute(String projectName) {
        try {
            projectService.deleteProject(projectName);
        } catch (ProjectRepository.ProjectNotFoundException e) {
            return "Project not found: " + projectName;
        }

        return "Project deleted: " + projectName;
    }
}
