package com.preponderous.parpt.export;

import com.preponderous.parpt.domain.Project;
import com.preponderous.parpt.score.ScoreCalculator;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Component responsible for sorting projects by their calculated scores or by one of
 * their scoring fields. This separates the sorting logic from the formatting logic.
 */
@Component
public class ProjectSorter {

    /**
     * The sort keys accepted by {@link #sortBy(List, String)}, in the order they are
     * presented to the user.
     */
    private static final List<String> SUPPORTED_SORT_KEYS =
            List.of("name", "impact", "confidence", "ease", "reach", "effort", "ice", "rice");

    private final ScoreCalculator scoreCalculator;

    public ProjectSorter(ScoreCalculator scoreCalculator) {
        this.scoreCalculator = scoreCalculator;
    }

    /**
     * Returns the sort keys accepted by {@link #sortBy(List, String)}.
     *
     * @return an unmodifiable list of supported sort keys
     */
    public static List<String> getSupportedSortKeys() {
        return SUPPORTED_SORT_KEYS;
    }

    /**
     * Indicates whether a sort key is accepted by {@link #sortBy(List, String)}.
     * The comparison is case-insensitive.
     *
     * @param sortKey the sort key to check, may be null
     * @return true if the key is supported
     */
    public static boolean isSupportedSortKey(String sortKey) {
        return sortKey != null && SUPPORTED_SORT_KEYS.contains(sortKey.toLowerCase(Locale.ROOT));
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

    /**
     * Sorts a list of projects by the given sort key. Projects are ordered by name
     * alphabetically (case-insensitive, A to Z) and by every other key from highest
     * to lowest. Projects that compare equal keep their original relative order.
     *
     * @param projects the list of projects to sort
     * @param sortKey one of {@link #getSupportedSortKeys()}, case-insensitive
     * @return new list with projects sorted by the given key
     * @throws IllegalArgumentException if the list is null or the sort key is unsupported
     */
    public List<Project> sortBy(List<Project> projects, String sortKey) {
        if (projects == null) {
            throw new IllegalArgumentException("Projects list cannot be null");
        }

        return projects.stream()
                .sorted(comparatorFor(sortKey))
                .toList();
    }

    private Comparator<Project> comparatorFor(String sortKey) {
        if (!isSupportedSortKey(sortKey)) {
            throw new IllegalArgumentException("Unsupported sort key: " + sortKey
                    + ". Supported keys: " + String.join(", ", SUPPORTED_SORT_KEYS));
        }

        return switch (sortKey.toLowerCase(Locale.ROOT)) {
            case "name" -> Comparator.comparing(Project::getName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "impact" -> Comparator.comparingInt((Project p) -> p.getImpact()).reversed();
            case "confidence" -> Comparator.comparingInt((Project p) -> p.getConfidence()).reversed();
            case "ease" -> Comparator.comparingInt((Project p) -> p.getEase()).reversed();
            case "reach" -> Comparator.comparingInt((Project p) -> p.getReach()).reversed();
            case "effort" -> Comparator.comparingInt((Project p) -> p.getEffort()).reversed();
            case "ice" -> Comparator.comparingDouble((Project p) -> scoreCalculator.ice(p)).reversed();
            case "rice" -> Comparator.comparingDouble((Project p) -> scoreCalculator.rice(p)).reversed();
            default -> throw new IllegalArgumentException("Unsupported sort key: " + sortKey);
        };
    }
}