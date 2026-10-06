package com.feefo.normalise;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NormaliserTest {

    private Normaliser testable;

    @BeforeEach
    void setUp() {
        testable = new Normaliser(NormaliserConfig.load(), new TokenSimilarityScorer());
    }

    @DisplayName("""
            Given an input for a job title
            When the job title contains engineer
            Then normalize to Software engineer
            """)
    @ParameterizedTest
    @ValueSource(strings = {"Java engineer", "C++ engineer", "Frontend engineer"})
    void testNormalizeEngineerToSoftwareEngineer(String testValue) {
        // Arrange + Act
        var result = testable.normalise(testValue);

        // Assert
        assertThat(result).as("valid job title result").isNotNull();
        assertThat(result.normalised()).as("valid normalised job title").isEqualTo("Software engineer");
    }

    @DisplayName("""
            Given an input for a job title
            When the job title contains account
            Then normalize to Accountant
            """)
    @ParameterizedTest
    @ValueSource(strings = {"Account, account", "Chief Account", "chief account"})
    void testNormalizeAccountToAccountant(String testValue) {
        // Arrange + Act
        var result = testable.normalise(testValue);

        // Assert
        assertThat(result).as("valid job title result").isNotNull();
        assertThat(result.normalised()).as("valid normalised job title").isEqualTo("Accountant");
    }

    @DisplayName("""
            Given the examples from the assignment
            When the job title is matched
            Then the expected normalised title is returned
            """)
    @ParameterizedTest
    @CsvSource({
            "Java engineer, Software engineer",
            "C# engineer, Software engineer",
            "Accountant, Accountant",
            "Chief Accountant, Accountant"
    })
    void testAssignmentExamples(String testValue, String expected) {
        // Arrange + Act
        var result = testable.normalise(testValue);

        // Assert
        assertThat(result.normalised()).as("valid normalised job title").isEqualTo(expected);
        assertThat(result.original()).as("original job title").isEqualTo(testValue);
    }

    @DisplayName("""
            Given an input equal to a normalised title
            When the job title is matched
            Then it is an exact match with quality 1.0
            """)
    @Test
    void testExactMatch() {
        // Arrange + Act
        var result = testable.normalise("quantity Surveyor");

        // Assert
        assertThat(result.normalised()).as("valid normalised job title").isEqualTo("Quantity surveyor");
        assertThat(result.quality()).as("quality").isEqualTo(1.0);
        assertThat(result.status()).as("match status").isEqualTo(MatchStatus.EXACT);
    }

    @DisplayName("""
            Given an input that only partly matches a normalised title
            When the job title is matched
            Then it is a partial match with a quality below 1.0
            """)
    @Test
    void testPartialMatch() {
        // Arrange + Act
        var result = testable.normalise("Java engineer");

        // Assert
        assertThat(result.status()).as("match status").isEqualTo(MatchStatus.PARTIAL);
        assertThat(result.quality()).as("quality").isGreaterThanOrEqualTo(0.4).isLessThan(1.0);
    }

    @DisplayName("""
            Given an input unrelated to any normalised title
            When the job title is matched
            Then there is no match and no normalised title
            """)
    @ParameterizedTest
    @ValueSource(strings = {"Chef", "xyz", "#!?"})
    void testNoMatch(String testValue) {
        // Arrange + Act
        var result = testable.normalise(testValue);

        // Assert
        assertThat(result.status()).as("match status").isEqualTo(MatchStatus.NO_MATCH);
        assertThat(result.normalised()).as("normalised job title").isNull();
        assertThat(result.quality()).as("quality").isLessThan(0.4);
    }

    @DisplayName("""
            Given a missing or blank input
            When the job title is matched
            Then it is rejected
            """)
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void testMissingInputIsRejected(String testValue) {
        // Arrange + Act + Assert
        assertThatThrownBy(() -> testable.normalise(testValue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("job title");
    }

    @DisplayName("""
            Given a scorer returning fixed qualities
            When the job title is matched
            Then the highest quality candidate is checked against the minimum quality
            """)
    @ParameterizedTest
    @CsvSource({
            "0.3, NO_MATCH",
            "0.5, PARTIAL",
            "1.0, EXACT"
    })
    void testMinimumQualityThreshold(double bestQuality, MatchStatus expected) {
        // Arrange
        var config = new NormaliserConfig(List.of("Low", "Best"), 0.5);
        SimilarityScorer scorer = (_, candidate) -> candidate.equals("Best") ? bestQuality : 0.1;
        testable = new Normaliser(config, scorer);

        // Act
        var result = testable.normalise("anything");

        // Assert
        assertThat(result.status()).as("match status").isEqualTo(expected);
        assertThat(result.quality()).as("quality").isEqualTo(bestQuality);
    }

    @DisplayName("""
            Given missing dependencies
            When the normaliser is created
            Then it is rejected
            """)
    @Test
    void testMissingDependenciesAreRejected() {
        // Arrange
        var config = new NormaliserConfig(List.of("Architect"), 0.5);
        var scorer = new TokenSimilarityScorer();

        // Act + Assert
        assertThatThrownBy(() -> new Normaliser(null, scorer))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Normaliser(config, null))
                .isInstanceOf(NullPointerException.class);
    }
}
