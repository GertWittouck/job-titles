package com.feefo.normalise;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class TokenSimilarityScorerTest {

    private TokenSimilarityScorer testable;

    @BeforeEach
    void setUp() {
        testable = new TokenSimilarityScorer();
    }

    @DisplayName("""
            Given two identical job titles
            When they are scored
            Then the quality is 1.0
            """)
    @Test
    void testIdenticalTitlesScoreOne() {
        // Arrange + Act
        var result = testable.score("Software engineer", "Software engineer");

        // Assert
        assertThat(result).as("quality").isEqualTo(1.0);
    }

    @DisplayName("""
            Given two titles that only differ in case and punctuation
            When they are scored
            Then the quality is 1.0
            """)
    @Test
    void testCaseAndPunctuationAreIgnored() {
        // Arrange + Act
        var result = testable.score("  SOFTWARE-engineer! ", "Software engineer");

        // Assert
        assertThat(result).as("quality").isEqualTo(1.0);
    }

    @DisplayName("""
            Given two titles without any common characters
            When they are scored
            Then the quality is 0.0
            """)
    @Test
    void testCompletelyDifferentTitlesScoreZero() {
        // Arrange + Act
        var result = testable.score("xyz", "Accountant");

        // Assert
        assertThat(result).as("quality").isEqualTo(0.0);
    }

    @DisplayName("""
            Given an input without any words
            When it is scored
            Then the quality is 0.0
            """)
    @Test
    void testInputWithoutWordsScoresZero() {
        // Arrange + Act
        var result = testable.score("#!?", "Accountant");

        // Assert
        assertThat(result).as("quality").isEqualTo(0.0);
    }

    @DisplayName("""
            Given two titles
            When they are scored in either order
            Then the quality is the same
            """)
    @ParameterizedTest
    @CsvSource({
            "Java engineer, Software engineer",
            "Chief Accountant, Accountant",
            "C# engineer, Quantity surveyor"
    })
    void testScoreIsSymmetric(String first, String second) {
        // Arrange + Act
        var forward = testable.score(first, second);
        var backward = testable.score(second, first);

        // Assert
        assertThat(forward)
                .as("symmetric quality").isCloseTo(backward, within(1e-9))
                .as("quality range").isBetween(0.0, 1.0);
    }

    @DisplayName("""
            Given an input sharing a word with the candidate
            When it is scored
            Then it scores higher than an unrelated candidate
            """)
    @Test
    void testSharedWordScoresHigherThanUnrelated() {
        // Arrange + Act
        var related = testable.score("Java engineer", "Software engineer");
        var unrelated = testable.score("Java engineer", "Accountant");

        // Assert
        assertThat(related).as("related quality").isGreaterThan(unrelated);
    }

    @DisplayName("""
            Given a missing input or candidate
            When it is scored
            Then it is rejected
            """)
    @Test
    void testNullArgumentsAreRejected() {
        // Arrange + Act + Assert
        assertThatThrownBy(() -> testable.score(null, "Accountant"))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> testable.score("Accountant", null))
                .isInstanceOf(NullPointerException.class);
    }
}
