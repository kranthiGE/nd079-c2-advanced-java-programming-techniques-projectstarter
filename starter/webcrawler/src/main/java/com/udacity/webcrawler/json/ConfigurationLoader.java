package com.udacity.webcrawler.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A static utility class that loads a JSON configuration file.
 */
public final class ConfigurationLoader {

  private final Path path;

  /**
   * Create a {@link ConfigurationLoader} that loads configuration from the given {@link Path}.
   */
  public ConfigurationLoader(Path path) {
    this.path = Objects.requireNonNull(path);
  }

  /**
   * Loads configuration from this {@link ConfigurationLoader}'s path
   *
   * @return the loaded {@link CrawlerConfiguration}.
   * @throws IOException
   * @throws ConfigLoaderException
   */
  public CrawlerConfiguration load() throws IOException, ConfigLoaderException {
    final CrawlerConfiguration crawlerConfiguration;
    // first read the input JSON file from path variable
    try(BufferedReader reader = Files.newBufferedReader(path)){
      // call read() to fetch crawlerConfiguration object
      crawlerConfiguration = read(reader);
    }
    return crawlerConfiguration;
  }

  /**
   * Loads crawler configuration from the given reader.
   *
   * @param reader a Reader pointing to a JSON string that contains crawler configuration.
   * @return a crawler configuration
   * @throws ConfigLoaderException
   * @throws IOException 
   * @throws JsonMappingException
   * @throws JsonParseException
   */
  public static CrawlerConfiguration read(Reader reader) 
  throws ConfigLoaderException {
    // This is here to get rid of the unused variable warning.
    Objects.requireNonNull(reader);
    // deserialize JSON to java object by creating ObjectMapper instance and reading the JSON under config folder
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
    try {
      return objectMapper.readValue(reader, CrawlerConfiguration.class);
    } catch (IOException e) {
      throw new ConfigLoaderException("IO exception in crawler config read", e);
    }
  }
}
