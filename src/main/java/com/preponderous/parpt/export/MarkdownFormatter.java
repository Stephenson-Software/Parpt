package com.preponderous.parpt.export;

import com.preponderous.parpt.domain.Project;
import com.preponderous.parpt.score.ScoreCalculator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Component responsible for formatting project data into markdown strings.
 * This class handles the actual markdown formatting logic without dealing with file I/O.
 */
@Component
public class MarkdownFormatter {

    private final ScoreCalculator scoreCalculator;
    private final ScoreDescriptionProvider scoreDescriptionProvider;

    public MarkdownFormatter(ScoreCalculator scoreCalculator, ScoreDescriptionProvider scoreDescriptionProvider) {
        this.scoreCalculator = scoreCalculator;
        this.scoreDescriptionProvider = scoreDescriptionProvider;
    }

    /**
     * Generates the markdown header with timestamp and sorting information.
     *
     * @param sortByRice true if sorted by RICE, false if sorted by ICE
     * @return formatted header string
     */
    public String formatHeader(boolean sortByRice) {
        StringBuilder header = new StringBuilder();
        header.append("# Project Priorities\n\n");
        header.append("*Generated on ")
              .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
              .append("*\n\n");
        
        String scoreType = sortByRice ? "RICE" : "ICE";
        header.append("*Sorted by ").append(scoreType).append(" score (highest to lowest)*\n\n");
        
        return header.toString();
    }

    /**
     * Formats a single project into markdown.
     *
     * @param project the project to format
     * @param rank the ranking position (1, 2, 3, etc.)
     * @return formatted project markdown string
     */
    public String formatProject(Project project, int rank) {
        StringBuilder content = new StringBuilder();
        
        double iceScore = scoreCalculator.ice(project);
        double riceScore = scoreCalculator.rice(project);

        // Project header and description
        content.append("## ").append(rank).append(". ").append(project.getName()).append("\n\n");
        content.append("**Description:** ").append(project.getDescription()).append("\n\n");
        
        // Scores summary
        content.append("### Scores\n");
        content.append("- **ICE Score:** ").append(String.format("%.2f", iceScore)).append("\n");
        content.append("- **RICE Score:** ").append(String.format("%.2f", riceScore)).append("\n\n");
        
        // Individual components
        content.append("### Components\n");
        content.append(formatScoreComponent("Impact", project.getImpact()));
        content.append(formatScoreComponent("Confidence", project.getConfidence()));
        content.append(formatScoreComponent("Ease", project.getEase()));
        content.append(formatScoreComponent("Reach", project.getReach()));
        content.append(formatScoreComponent("Effort", project.getEffort()));
        content.append("\n");
        
        content.append("---\n\n");
        
        return content.toString();
    }

    /**
     * Formats a single score component line.
     *
     * @param componentName the name of the component (Impact, Confidence, etc.)
     * @param score the numerical score (1-5)
     * @return formatted component line
     */
    private String formatScoreComponent(String componentName, int score) {
        return "- **" + componentName + ":** " + score + "/5 (" + 
               scoreDescriptionProvider.getDescription(score) + ")\n";
    }

    /**
     * Formats the "no projects found" message.
     *
     * @return formatted no projects message
     */
    public String formatNoProjectsMessage() {
        return "No projects found.\n";
    }
}