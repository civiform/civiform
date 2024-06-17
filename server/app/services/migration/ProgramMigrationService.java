package services.migration;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import controllers.admin.ProgramMigrationWrapper;
import services.ErrorAnd;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

/**
 * A service responsible for helping admins migrate program definitions between different
 * environments.
 */
public final class ProgramMigrationService {
  private final ObjectMapper objectMapper;

  @Inject
  public ProgramMigrationService(ObjectMapper objectMapper) {
    // These extra modules let ObjectMapper serialize Guava types like ImmutableList.

    JsonFactory jsonFactory = objectMapper.getFactory();
    jsonFactory.enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION);
    this.objectMapper =
        checkNotNull(objectMapper)
            .registerModule(new GuavaModule())
            .registerModule(new Jdk8Module());
  }

  /**
   * Attempts to convert the provided {@code program} and {@code questions} into a serialized
   * instance of {@link ProgramMigrationWrapper}, returning either the successfully serialized
   * string or an error message.
   *
   * <p>If an error is returned, there will always be exactly one error message.
   */
  public ErrorAnd<String, String> serialize(
      ProgramDefinition program, ImmutableList<QuestionDefinition> questions) {
    try {
      String programJson =
          objectMapper
              .writerWithDefaultPrettyPrinter()
              .writeValueAsString(new ProgramMigrationWrapper(program, questions));
      return ErrorAnd.of(programJson);
    } catch (JsonProcessingException e) {
      return ErrorAnd.error(
          ImmutableSet.of(String.format("Program could not be serialized: %s", e)));
    }
  }

  /**
   * Attempts to deserialize the provided {@code programJson} into an instance of {@link
   * ProgramMigrationWrapper}, returning either the successfully deserialized wrapper or an error
   * message.
   *
   * <p>If an error is returned, there will always be exactly one error message.
   */
  public ErrorAnd<ProgramMigrationWrapper, String> deserialize(String programJson) {
    try {
      ProgramMigrationWrapper programMigrationWrapper =
          objectMapper.readValue(programJson, ProgramMigrationWrapper.class);
      return ErrorAnd.of(programMigrationWrapper);
    } catch (RuntimeException | JsonProcessingException e) {
      return ErrorAnd.error(
          ImmutableSet.of(String.format("JSON is incorrectly formatted: %s", e.getMessage())));
    }
  }
}
