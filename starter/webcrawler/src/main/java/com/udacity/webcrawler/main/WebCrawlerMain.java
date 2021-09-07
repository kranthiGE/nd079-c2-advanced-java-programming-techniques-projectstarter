package com.udacity.webcrawler.main;

import com.google.inject.Guice;
import com.udacity.webcrawler.WebCrawler;
import com.udacity.webcrawler.WebCrawlerModule;
import com.udacity.webcrawler.json.ConfigurationLoader;
import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.json.CrawlResultWriter;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.profiler.Profiler;
import com.udacity.webcrawler.profiler.ProfilerModule;

import javax.inject.Inject;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Objects;

public final class WebCrawlerMain {

  private final CrawlerConfiguration config;

  private WebCrawlerMain(CrawlerConfiguration config) {
    this.config = Objects.requireNonNull(config);
  }

  @Inject
  private WebCrawler crawler;

  @Inject
  private Profiler profiler;

  private void run() throws Exception {
    Guice.createInjector(new WebCrawlerModule(config), new ProfilerModule()).injectMembers(this);

    CrawlResult result = crawler.crawl(config.getStartPages());
    CrawlResultWriter resultWriter = new CrawlResultWriter(result);
    // Write the crawl results to a JSON file (or System.out if the file name is empty)
    if(config.getResultPath().isEmpty() || config.getResultPath().isBlank()){
      // write to System.out
      StringWriter writer = new StringWriter();
      resultWriter.write(writer);
      System.out.print(writer.toString());
    } else {
      // write to a file based on the output file path in config
      resultWriter.write(Path.of(config.getResultPath()));
    }
    // Write the profile data to a text file (or System.out if the file name is empty)
    if(config.getProfileOutputPath().isEmpty() || config.getProfileOutputPath().isBlank()){
      // write to System.out
      StringWriter writer = new StringWriter();
      profiler.writeData(writer);
    } else {
      profiler.writeData(Path.of(config.getProfileOutputPath()));
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.out.println("Usage: WebCrawlerMain [starting-url]");
      return;
    }

    CrawlerConfiguration config = new ConfigurationLoader(Path.of(args[0])).load();
    new WebCrawlerMain(config).run();
  }
}
