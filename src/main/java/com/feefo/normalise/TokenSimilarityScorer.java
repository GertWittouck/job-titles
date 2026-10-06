package com.feefo.normalise;

import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Scores job titles word by word.
 * <p>
 * Both titles are split into lower-case words. Every word is paired with its most similar word in the
 * other title, where word similarity is based on the edit (Levenshtein) distance. The quality is the
 * average of those best similarities, taken in both directions so that the score is symmetric and an
 * extra word such as "Chief" lowers the score without dominating it.
 */
public class TokenSimilarityScorer implements SimilarityScorer {

    private static final LevenshteinDistance LEVENSHTEIN = LevenshteinDistance.getDefaultInstance();

    @Override
    public double score(String input, String candidate) {
        Objects.requireNonNull(input, "input must not be null");
        Objects.requireNonNull(candidate, "candidate must not be null");

        var inputWords = words(input);
        var candidateWords = words(candidate);
        if (inputWords.isEmpty() || candidateWords.isEmpty()) {
            return 0.0;
        }
        return (averageBestSimilarity(inputWords, candidateWords)
                + averageBestSimilarity(candidateWords, inputWords)) / 2;
    }

    private static List<String> words(String title) {
        return Arrays.stream(title.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(word -> !word.isEmpty())
                .toList();
    }

    private static double averageBestSimilarity(List<String> from, List<String> to) {
        return from.stream()
                .mapToDouble(word -> to.stream().mapToDouble(other -> wordSimilarity(word, other)).max().orElse(0.0))
                .average()
                .orElse(0.0);
    }

    private static double wordSimilarity(String first, String second) {
        int longest = Math.max(first.length(), second.length());
        return 1.0 - (double) LEVENSHTEIN.apply(first, second) / longest;
    }
}
