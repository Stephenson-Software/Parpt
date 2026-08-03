package com.preponderous.parpt.export;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class ScoreDescriptionProviderTest {

    private ScoreDescriptionProvider scoreDescriptionProvider;

    @BeforeEach
    void setUp() {
        scoreDescriptionProvider = new ScoreDescriptionProvider();
    }

    @ParameterizedTest
    @CsvSource({
            "1, very low",
            "2, low", 
            "3, medium",
            "4, high",
            "5, very high"
    })
    void getDescription_WithValidScores_ShouldReturnCorrectDescription(int score, String expectedDescription) {
        // When
        String result = scoreDescriptionProvider.getDescription(score);

        // Then
        assertEquals(expectedDescription, result);
    }

    @Test
    void getDescription_WithInvalidScore_ShouldReturnStringValue() {
        // When & Then
        assertEquals("0", scoreDescriptionProvider.getDescription(0));
        assertEquals("6", scoreDescriptionProvider.getDescription(6));
        assertEquals("-1", scoreDescriptionProvider.getDescription(-1));
        assertEquals("10", scoreDescriptionProvider.getDescription(10));
    }

    @Test
    void getDescription_ShouldBeDeterministicForSameInput() {
        // When
        String result1 = scoreDescriptionProvider.getDescription(3);
        String result2 = scoreDescriptionProvider.getDescription(3);

        // Then
        assertEquals(result1, result2);
        assertEquals("medium", result1);
    }

    @Test
    void getDescription_ShouldHandleEdgeCases() {
        // When & Then
        assertEquals("very low", scoreDescriptionProvider.getDescription(1));
        assertEquals("very high", scoreDescriptionProvider.getDescription(5));
    }
}