package com.feefo.normalise;

/**
 * Describes how well an input matched one of the normalised job titles.
 */
public enum MatchStatus {

    /** The input is the normalised title (quality 1.0). */
    EXACT,

    /** The input resembles a normalised title well enough, but not exactly. */
    PARTIAL,

    /** No normalised title reached the minimum quality. */
    NO_MATCH
}
