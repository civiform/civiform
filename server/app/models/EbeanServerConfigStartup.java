package models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ebean.DatabaseBuilder;
import io.ebean.event.ServerConfigStartup;
import services.ObjectMapperSingleton;

/**
 * Provides a Jackson {@link ObjectMapper} that understands how to (de)serialize Guava types and
 * Java 8 Optionals. Note that this is necessary because Ebean uses a different ObjectMapper than
 * the one provided by the Play framework.
 */
public class EbeanServerConfigStartup implements ServerConfigStartup {
  @Override
  public void onStart(DatabaseBuilder config) {
    // Use legacy serialization settings. (De)serialization errors may occur if changed.
    ObjectMapper mapper = ObjectMapperSingleton.createLegacyCopy();
    config.objectMapper(mapper);
  }
}
