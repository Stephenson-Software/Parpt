package com.preponderous.parpt.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputValidatorTest {

    @Test
    void testScoresWithinRangeAreValid() {
        for (int score = 1; score <= 5; score++) {
            assertTrue(InputValidator.isValidScore(score), "Score " + score + " should be valid");
        }
    }

    @Test
    void testScoresOutsideRangeAreInvalid() {
        assertFalse(InputValidator.isValidScore(0));
        assertFalse(InputValidator.isValidScore(6));
        assertFalse(InputValidator.isValidScore(-1));
        assertFalse(InputValidator.isValidScore(Integer.MAX_VALUE));
        assertFalse(InputValidator.isValidScore(Integer.MIN_VALUE));
    }

    @Test
    void testAllScoresWithinRangeAreValid() {
        assertTrue(InputValidator.areValidScores(1, 2, 3, 4, 5));
    }

    @Test
    void testOneScoreOutsideRangeMakesAllInvalid() {
        assertFalse(InputValidator.areValidScores(1, 2, 6, 4, 5));
        assertFalse(InputValidator.areValidScores(0, 2, 3, 4, 5));
    }

    @Test
    void testNullScoresAreIgnored() {
        assertTrue(InputValidator.areValidScores(null, 3, null));
        assertTrue(InputValidator.areValidScores((Integer) null));
        assertFalse(InputValidator.areValidScores(null, 6, null));
    }
}
