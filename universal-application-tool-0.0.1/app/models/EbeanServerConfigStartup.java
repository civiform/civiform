package models;

import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ebean.config.ServerConfig;
import io.ebean.event.ServerConfigStartup;
import javax.inject.Inject;

/** Provides the same {@link ObjectMapper} that is used in Play. */
public class EbeanServerConfigStartup implements ServerConfigStartup {

  public void onStart(ServerConfig serverConfig) {
    ObjectMapper mapper = new ObjectMapper().registerModule(new GuavaModule());
    serverConfig.setObjectMapper(mapper);
  }
}
