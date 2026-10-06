package com.feefo;

import com.feefo.normalise.ConfigurationException;
import com.feefo.normalise.Normaliser;
import com.feefo.normalise.NormaliserConfig;
import com.feefo.normalise.TokenSimilarityScorer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.Stream;

/**
 * Runs the examples from the assignment.
 */
public class Main {

    private static final Logger LOG = LogManager.getLogger(Main.class);

    static void main() {
        try {
            var config = NormaliserConfig.load();
            var normaliser = new Normaliser(config, new TokenSimilarityScorer());

            normaliseJobTitles(normaliser);
        } catch (ConfigurationException e) {
            LOG.error("Cannot start the job title normaliser: {}", e.getMessage());
            System.exit(1);
        } catch (RuntimeException e) {
            LOG.error("Unexpected error while normalising job titles", e);
            System.exit(1);
        }
    }

    /**
     * Normalises each example from the assignment and logs the outcome.
     */
    private static void normaliseJobTitles(Normaliser normaliser) {
        Stream.of("Java engineer", "C# engineer", "Software Engineer", "Lead Architect", "Accountant", "Chief Accountant", "Chef")
                .forEach(jobTitle -> normaliseJobTitle(normaliser, jobTitle));
    }

    /**
     * Normalises one job title and logs the outcome; an invalid job title is logged and skipped.
     */
    private static void normaliseJobTitle(Normaliser normaliser, String jobTitle) {
        try {
            var match = normaliser.normalise(jobTitle);
            var formattedQuality = "%.2f".formatted(match.quality());
            LOG.info("\"{}\" -> {} (quality {}, {})", match.original(), match.normalised(), formattedQuality, match.status());
        } catch (IllegalArgumentException e) {
            LOG.warn("Skipping invalid job title \"{}\": {}", jobTitle, e.getMessage());
        }
    }
}
