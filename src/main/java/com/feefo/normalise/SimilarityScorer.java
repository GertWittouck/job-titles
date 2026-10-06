package com.feefo.normalise;

/**
 * Scores how similar an input job title is to a candidate normalised title.
 */
@FunctionalInterface
public interface SimilarityScorer {

    /**
     * @return a quality between 0.0 (nothing in common) and 1.0 (identical)
     */
    double score(String input, String candidate);
}
