package com.preponderous.parpt.export;

import org.springframework.stereotype.Component;

/**
 * Component responsible for providing descriptive text for numerical scores.
 * This centralizes the score-to-description mapping logic.
 */
@Component
public class ScoreDescriptionProvider {

    /**
     * Converts a numerical score (1-5) to a descriptive text.
     *
     * @param score the numerical score
     * @return descriptive text for the score
     */
    public String getDescription(int score) {
        return switch (score) {
            case 1 -> "very low";
            case 2 -> "low";
            case 3 -> "medium";
            case 4 -> "high";
            case 5 -> "very high";
            default -> String.valueOf(score);
        };
    }
}