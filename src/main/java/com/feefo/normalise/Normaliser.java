package com.feefo.normalise;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Comparator;
import java.util.Objects;

/**
 * Normalises free-text job titles to the best matching title from a list of normalised job titles.
 */
public class Normaliser {

    private static final Logger LOG = LogManager.getLogger(Normaliser.class);

    private final NormaliserConfig config;
    private final SimilarityScorer scorer;

    public Normaliser(NormaliserConfig config, SimilarityScorer scorer) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.scorer = Objects.requireNonNull(scorer, "scorer must not be null");
    }

    /**
     * @param jobTitle the free-text job title to normalise
     * @return the best match, or a {@link MatchStatus#NO_MATCH} result when no title reaches the minimum quality
     * @throws IllegalArgumentException when the job title is null or blank
     */
    public JobTitleMatch normalise(String jobTitle) {
        if (StringUtils.isBlank(jobTitle)) {
            throw new IllegalArgumentException("job title must not be null or blank");
        }

        var best = config.normalisedTitles().stream()
                .map(candidate -> score(jobTitle, candidate))
                .max(Comparator.comparingDouble(ScoredTitle::quality))
                .orElseThrow();

        var match = best.quality() < config.minimumQuality()
                ? JobTitleMatch.noMatch(jobTitle, best.quality())
                : JobTitleMatch.of(jobTitle, best.title(), best.quality());

        LOG.debug("Matched '{}' to '{}' with quality {} ({})", jobTitle, best.title(), best.quality(), match.status());

        return match;
    }

    private ScoredTitle score(String jobTitle, String candidate) {
        var quality = scorer.score(jobTitle, candidate);
        LOG.trace("'{}' scored {} against '{}'", jobTitle, quality, candidate);
        return new ScoredTitle(candidate, quality);
    }

    private record ScoredTitle(String title, double quality) {
    }
}
