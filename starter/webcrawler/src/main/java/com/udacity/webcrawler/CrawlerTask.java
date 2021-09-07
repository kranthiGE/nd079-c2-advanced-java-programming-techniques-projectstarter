package com.udacity.webcrawler;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CrawlerTask extends RecursiveTask<Map<String, Integer>> {
    
    private static final Logger log = LoggerFactory.getLogger(CrawlerTask.class);
    
    private final String url;
    private final Instant deadline;
    private final int maxDepth;
    private final Set<String> visitedUrls;
    private final Clock clock;
    private final List<Pattern> ignoredUrls;
    private final PageParserFactory parserFactory;

    private CrawlerTask(
        String url, 
        Instant deadline,
        int maxDepth,
        Set<String> visitedUrls,
        Clock clock,
        List<Pattern> ignoredUrls,
        PageParserFactory parserFactory
        ){
            this.url = url;
            this.deadline = deadline;
            this.maxDepth = maxDepth;
            this.visitedUrls = visitedUrls;
            this.clock = clock;
            this.ignoredUrls = ignoredUrls;
            this.parserFactory = parserFactory;
    }

    public String getUrl() {
        return url;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public Set<String> getVisitedUrls() {
        return visitedUrls;
    }

    public static final class Builder {
        private String url;
        private Instant deadline;
        private int maxDepth;
        private Set<String> visitedUrls;
        private Clock clock;
        private List<Pattern> ignoredUrls;
        private PageParserFactory parserFactory;

        public Builder setUrl(String url){
            this.url = url;
            return this;
        }

        public Builder setDeadline(Instant deadline){
            this.deadline = deadline;
            return this;
        }

        public Builder setMaxDepth(int maxDepth){
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder setVisitedUrls(Set<String> visitedUrls){
            this.visitedUrls = visitedUrls;
            return this;
        }

        public Builder setClock(Clock clock){
            this.clock = clock;
            return this;
        }

        public Builder setIgnoredUrls(List<Pattern> ignoredUrls){
            this.ignoredUrls = ignoredUrls;
            return this;
        }

        public Builder setParserFactory(PageParserFactory parserFactory){
            this.parserFactory = parserFactory;
            return this;
        }

        public CrawlerTask build(){
            return new CrawlerTask(url, deadline, maxDepth, visitedUrls, 
                            clock, ignoredUrls, parserFactory);
        }
    }
    
    @Override
    protected Map<String, Integer> compute() {
        log.debug("In compute for url " + url);
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return null;
        }
        for (Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return null;
            }
        }
        if (visitedUrls.contains(url)) {
            return null;
        }
        visitedUrls.add(url);
        PageParser.Result result = parserFactory.get(url).parse();
        Map<String, Integer> wordCounts = new ConcurrentHashMap<>();
        wordCounts.putAll(result.getWordCounts());

        // submit multiple subtasks to RecursiveTask to run invoke them all
        List<CrawlerTask> subCrawlerTasks = result.getLinks()
                                                .stream()
                                                .map(
                                                    l -> (new CrawlerTask.Builder()
                                                    .setClock(clock)
                                                    .setIgnoredUrls(ignoredUrls)
                                                    .setDeadline(deadline)
                                                    .setMaxDepth(maxDepth - 1)
                                                    .setParserFactory(parserFactory)
                                                    .setVisitedUrls(visitedUrls)
                                                    .setUrl(l)
                                                    .build())
                                                ).collect(Collectors.toList());
        // invoke all the crawler sub tasks
        invokeAll(subCrawlerTasks);
        // add the word count from each sub task
        subCrawlerTasks.stream()
                        .forEach(
                            task -> {
                                Map<String, Integer> newCount = task.join();
                                if(newCount != null){
                                    newCount.entrySet()
                                            .forEach(
                                                e -> wordCounts.compute(e.getKey(), 
                                                (k, v) -> (v == null) ? e.getValue() : v + e.getValue()
                                                )
                                            );
                                }
                            }
                        );
        log.debug("In compute, wordCount size is " + wordCounts.size());
        return wordCounts;
    }
}
