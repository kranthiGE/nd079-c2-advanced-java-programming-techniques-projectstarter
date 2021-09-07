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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((clock == null) ? 0 : clock.hashCode());
        result = prime * result + ((deadline == null) ? 0 : deadline.hashCode());
        result = prime * result + ((ignoredUrls == null) ? 0 : ignoredUrls.hashCode());
        result = prime * result + maxDepth;
        result = prime * result + ((parserFactory == null) ? 0 : parserFactory.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        result = prime * result + ((visitedUrls == null) ? 0 : visitedUrls.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CrawlerTask other = (CrawlerTask) obj;
        if (clock == null) {
            if (other.clock != null)
                return false;
        } else if (!clock.equals(other.clock))
            return false;
        if (deadline == null) {
            if (other.deadline != null)
                return false;
        } else if (!deadline.equals(other.deadline))
            return false;
        if (ignoredUrls == null) {
            if (other.ignoredUrls != null)
                return false;
        } else if (!ignoredUrls.equals(other.ignoredUrls))
            return false;
        if (maxDepth != other.maxDepth)
            return false;
        if (parserFactory == null) {
            if (other.parserFactory != null)
                return false;
        } else if (!parserFactory.equals(other.parserFactory))
            return false;
        if (url == null) {
            if (other.url != null)
                return false;
        } else if (!url.equals(other.url))
            return false;
        if (visitedUrls == null) {
            if (other.visitedUrls != null)
                return false;
        } else if (!visitedUrls.equals(other.visitedUrls))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "CrawlerTask [clock=" + clock + ", deadline=" + deadline + ", ignoredUrls=" + ignoredUrls + ", maxDepth="
                + maxDepth + ", parserFactory=" + parserFactory + ", url=" + url + ", visitedUrls=" + visitedUrls + "]";
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
