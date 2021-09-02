package com.udacity.webcrawler.json;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class to write a {@link CrawlResult} to file.
 */
public final class CrawlResultWriter {
  private final CrawlResult result;

  /**
   * Creates a new {@link CrawlResultWriter} that will write the given {@link CrawlResult}.
   */
  public CrawlResultWriter(CrawlResult result) {
    this.result = Objects.requireNonNull(result);
  }

  /**
   * Formats the {@link CrawlResult} as JSON and writes it to the given {@link Path}.
   *
   * <p>If a file already exists at the path, the existing file should not be deleted; new data
   * should be appended to it.
   *
   * @param path the file path where the crawl result data should be written.
   * @throws IOException
   * @throws JsonMappingException
   * @throws JsonGenerationException
   */
  public void write(Path path) throws IOException {
    // This is here to get rid of the unused variable warning.
    Objects.requireNonNull(path);
    // create a json mapper and write the JSON content from CrawlResult object into file
    try(BufferedWriter writer = Files.newBufferedWriter(path)){
      write(writer);
    }
  }

  /**
   * Formats the {@link CrawlResult} as JSON and writes it to the given {@link Writer}.
   *
   * @param writer the destination where the crawl result data should be written.
   * @throws IOException
   * @throws JsonMappingException
   * @throws JsonGenerationException
   */
  public void write(Writer writer) 
  throws JsonGenerationException, JsonMappingException, IOException {
    // This is here to get rid of the unused variable warning.
    Objects.requireNonNull(writer);

    final ObjectMapper objectMapper = new ObjectMapper();
    // disabling the auto close feature of JACKSON that closes the writer
    objectMapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    objectMapper.writeValue(writer, result); 
  }
}
