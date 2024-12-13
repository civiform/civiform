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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import models.ProgramNotificationPreference;
import repository.QuestionRepository;
import services.ErrorAnd;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.TextQuestionDefinition;

/**
 * A service responsible for helping admins migrate program definitions between different
 * environments.
 */
public final class ProgramMigrationService {
  // We use `-_-` as the delimiter because it's unlikely to already be used in a question with a
  // name like `name - parent`.
  // It will transform to a key formatted like `%s__%s`
  private static final String CONFLICTING_QUESTION_FORMAT = "%s -_- %s";
  private static final Pattern SUFFIX_PATTERN = Pattern.compile(" -_- [a-z]+$");
  private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";

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
   * questions. If a match is found, generate a new admin name of the <br>
   * format `orginal admin name -_- a`.
   *
   * <p>Return a map of old_question_name -> updated_question_data
   */
  public ImmutableMap<String, QuestionDefinition> maybeOverwriteQuestionName(
      ImmutableList<QuestionDefinition> questions) {
    List<String> newNamesSoFar = new ArrayList<>();
    return questions.stream()
        .collect(
            ImmutableMap.toImmutableMap(
                QuestionDefinition::getName,
                question -> {
                  String newAdminName = findUniqueAdminName(question.getName(), newNamesSoFar);
                  newNamesSoFar.add(newAdminName);
                  try {
                    return new QuestionDefinitionBuilder(question).setName(newAdminName).build();
                  } catch (UnsupportedQuestionTypeException error) {
                    throw new RuntimeException(error);
                  }
                }));
  }

  /**
   * Generate a new admin name for questions of the format "orginal admin name -_- a" where "a" is
   * the next consecutive letter such that we don't already have a question with that admin name
   * saved. For example if the admin name is "sample question" and we already have <br>
   * "sample question -_- a" and "sample question -_- b" saved in the db, the generated name will be
   * "sample question -_- c".
   */
  String findUniqueAdminName(String adminName, List<String> newNamesSoFar) {
    if (!nameHasConflict(adminName, newNamesSoFar)) {
      return adminName;
    }

    // If the question name contains a suffix of the form " -_- a" (for example "admin name -_- a"),
    // we want to strip off the " -_- n" to find the base name. This also allows us to correctly
    // increment the suffix of the base admin name so we don't end up with admin names like "admin
    // name -_- a -_- a".
    Matcher matcher = SUFFIX_PATTERN.matcher(adminName);
    String adminNameBase = adminName;
    if (matcher.find()) {
      adminNameBase = adminName.substring(0, matcher.start());
    }

    int extension = 0;
    String newName = "";
    do {
      extension++;
      newName =
          CONFLICTING_QUESTION_FORMAT.formatted(adminNameBase, convertNumberToSuffix(extension));
    } while (nameHasConflict(newName, newNamesSoFar));

    return newName;
  }

  private boolean nameHasConflict(String name, List<String> newNamesSoFar) {
    // Check if any of the names we've already generated might conflict with this one.
    // We can compare raw names, rather than keys, because everything we generate
    // follows the same pattern and will reduce to keys in the same way.
    if (newNamesSoFar.contains(name)) {
      return true;
    }

    QuestionDefinition testQuestion =
        new TextQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
                .build());

    return questionRepository.findConflictingQuestion(testQuestion).isPresent();
  }

  /**
   * Convert a number to the equivalent "excel column name".
   *
   * <p>For example, 5 maps to "e", and 28 maps to "ab".
   *
   * @param num to convert
   * @return The "excel column name" form of the number
   */
  String convertNumberToSuffix(int num) {
    String result = "";

    // Division algorithm to convert from base 10 to "base 26"
    int dividend = num; // 28
    while (dividend > 0) {
      // Subtract one so we're doing math with a zero-based index.
      // We need "a" to be 0, and "z" to be 25, so that 26 wraps around
      // to be "aa". "a" is "ten" in base 26.
      dividend = dividend - 1;
      int remainder = dividend % 26;
      result = ALPHABET.charAt(remainder) + result;
      dividend = dividend / 26;
      ;
    }

    return result;
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
