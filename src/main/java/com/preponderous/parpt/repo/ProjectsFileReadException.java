package com.preponderous.parpt.repo;

/**
 * Thrown when the projects file exists but cannot be read, so startup can report the file and
 * the reason without a stack trace.
 */
public class ProjectsFileReadException extends RuntimeException {
    private final String projectsFilePath;

    public ProjectsFileReadException(String projectsFilePath, Throwable cause) {
        super("Failed to read projects from " + projectsFilePath + ": " + cause.getMessage(), cause);
        this.projectsFilePath = projectsFilePath;
    }

    public String getProjectsFilePath() {
        return projectsFilePath;
    }
}
