package com.feefo.normalise;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NormaliserConfigTest {

    @DisplayName("""
            Given the default properties file
            When the configuration is loaded
            Then it contains the normalised titles and minimum quality
            """)
    @Test
    void testLoadDefaultConfiguration() {
        // Arrange + Act
        var result = NormaliserConfig.load();

        // Assert
        assertThat(result.normalisedTitles()).as("normalised titles")
                .containsExactly("Architect", "Software engineer", "Quantity surveyor", "Accountant");
        assertThat(result.minimumQuality()).as("minimum quality").isEqualTo(0.4);
    }

    @DisplayName("""
            Given a properties file that does not exist
            When the configuration is loaded
            Then a configuration error names the file
            """)
    @Test
    void testMissingFileIsRejected() {
        // Arrange + Act + Assert
        assertThatThrownBy(() -> NormaliserConfig.load("config/does-not-exist.properties"))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining("config/does-not-exist.properties");
    }

    @DisplayName("""
            Given an invalid properties file
            When the configuration is loaded
            Then a configuration error explains the problem
            """)
    @ParameterizedTest
    @CsvSource({
            "config/missing-titles.properties, normaliser.titles",
            "config/empty-titles.properties, normaliser.titles",
            "config/missing-quality.properties, normaliser.minimum-quality",
            "config/non-numeric-quality.properties, normaliser.minimum-quality",
            "config/quality-out-of-range.properties, normaliser.minimum-quality"
    })
    void testInvalidFileIsRejected(String resource, String expectedKey) {
        // Arrange + Act + Assert
        assertThatThrownBy(() -> NormaliserConfig.load(resource))
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContaining(expectedKey)
                .hasMessageContaining(resource);
    }

    @DisplayName("""
            Given titles with surrounding spaces and empty entries
            When the configuration is loaded
            Then the titles are trimmed and empty entries skipped
            """)
    @Test
    void testTitlesAreTrimmed() {
        // Arrange + Act
        var result = NormaliserConfig.load("config/untidy-titles.properties");

        // Assert
        assertThat(result.normalisedTitles()).as("normalised titles")
                .containsExactly("Architect", "Accountant");
    }

    @DisplayName("""
            Given invalid values
            When the configuration is created directly
            Then it is rejected
            """)
    @Test
    void testInvalidValuesAreRejected() {
        // Arrange
        List<String> blankTitles = List.of(" ");
        List<String> architectTitle = List.of("Architect");

        // Act + Assert
        assertThatThrownBy(() -> new NormaliserConfig(List.of(), 0.4))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NormaliserConfig(blankTitles, 0.4))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NormaliserConfig(architectTitle, 1.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new NormaliserConfig(null, 0.4))
                .isInstanceOf(NullPointerException.class);
    }
}
