package com.feefo.normalise;

import java.util.Objects;

/**
 * The result of matching a job title against the normalised job titles.
 *
 * @param original   the job title as it was provided
 * @param normalised the best matching normalised title, or {@code null} when there is no match
 * @param quality    how well the input matched, between 0.0 and 1.0
 * @param status     whether the match is exact, partial or missing
 */
public record JobTitleMatch(
        String original,
        String normalised,
        double quality,
        MatchStatus status) {

    public JobTitleMatch {
        Objects.requireNonNull(original, "original job title must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (!(quality >= 0.0 && quality <= 1.0)) {
            throw new IllegalArgumentException("quality must be between 0.0 and 1.0 but was " + quality);
        }
        if ((status == MatchStatus.NO_MATCH) != (normalised == null)) {
            throw new IllegalArgumentException(
                    "a normalised title must be present unless the status is NO_MATCH, but status was " + status);
        }
    }

    /**
     * Creates a successful match; the status is derived from the quality.
     */
    public static JobTitleMatch of(String original, String normalised, double quality) {
        Objects.requireNonNull(normalised, "normalised job title must not be null");
        var status = quality == 1.0 ? MatchStatus.EXACT : MatchStatus.PARTIAL;
        return new JobTitleMatch(original, normalised, quality, status);
    }

    /**
     * Creates a result for an input that did not match any normalised title well enough.
     */
    public static JobTitleMatch noMatch(String original, double quality) {
        return new JobTitleMatch(original, null, quality, MatchStatus.NO_MATCH);
    }

    public boolean hasMatch() {
        return status != MatchStatus.NO_MATCH;
    }
}
