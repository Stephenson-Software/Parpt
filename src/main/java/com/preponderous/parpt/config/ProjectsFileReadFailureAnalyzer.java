package com.preponderous.parpt.config;

import com.preponderous.parpt.repo.ProjectsFileReadException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Turns a projects file that cannot be read at startup into a short report naming the file and
 * the reason, in place of the bean-creation stack trace. Registered in
 * {@code META-INF/spring.factories}.
 */
public class ProjectsFileReadFailureAnalyzer extends AbstractFailureAnalyzer<ProjectsFileReadException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, ProjectsFileReadException cause) {
        String action = "Repair " + cause.getProjectsFilePath() + " or move it aside, then start Parpt again. "
                + "Parpt has not changed the file, so the projects in it can still be recovered by hand. "
                + "To start with a different file, pass -Dapp.projects.file=<path>.";
        return new FailureAnalysis(cause.getMessage(), action, cause);
    }
}
