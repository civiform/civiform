package services.applicant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ParseContext;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import java.util.EnumSet;
import javax.inject.Singleton;
import services.ObjectMapperSingleton;

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
    // Use legacy serialization settings. (De)serialization errors may occur if changed.
    ObjectMapper mapper = ObjectMapperSingleton.createLegacyCopy();

    return Configuration.builder()
        .jsonProvider(new JacksonJsonProvider(mapper))
        .mappingProvider(new JacksonMappingProvider(mapper))
        .options(EnumSet.noneOf(Option.class))
        .build();
  }
}
