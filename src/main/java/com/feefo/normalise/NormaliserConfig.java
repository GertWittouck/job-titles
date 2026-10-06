package com.feefo.normalise;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * The configurable settings of the job title normaliser.
 *
 * @param normalisedTitles the job titles that inputs are normalised to
 * @param minimumQuality   the quality a match needs to reach, between 0.0 and 1.0
 */
public record NormaliserConfig(List<String> normalisedTitles, double minimumQuality) {

    private static final Logger LOG = LogManager.getLogger(NormaliserConfig.class);

    private static final String DEFAULT_RESOURCE = "normaliser.properties";
    private static final String TITLES_KEY = "normaliser.titles";
    private static final String MINIMUM_QUALITY_KEY = "normaliser.minimum-quality";

    public NormaliserConfig {
        Objects.requireNonNull(normalisedTitles, "normalised titles must not be null");
        normalisedTitles = List.copyOf(normalisedTitles);
        if (normalisedTitles.isEmpty()) {
            throw new IllegalArgumentException("at least one normalised title is required");
        }
        if (normalisedTitles.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("normalised titles must not be blank");
        }
        if (!(minimumQuality >= 0.0 && minimumQuality <= 1.0)) {
            throw new IllegalArgumentException("minimum quality must be between 0.0 and 1.0 but was " + minimumQuality);
        }
    }

    /**
     * Loads the configuration from {@value DEFAULT_RESOURCE} on the classpath.
     */
    public static NormaliserConfig load() {
        return load(DEFAULT_RESOURCE);
    }

    /**
     * Loads the configuration from the given properties file on the classpath.
     *
     * @throws ConfigurationException when the file cannot be read or contains invalid values
     */
    public static NormaliserConfig load(String resource) {
        var properties = readProperties(resource);
        var titles = parseTitles(properties, resource);
        var minimumQuality = parseMinimumQuality(properties, resource);

        try {
            var config = new NormaliserConfig(titles, minimumQuality);
            LOG.info("Loaded {} normalised job titles from {} (minimum quality {})", titles.size(), resource, minimumQuality);
            return config;
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException("Invalid configuration in " + resource + ": " + e.getMessage(), e);
        }
    }

    private static Properties readProperties(String resource) {
        try (var stream = NormaliserConfig.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new ConfigurationException("Configuration file " + resource + " was not found on the classpath");
            }
            var properties = new Properties();
            properties.load(stream);
            return properties;
        } catch (IOException e) {
            throw new ConfigurationException("Configuration file " + resource + " could not be read", e);
        }
    }

    private static List<String> parseTitles(Properties properties, String resource) {
        var titles = Arrays.stream(requiredValue(properties, TITLES_KEY, resource).split(","))
                .map(String::strip)
                .filter(title -> !title.isEmpty())
                .toList();

        if (titles.isEmpty()) {
            throw new ConfigurationException(TITLES_KEY + " in " + resource + " must contain at least one title");
        }

        return titles;
    }

    private static double parseMinimumQuality(Properties properties, String resource) {
        var value = requiredValue(properties, MINIMUM_QUALITY_KEY, resource);
        double minimumQuality;
        try {
            minimumQuality = Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new ConfigurationException(MINIMUM_QUALITY_KEY + " in " + resource + " must be a number but was '" + value + "'", e);
        }
        if (!(minimumQuality >= 0.0 && minimumQuality <= 1.0)) {
            throw new ConfigurationException(MINIMUM_QUALITY_KEY + " in " + resource + " must be between 0.0 and 1.0 but was " + value);
        }
        return minimumQuality;
    }

    private static String requiredValue(Properties properties, String key, String resource) {
        var value = properties.getProperty(key);
        if (StringUtils.isBlank(value)) {
            throw new ConfigurationException("Missing required property " + key + " in " + resource);
        }
        return value.strip();
    }
}
