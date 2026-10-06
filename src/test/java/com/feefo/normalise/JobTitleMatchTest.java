package com.feefo.normalise;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobTitleMatchTest {

    @DisplayName("""
            Given a normalised title with a perfect quality
            When a match is created
            Then the status is EXACT
            """)
    @Test
    void testPerfectQualityIsExactMatch() {
        // Arrange + Act
        var result = JobTitleMatch.of("Accountant", "Accountant", 1.0);

        // Assert
        assertThat(result.status()).as("match status").isEqualTo(MatchStatus.EXACT);
        assertThat(result.normalised()).as("normalised job title").isEqualTo("Accountant");
        assertThat(result.hasMatch()).as("has match").isTrue();
    }

    @DisplayName("""
            Given a normalised title with a quality below 1.0
            When a match is created
            Then the status is PARTIAL
            """)
    @Test
    void testImperfectQualityIsPartialMatch() {
        // Arrange + Act
        var result = JobTitleMatch.of("Java engineer", "Software engineer", 0.6);

        // Assert
        assertThat(result.status()).as("match status").isEqualTo(MatchStatus.PARTIAL);
        assertThat(result.quality()).as("quality").isEqualTo(0.6);
        assertThat(result.hasMatch()).as("has match").isTrue();
    }

    @DisplayName("""
            Given an input without a good enough match
            When a no-match result is created
            Then there is no normalised title
            """)
    @Test
    void testNoMatchHasNoNormalisedTitle() {
        // Arrange + Act
        var result = JobTitleMatch.noMatch("Chef", 0.2);

        // Assert
        assertThat(result.status()).as("match status").isEqualTo(MatchStatus.NO_MATCH);
        assertThat(result.normalised()).as("normalised job title").isNull();
        assertThat(result.original()).as("original job title").isEqualTo("Chef");
        assertThat(result.hasMatch()).as("has match").isFalse();
    }

    @DisplayName("""
            Given a quality outside the range 0.0 to 1.0
            When a match is created
            Then it is rejected
            """)
    @ParameterizedTest
    @ValueSource(doubles = {-0.1, 1.1, Double.NaN})
    void testQualityOutOfRangeIsRejected(double quality) {
        // Arrange + Act + Assert
        assertThatThrownBy(() -> JobTitleMatch.of("Java engineer", "Software engineer", quality))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quality");
    }

    @DisplayName("""
            Given a missing original or normalised title
            When a match is created
            Then it is rejected
            """)
    @Test
    void testMissingTitlesAreRejected() {
        // Arrange + Act + Assert
        assertThatThrownBy(() -> JobTitleMatch.of(null, "Accountant", 1.0))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("original");
        assertThatThrownBy(() -> JobTitleMatch.of("Accountant", null, 1.0))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("normalised");
    }

    @DisplayName("""
            Given a status that contradicts the normalised title
            When a match is created
            Then it is rejected
            """)
    @Test
    void testInconsistentStatusIsRejected() {
        // Arrange + Act + Assert
        assertThatThrownBy(() -> new JobTitleMatch("Chef", "Accountant", 0.2, MatchStatus.NO_MATCH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JobTitleMatch("Chef", null, 0.2, MatchStatus.PARTIAL))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
