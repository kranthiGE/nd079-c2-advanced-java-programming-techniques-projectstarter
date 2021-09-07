package com.udacity.webcrawler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class that sorts the map of word counts.
 */
final class WordCounts {

  /**
   * Given an unsorted map of word counts, returns a new map whose word counts are sorted according
   * to the provided {@link WordCountComparator}, and includes only the top
   * {@param popluarWordCount} words and counts.
   *
   * @param wordCounts       the unsorted map of word counts.
   * @param popularWordCount the number of popular words to include in the result map.
   * @return a map containing the top {@param popularWordCount} words and counts in the right order.
   */
  static Map<String, Integer> sort(Map<String, Integer> wordCounts, int popularWordCount) {

    Map<String, Integer> sortedCounts = wordCounts.entrySet().stream()
                                                  .sorted((a, b) -> {
                                                    if (!a.getValue().equals(b.getValue())) {
                                                      return (b.getValue() - a.getValue());
                                                    }
                                                    if (a.getKey().length() != b.getKey().length()) {
                                                      return b.getKey().length() - a.getKey().length();
                                                    }
                                                    return a.getKey().compareTo(b.getKey());
                                                  })
                                                  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, 
                                                  (e1, e2) -> e2, LinkedHashMap::new));

    Map<String, Integer> topCounts = new LinkedHashMap<>();

    sortedCounts.entrySet().stream()
                            .limit(Math.min(popularWordCount, wordCounts.size()))
                            .forEach(e -> topCounts.put(e.getKey(), e.getValue()));
    
    return topCounts;
  }
}