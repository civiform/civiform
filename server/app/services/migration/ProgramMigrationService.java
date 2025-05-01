package services.migration;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProgramAcls;
import autovalue.shaded.com.google.common.annotations.VisibleForTesting;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import controllers.admin.ProgramMigrationWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import models.ProgramModel;
import models.ProgramNotificationPreference;
import models.QuestionModel;
import repository.ApplicationStatusesRepository;
import repository.ProgramRepository;
import repository.QuestionRepository;
import repository.VersionRepository;
import services.ErrorAnd;
import services.LocalizedStrings;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.TextQuestionDefinition;
import services.statuses.StatusDefinitions;

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

  private final ApplicationStatusesRepository applicationStatusesRepository;
  private final ObjectMapper objectMapper;
  private final ProgramRepository programRepository;
  private final QuestionRepository questionRepository;
  private final VersionRepository versionRepository;

  @Inject
  public ProgramMigrationService(
      ApplicationStatusesRepository applicationStatusesRepository,
      ObjectMapper objectMapper,
      ProgramRepository programRepository,
      QuestionRepository questionRepository,
      VersionRepository versionRepository) {
    // These extra modules let ObjectMapper serialize Guava types like ImmutableList.
    this.objectMapper =
        checkNotNull(objectMapper)
            .registerModule(new GuavaModule())
            .registerModule(new Jdk8Module())
            .configure(Feature.INCLUDE_SOURCE_IN_LOCATION, true);
    this.applicationStatusesRepository = checkNotNull(applicationStatusesRepository);
    this.programRepository = checkNotNull(programRepository);
    this.questionRepository = checkNotNull(questionRepository);
    this.versionRepository = checkNotNull(versionRepository);
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
   * ProgramMigrationWrapper}.
   *
   * @param programJson The JSON string to deserialize.
   * @return An {@link ErrorAnd} containing either the deserialized {@link ProgramMigrationWrapper}
   *     or exactly error message.
   */
  public ErrorAnd<ProgramMigrationWrapper, String> deserialize(String programJson) {
    return deserialize(programJson, ImmutableMap.of());
  }

  /**
   * Attempts to deserialize the provided {@code programJson} into an instance of {@link
   * ProgramMigrationWrapper}, including any {@code duplicateHandling} options that are specified.
   *
   * @param programJson The JSON string to deserialize.
   * @param duplicateHandling A map of question names to handling options. Guaranteed to be empty if
   *     the feature is not yet launched or the request is not from the right page.
   * @return An {@link ErrorAnd} containing either the deserialized {@link ProgramMigrationWrapper}
   *     or exactly one error message.
   */
  public ErrorAnd<ProgramMigrationWrapper, String> deserialize(
      String programJson, ImmutableMap<String, String> duplicateHandling) {
    try {
      ProgramMigrationWrapper programMigrationWrapper =
          objectMapper.readValue(programJson, ProgramMigrationWrapper.class);
      if (!duplicateHandling.isEmpty()) {
        programMigrationWrapper.setDuplicateQuestionHandlingOptions(
            duplicateHandling.entrySet().stream()
                .map(
                    e ->
                        Maps.immutableEntry(
                            e.getKey(),
                            ProgramMigrationWrapper.DuplicateQuestionHandlingOption.valueOf(
                                e.getValue())))
                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)));
      }
      return ErrorAnd.of(programMigrationWrapper);
    } catch (RuntimeException | JsonProcessingException e) {
      return ErrorAnd.error(
          ImmutableSet.of(String.format("JSON is incorrectly formatted: %s", e.getMessage())));
    }
  }

  /** Returns only the admin names that already exist in the question bank. */
  public ImmutableList<String> getExistingAdminNames(ImmutableList<QuestionDefinition> questions) {
    return questions.stream()
        .filter(q -> questionRepository.findConflictingQuestion(q).isPresent())
        .map(QuestionDefinition::getName)
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Checks if there are existing questions that match the admin id of any of the incoming
   * questions. If a match is found, generate a new admin name of the <br>
   * format `orginal admin name -_- a`.
   *
   * <p>Return a map of old_question_name -> updated_question_data
   *
   * <p>TODO: #9628 - Make private once legacy UI is cleaned up
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
          CONFLICTING_QUESTION_FORMAT.formatted(
              adminNameBase, Utils.convertNumberToSuffix(extension));
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

  /**
   * Saves the specified program to the DB, and processes all questions associated with it.
   *
   * @param programDefinition the {@link ProgramDefinition} to save
   * @param questionDefinitions the {@link QuestionDefinition}s associated with the program
   * @param withDuplicates whether to allow duplicate question names
   * @param duplicateHandlingEnabled whether to allow admin-specified duplicate handling
   */
  public ProgramModel saveImportedProgram(
      ProgramDefinition programDefinition,
      ImmutableList<QuestionDefinition> questionDefinitions,
      boolean withDuplicates,
      boolean duplicateHandlingEnabled) {
    ProgramDefinition updatedProgram = programDefinition;
    if (questionDefinitions != null) {
      if (duplicateHandlingEnabled && withDuplicates) {
        // With the new duplicate-handling UI, we do not show admins a de-duped/suffixed admin
        // name before saving the imported program. Instead, we must calculate that name at this
        // point.
        questionDefinitions =
            ImmutableList.copyOf(maybeOverwriteQuestionName(questionDefinitions).values());
      }
      ImmutableMap<Long, QuestionDefinition> questionsOnJsonById =
          questionDefinitions.stream()
              .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getId, qd -> qd));

      ImmutableMap<String, QuestionDefinition> updatedQuestionsMap =
          updateEnumeratorIdsAndSaveAllQuestions(questionDefinitions, questionsOnJsonById);

      ImmutableList<BlockDefinition> updatedBlockDefinitions =
          Utils.updateBlockDefinitions(programDefinition, questionsOnJsonById, updatedQuestionsMap);

      updatedProgram =
          programDefinition.toBuilder().setBlockDefinitions(updatedBlockDefinitions).build();
    }

    // TODO: #9628 - Support admin-specified duplicate handling per-question
    ProgramModel savedProgram =
        programRepository.insertProgramSync(
            new ProgramModel(updatedProgram, versionRepository.getDraftVersionOrCreate()));

    // If we are re-using existing questions, we will put all programs in draft mode to ensure no
    // errors. We could also go through each question being updated and see what program it
    // applies to, but this is more straightforward.
    if (!withDuplicates) {
      ImmutableList<Long> programsInDraft =
          versionRepository.getProgramsForVersion(versionRepository.getDraftVersion()).stream()
              .map(p -> p.id)
              .collect(ImmutableList.toImmutableList());
      versionRepository
          .getProgramsForVersion(versionRepository.getActiveVersion())
          .forEach(
              program -> {
                if (!programsInDraft.contains(program.id)) {
                  programRepository.createOrUpdateDraft(program);
                }
              });
    }

    // TODO(#8613) migrate application statuses for the program
    applicationStatusesRepository.createOrUpdateStatusDefinitions(
        updatedProgram.adminName(), new StatusDefinitions());

    return savedProgram;
  }

  /**
   * Save all questions and then update enumerator child questions with the correct ids of their
   * newly saved parent questions.
   */
  @VisibleForTesting
  ImmutableMap<String, QuestionDefinition> updateEnumeratorIdsAndSaveAllQuestions(
      ImmutableList<QuestionDefinition> questionDefinitions,
      ImmutableMap<Long, QuestionDefinition> questionsOnJsonById) {

    // Save all the questions
    ImmutableList<QuestionModel> newlySavedQuestions =
        questionRepository.bulkCreateQuestions(questionDefinitions);

    ImmutableMap<String, QuestionDefinition> newlySaveQuestionsByAdminName =
        newlySavedQuestions.stream()
            .map(question -> question.getQuestionDefinition())
            .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getName, qd -> qd));

    ImmutableMap<String, QuestionDefinition> fullyUpdatedQuestions =
        newlySavedQuestions.stream()
            .map(
                question -> {
                  QuestionDefinition qd = question.getQuestionDefinition();
                  if (qd.getEnumeratorId().isPresent()) {
                    // The child question was saved with the incorrect enumerator id so we need to
                    // update it
                    Long oldEnumeratorId = qd.getEnumeratorId().get();
                    // Use the old enumerator id to get the admin name of the parent question off
                    // the old question map
                    String parentQuestionAdminName =
                        questionsOnJsonById.get(oldEnumeratorId).getName();
                    // Use the admin name to get the updated id for the parent question off the new
                    // question map
                    Long newlySavedParentQuestionId =
                        newlySaveQuestionsByAdminName.get(parentQuestionAdminName).getId();
                    // Update the child question with the correct id and save the question
                    qd = questionRepository.updateEnumeratorId(qd, newlySavedParentQuestionId);
                    qd = questionRepository.createOrUpdateDraft(qd).getQuestionDefinition();
                  }
                  return qd;
                })
            .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getName, qd -> qd));

    return fullyUpdatedQuestions;
  }
}
