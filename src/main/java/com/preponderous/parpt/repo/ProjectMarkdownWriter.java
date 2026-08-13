package com.preponderous.parpt.repo;

import com.preponderous.parpt.domain.Project;
import com.preponderous.parpt.export.ProjectSorter;

import java.util.List;

/**
 * Interface for writing project data to Markdown format.
 * This interface provides methods to export project data
 * in Obsidian-compatible markdown format.
 */
public interface ProjectMarkdownWriter {
    /**
     * Writes a list of projects to Markdown format.
     * Projects are sorted by the given sort key before being written.
     *
     * @param projects the list of projects to be written to Markdown
     * @param sortKey one of {@link ProjectSorter#getSupportedSortKeys()}, case-insensitive
     */
    void writeMarkdown(List<Project> projects, String sortKey);

    /**
     * Writes a list of projects to Markdown format.
     * Projects will be sorted by ICE score by default.
     *
     * @param projects the list of projects to be written to Markdown
     */
    default void writeMarkdown(List<Project> projects) {
        writeMarkdown(projects, ProjectSorter.DEFAULT_SORT_KEY);
    }
}