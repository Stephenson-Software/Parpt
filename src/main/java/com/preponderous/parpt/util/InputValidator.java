package com.preponderous.parpt.util;

/**
 * Validates user-supplied input, so every command applies the same rules.
 */
public final class InputValidator {
    public static final int MIN_SCORE = 1;
    public static final int MAX_SCORE = 5;

    private InputValidator() {
    }

    /**
     * Checks whether a score lies within the allowed range.
     *
     * @param score The score to check
     * @return True if the score is between {@link #MIN_SCORE} and {@link #MAX_SCORE}, inclusive
     */
    public static boolean isValidScore(int score) {
        return score >= MIN_SCORE && score <= MAX_SCORE;
    }

    /**
     * Checks whether every given score lies within the allowed range. A null score stands for a
     * value that was not supplied and is ignored.
     *
     * @param scores The scores to check
     * @return True if no non-null score is outside the allowed range
     */
    public static boolean areValidScores(Integer... scores) {
        for (Integer score : scores) {
            if (score != null && !isValidScore(score)) {
                return false;
            }
        }
        return true;
    }
}
