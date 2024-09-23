package services.applicant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.cache.CacheProvider;
import com.jayway.jsonpath.spi.cache.NOOPCache;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import java.util.EnumSet;
import javax.inject.Singleton;

/** Custom JsonPathProvider used to parse applicant data in JSON format. */
@Singleton
public final class JsonPathProvider {

  private static final ParseContext JSON_PATH_PARSE_CONTEXT =
      JsonPath.using(generateConfiguration());

  /**
   * Gets a JsonPath {@link ParseContext} that uses Jackson as the JSON provider instead of the
   * JsonSmart default, which doesn't support all methods in {@link
   * com.jayway.jsonpath.ReadContext}. See https://github.com/json-path/JsonPath for more
   * information.
   *
   * @return a {@link ParseContext} that uses Jackson's ObjectMapper
   */
  public static ParseContext getJsonPath() {
    return JSON_PATH_PARSE_CONTEXT;
  }

  private static Configuration generateConfiguration() {
    ObjectMapper mapper =
        new ObjectMapper().registerModule(new GuavaModule()).registerModule(new Jdk8Module());
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    return Configuration.builder()
        .jsonProvider(new JacksonJsonProvider(mapper))
        .mappingProvider(new JacksonMappingProvider(mapper))
        .options(EnumSet.noneOf(Option.class))
        .build();
  }
}
