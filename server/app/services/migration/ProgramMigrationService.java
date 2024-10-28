package services.migration;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProgramAcls;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import controllers.admin.ProgramMigrationWrapper;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import models.ProgramNotificationPreference;
import repository.QuestionRepository;
import services.ErrorAnd;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;

/**
 * A service responsible for helping admins migrate program definitions between different
 * environments.
 */
public final class ProgramMigrationService {
  private final ObjectMapper objectMapper;
  private final QuestionRepository questionRepository;

  @Inject
  public ProgramMigrationService(ObjectMapper objectMapper, QuestionRepository questionRepository) {
    // These extra modules let ObjectMapper serialize Guava types like ImmutableList.
    this.objectMapper =
        checkNotNull(objectMapper)
            .registerModule(new GuavaModule())
            .registerModule(new Jdk8Module())
            .configure(Feature.INCLUDE_SOURCE_IN_LOCATION, true);
    this.questionRepository = checkNotNull(questionRepository);
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

  /**
   * Checks if there are existing questions that match the admin id of any of the incoming
   * questions. If a match is found, generate a new admin name of the format "orginal admin name-n".
   *
   * <p>Return a map of old_question_name -> updated_question_data
   */
  public ImmutableMap<String, QuestionDefinition> maybeOverwriteQuestionName(
      ImmutableList<QuestionDefinition> questions) {
    return questions.stream()
        .collect(
            ImmutableMap.toImmutableMap(
                QuestionDefinition::getName,
                question -> {
                  String newAdminName = maybeGenerateNewAdminName(question.getName());
                  try {
                    return new QuestionDefinitionBuilder(question).setName(newAdminName).build();
                  } catch (UnsupportedQuestionTypeException error) {
                    throw new RuntimeException(error);
                  }
                }));
  }

  /**
   * Generate a new admin name for questions of the format "orginal admin name-n" where "n" is the
   * next consecutive number such that we don't already have a question with that admin name saved.
   * For example if the admin name is "sample question" and we already have "sample question-1" and
   * "sample question-2" saved in the db, the generated name will be "sample question-3"
   */
  public String maybeGenerateNewAdminName(String adminName) {
    // If the question name contains a suffix of the form "-n" (for example "admin-name-1"), we want
    // to strip off the "-n" before searching for the admin name to ensure all similar names are
    // returned. This also allows us to correctly increment the suffix of the base admin name so we
    // don't end up with admin names like "admin-name-1-1".
    Pattern HYPHEN_DIGIT_PATTERN = Pattern.compile("-[0-9]+");
    Matcher matcher = HYPHEN_DIGIT_PATTERN.matcher(adminName);
    if (matcher.find()) {
      int lastHyphenIndex = adminName.lastIndexOf("-");
      adminName = adminName.substring(0, lastHyphenIndex);
    }
    ImmutableList<String> similarAdminNames = questionRepository.getSimilarAdminNames(adminName);

    String newAdminName = adminName;
    int n = 1;
    while (similarAdminNames.contains(newAdminName)) {
      newAdminName = adminName + "-" + n;
      n++;
      continue;
    }
    return newAdminName;
  }

  /**
   * Prepare the {@link ProgramDefinition} for export by making any changes required, such as
   * removing settings that may be specific to an environment.
   *
   * @param programDefinition the {@link ProgramDefinition} to modify
   * @return a modified {@link ProgramDefinition} ready for export
   */
  public ProgramDefinition prepForExport(ProgramDefinition programDefinition) {
    return programDefinition.toBuilder()
        // TODO(#8613) migrate program categories and associated TI groups
        .setCategories(ImmutableList.of())
        .setAcls(new ProgramAcls())
        // Don't export environment specific notification preferences
        .setNotificationPreferences(ImmutableList.of())
        // Explicitly set program type to DEFAULT so we don't import program as a
        // pre-screener/common intake
        .setProgramType(ProgramType.DEFAULT)
        .build();
  }

  /**
   * Prepare the {@link ProgramDefinition} for import by making any changes required, such as
   * setting defaults.
   *
   * @param programDefinition the {@link ProgramDefinition} to modify
   * @return a modified {@link ProgramDefinition} ready for import
   */
  public ProgramDefinition prepForImport(ProgramDefinition programDefinition) {
    return programDefinition.toBuilder()
        .setNotificationPreferences(ProgramNotificationPreference.getDefaults())
        .build();
  }
}
