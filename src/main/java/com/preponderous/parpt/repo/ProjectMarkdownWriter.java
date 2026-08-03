package com.preponderous.parpt.repo;

import com.preponderous.parpt.domain.Project;

import java.util.List;

/**
 * Interface for writing project data to Markdown format.
 * This interface provides methods to export project data
 * in Obsidian-compatible markdown format.
 */
public interface ProjectMarkdownWriter {
    /**
     * Writes a list of projects to Markdown format.
     * Projects should be sorted by their calculated scores.
     *
     * @param projects the list of projects to be written to Markdown
     * @param sortByRice true to sort by RICE score, false to sort by ICE score
     */
    void writeMarkdown(List<Project> projects, boolean sortByRice);

    /**
     * Writes a list of projects to Markdown format.
     * Projects will be sorted by ICE score by default.
     *
     * @param projects the list of projects to be written to Markdown
     */
    default void writeMarkdown(List<Project> projects) {
        writeMarkdown(projects, false);
    }
}