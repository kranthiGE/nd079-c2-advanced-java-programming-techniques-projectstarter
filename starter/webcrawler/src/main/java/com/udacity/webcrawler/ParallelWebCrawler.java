package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {

  private static final Logger log = LoggerFactory.getLogger(ParallelWebCrawler.class);

  private final Clock clock;
  private final PageParserFactory parserFactory;
  private final Duration timeout;
  private final int popularWordCount;
  private final int maxDepth;
  private final List<Pattern> ignoredUrls;
  private final ForkJoinPool pool;

  @Inject
  ParallelWebCrawler(
      Clock clock,
      PageParserFactory parserFactory,
      @Timeout Duration timeout,
      @PopularWordCount int popularWordCount,
      @MaxDepth int maxDepth,
      @TargetParallelism int threadCount,
      @IgnoredUrls List<Pattern> ignoredUrls) {
    this.clock = clock;
    this.parserFactory = parserFactory;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.maxDepth = maxDepth;
    this.ignoredUrls = ignoredUrls;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {
    Instant deadline = clock.instant().plus(timeout);
    Map<String, Integer> totalWordCounts = new ConcurrentHashMap<>();
     
    ConcurrentSkipListSet<String> visitedUrls = new ConcurrentSkipListSet<>();
    log.info("in crawl method on urls: " + startingUrls);
    for (String url : startingUrls) {
      
      Map<String, Integer> wordCounts = pool.invoke(new CrawlerTask.Builder()
                                            .setClock(clock)
                                            .setDeadline(deadline)
                                            .setIgnoredUrls(ignoredUrls)
                                            .setMaxDepth(maxDepth)
                                            .setParserFactory(parserFactory)
                                            .setUrl(url)
                                            .setVisitedUrls(visitedUrls)
                                            .build());
      if(wordCounts == null || wordCounts.isEmpty()){
        continue;
      }
      log.debug("wordCounts size = " + wordCounts.size());
      wordCounts.entrySet()
              .forEach(
                e -> totalWordCounts.compute(e.getKey(), (k, v) -> (v == null) ? e.getValue() : v + e.getValue())
              );
    }
    log.info("crawling has completed for " + startingUrls.toString() + " and totalWordCounts size = " + totalWordCounts.size());
    
    if (totalWordCounts.isEmpty()) {
      return new CrawlResult.Builder()
          .setWordCounts(totalWordCounts)
          .setUrlsVisited(visitedUrls.size())
          .build();
    }

    return new CrawlResult.Builder()
        .setWordCounts(WordCounts.sort(totalWordCounts, popularWordCount))
        .setUrlsVisited(visitedUrls.size())
        .build();
  }

  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }
}
