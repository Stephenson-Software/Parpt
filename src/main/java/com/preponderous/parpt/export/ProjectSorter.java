package com.preponderous.parpt.export;

import com.preponderous.parpt.domain.Project;
import com.preponderous.parpt.score.ScoreCalculator;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Component responsible for sorting projects by their calculated scores.
 * This separates the sorting logic from the formatting logic.
 */
@Component
public class ProjectSorter {

    private final ScoreCalculator scoreCalculator;

    public ProjectSorter(ScoreCalculator scoreCalculator) {
        this.scoreCalculator = scoreCalculator;
    }

    /**
     * Sorts a list of projects by their calculated scores.
     *
     * @param projects the list of projects to sort
     * @param sortByRice true to sort by RICE score, false to sort by ICE score
     * @return new list with projects sorted by score (highest to lowest)
     */
    public List<Project> sortByScore(List<Project> projects, boolean sortByRice) {
        if (projects == null) {
            throw new IllegalArgumentException("Projects list cannot be null");
        }

        return projects.stream()
                .sorted(sortByRice ? 
                    Comparator.comparingDouble((Project p) -> scoreCalculator.rice(p)).reversed() :
                    Comparator.comparingDouble((Project p) -> scoreCalculator.ice(p)).reversed())
                .toList();
    }

    /**
     * Sorts projects by ICE score (highest to lowest).
     *
     * @param projects the list of projects to sort
     * @return new list with projects sorted by ICE score
     */
    public List<Project> sortByIce(List<Project> projects) {
        return sortByScore(projects, false);
    }

    /**
     * Sorts projects by RICE score (highest to lowest).
     *
     * @param projects the list of projects to sort
     * @return new list with projects sorted by RICE score
     */
    public List<Project> sortByRice(List<Project> projects) {
        return sortByScore(projects, true);
    }
}