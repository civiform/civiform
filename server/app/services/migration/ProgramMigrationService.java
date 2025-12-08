package services.migration;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.ProgramAcls;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import controllers.admin.AdminImportController;
import controllers.admin.ProgramMigrationWrapper;
import controllers.admin.ProgramMigrationWrapper.DuplicateQuestionHandlingOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import models.ProgramModel;
import models.ProgramNotificationPreference;
import models.QuestionModel;
import models.VersionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.ApplicationStatusesRepository;
import repository.ProgramRepository;
import repository.QuestionRepository;
import repository.TransactionManager;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.LocalizedStrings;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.question.ActiveAndDraftQuestions;
import services.question.QuestionService;
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
  private static final Logger logger = LoggerFactory.getLogger(ProgramMigrationService.class);
  // We use `-_-` as the delimiter because it's unlikely to already be used in a question with a
  // name like `name - parent`.
  // It will transform to a key formatted like `%s__%s`
  private static final String CONFLICTING_QUESTION_FORMAT = "%s -_- %s";
  private static final Pattern SUFFIX_PATTERN = Pattern.compile(" -_- [a-z]+$");

  private final ApplicationStatusesRepository applicationStatusesRepository;
  private final ObjectMapper objectMapper;
  private final ProgramRepository programRepository;
  private final QuestionRepository questionRepository;
  private final QuestionService questionService;
  private final VersionRepository versionRepository;
  private final TransactionManager transactionManager;

  @Inject
  public ProgramMigrationService(
      ApplicationStatusesRepository applicationStatusesRepository,
      ObjectMapper objectMapper,
      ProgramRepository programRepository,
      QuestionRepository questionRepository,
      QuestionService questionService,
      VersionRepository versionRepository,
      TransactionManager transactionManager) {
    // These extra modules let ObjectMapper serialize Guava types like ImmutableList.
    this.objectMapper =
        checkNotNull(objectMapper)
            .registerModule(new GuavaModule())
            .registerModule(new Jdk8Module())
            .configure(Feature.INCLUDE_SOURCE_IN_LOCATION, true);
    this.applicationStatusesRepository = checkNotNull(applicationStatusesRepository);
    this.programRepository = checkNotNull(programRepository);
    this.questionRepository = checkNotNull(questionRepository);
    this.questionService = checkNotNull(questionService);
    this.versionRepository = checkNotNull(versionRepository);
    this.transactionManager = checkNotNull(transactionManager);
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

  /**
   * Validates questions before they are rendered to the admin.
   *
   * @param program The program definition being validated.
   * @param questions The questions to validate.
   * @param existingAdminNames The existing admin names of questions in the Question Bank.
   * @return A set of validation errors from all validation checks.
   */
  public ImmutableSet<CiviFormError> validateQuestions(
      ProgramDefinition program,
      ImmutableList<QuestionDefinition> questions,
      ImmutableList<String> existingAdminNames) {

    return ImmutableSet.<CiviFormError>builder()
        .addAll(QuestionValidationUtils.validateQuestionOptionAdminNames(questions))
        .addAll(QuestionValidationUtils.validateAllProgramQuestionsPresent(program, questions))
        .addAll(QuestionValidationUtils.validateYesNoQuestions(questions))
        .addAll(
            QuestionValidationUtils.validateRepeatedQuestions(
                program, questions, existingAdminNames))
        .build();
  }

  /**
   * Question keys are derived from Question Admin IDs in {@link QuestionDefinition} with {@code
   * adminId().replaceAll("[^a-zA-Z ]", "").replaceAll("\\s", "_")}. Question keys in the imported
   * JSON must be unique from one another AND if a question key exists in the DB, the question name
   * must be exactly similar to that in the DB.
   *
   * @throws RuntimeException if there are any issues with the uniqueness of question keys
   */
  public void validateQuestionKeyUniqueness(ImmutableList<QuestionDefinition> questions) {
    Map<String, ImmutableList<String>> questionKeysToNames = new HashMap<>();
    for (QuestionDefinition question : questions) {
      Optional<QuestionModel> conflictingQ = questionRepository.findConflictingQuestion(question);
      if (conflictingQ.isPresent()
          && !conflictingQ.get().getQuestionDefinition().getName().equals(question.getName())) {
        throw new RuntimeException(
            String.format(
                "Question key %s (Admin ID \"%s\" with non-letter characters removed and spaces"
                    + " transformed to underscores) is already in use by question %s. Please change"
                    + " the Admin ID so it either matches the existing one, or compiles to a"
                    + " different question key.",
                question.getQuestionNameKey(),
                question.getName(),
                conflictingQ.get().getQuestionDefinition().getName()));
      }
      questionKeysToNames.merge(
          question.getQuestionNameKey(),
          ImmutableList.of(question.getName()),
          (ImmutableList<String> a, ImmutableList<String> b) -> {
            ImmutableList.Builder<String> builder = ImmutableList.builder();
            builder.addAll(a);
            builder.addAll(b);
            return builder.build();
          });
    }
    ImmutableList<ImmutableList<String>> duplicateKeys =
        questionKeysToNames.values().stream()
            .filter(names -> names.size() > 1)
            .collect(ImmutableList.toImmutableList());
    if (!duplicateKeys.isEmpty()) {
      throw new RuntimeException(
          String.format(
              "Question keys (Admin IDs with non-letter characters removed and spaces transformed"
                  + " to underscores) must be unique. Duplicate question keys found: %s",
              duplicateKeys));
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
   * <p>TODO: #9628 - Make private once usage in {@link AdminImportController} is cleaned up
   */
  public ImmutableMap<String, QuestionDefinition> maybeOverwriteQuestionName(
      ImmutableList<QuestionDefinition> questions) {
    return overwriteDuplicateNames(questions, ImmutableList.of(), true);
  }

  /**
   * Creates a new (unique) admin name for the specified questions.
   *
   * @param questions all the questions in the program
   * @param duplicates the names of questions that are already in the db
   * @return a map of old_question_name -> updated_question_data
   */
  private ImmutableMap<String, QuestionDefinition> overwriteDuplicateNames(
      ImmutableList<QuestionDefinition> questions, ImmutableList<String> duplicates) {
    return overwriteDuplicateNames(questions, duplicates, false);
  }

  private ImmutableMap<String, QuestionDefinition> overwriteDuplicateNames(
      ImmutableList<QuestionDefinition> questions,
      ImmutableList<String> duplicates,
      boolean checkAll) {
    // If checkAll is true, then the admin has not manually specified which questions to create
    // duplicate names for, and we should check every single question to figure out if it's a
    // duplicate.
    // If the admin has specified some questions which should be created as duplicates, we add all
    // the other question names to the `newNamesSoFar` list right off the bat so that the
    // duplicate-suffixing logic can't accidentally choose the same name as one of those other
    // questions. See the saveImportedProgram_duplicateHandlingEnabled_newAndDuplicateNamesCollide
    // test for an example.
    List<String> newNamesSoFar =
        checkAll
            ? new ArrayList<>()
            : questions.stream()
                .map(QuestionDefinition::getName)
                .filter(name -> !duplicates.contains(name))
                .collect(Collectors.toList());
    return questions.stream()
        .collect(
            ImmutableMap.toImmutableMap(
                QuestionDefinition::getName,
                question -> {
                  // If checkAll is true, then duplicate-handling is not specified by the admin, so
                  // we should check every single question to figure out if it's a duplicate.
                  // Otherwise, we can rely on the admin-specified duplicates input to tell us which
                  // questions' admin names should be overwritten.
                  boolean nameHasConflict =
                      checkAll
                          ? nameHasConflict(question.getName(), newNamesSoFar)
                          : duplicates.contains(question.getName());
                  String newAdminName =
                      nameHasConflict
                          ? generateUniqueAdminName(question.getName(), newNamesSoFar)
                          : question.getName();
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
   *
   * <p>Note: always generates a new name. Callers should ensure that the {@code adminName} already
   * exists before calling this method.
   */
  String generateUniqueAdminName(String adminName, List<String> newNamesSoFar) {
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
   * @param duplicateHandlingOptions the {@link DuplicateQuestionHandlingOption} for each question
   *     adminName
   * @return either the saved {@link ProgramModel} or a single error message
   */
  public ErrorAnd<ProgramModel, String> saveImportedProgram(
      ProgramDefinition programDefinition,
      ImmutableList<QuestionDefinition> questionDefinitions,
      ImmutableMap<String, ProgramMigrationWrapper.DuplicateQuestionHandlingOption>
          duplicateHandlingOptions) {
    ImmutableList<String> overwrittenQuestions =
        Utils.getQuestionNamesForDuplicateHandling(
            duplicateHandlingOptions,
            ProgramMigrationWrapper.DuplicateQuestionHandlingOption.OVERWRITE_EXISTING);
    ImmutableList<String> duplicatedQuestions =
        Utils.getQuestionNamesForDuplicateHandling(
            duplicateHandlingOptions,
            ProgramMigrationWrapper.DuplicateQuestionHandlingOption.CREATE_DUPLICATE);
    ImmutableList<String> reusedQuestions =
        Utils.getQuestionNamesForDuplicateHandling(
            duplicateHandlingOptions,
            ProgramMigrationWrapper.DuplicateQuestionHandlingOption.USE_EXISTING);

    // Using a transaction batch could improve performance. Ebeans batching consistently
    // misbehaves in this instance, since there are many nested transactions. So we do not enable
    // batching.
    return transactionManager.execute(
        () ->
            doSaveProgram(
                programDefinition,
                questionDefinitions,
                overwrittenQuestions,
                duplicatedQuestions,
                reusedQuestions));
  }

  private ErrorAnd<ProgramModel, String> doSaveProgram(
      ProgramDefinition programDefinition,
      ImmutableList<QuestionDefinition> questionDefinitions,
      ImmutableList<String> overwrittenQuestions,
      ImmutableList<String> duplicatedQuestions,
      ImmutableList<String> reusedQuestions) {
    ProgramDefinition updatedProgram = programDefinition;
    if (questionDefinitions != null) {
      if (overwrittenQuestions.size() > 0 && draftIsPopulated()) {
        return ErrorAnd.error(
            ImmutableSet.of(
                "Overwriting question definitions is only supported when there are no"
                    + " existing drafts. Please publish all drafts and try again."));
      }

      validateEnumeratorAndRepeatedQuestions(
          questionDefinitions, overwrittenQuestions, duplicatedQuestions, reusedQuestions);
      // When admins can select how to handle duplicate questions, we do not show admins a
      // de-duped/suffixed admin name before saving the imported program. We must calculate that
      // name at this point.
      questionDefinitions =
          ImmutableList.copyOf(
              overwriteDuplicateNames(questionDefinitions, duplicatedQuestions).values());
      ImmutableMap<Long, QuestionDefinition> questionsOnJsonById =
          questionDefinitions.stream()
              .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getId, qd -> qd));

      // Get the questions that will be reused
      ImmutableList<QuestionDefinition> reusedQuestionDefinitions =
          questionDefinitions.stream()
              .filter(q -> reusedQuestions.contains(q.getName()))
              .collect(ImmutableList.toImmutableList());
      // Need IDs of currently saved Qs
      ImmutableMap<String, QuestionDefinition> updatedQuestionsMap =
          updateEnumeratorIdsAndSaveQuestions(
              // Only save questions that are not reused from the question bank
              questionDefinitions.stream()
                  .filter(q -> !reusedQuestions.contains(q.getName()))
                  .collect(ImmutableList.toImmutableList()),
              reusedQuestionDefinitions,
              questionsOnJsonById);

      ImmutableList<BlockDefinition> updatedBlockDefinitions =
          Utils.updateBlockDefinitions(programDefinition, questionsOnJsonById, updatedQuestionsMap);

      updatedProgram =
          programDefinition.toBuilder().setBlockDefinitions(updatedBlockDefinitions).build();
    }

    ProgramModel savedProgram =
        programRepository.insertProgramSync(
            new ProgramModel(updatedProgram, versionRepository.getDraftVersionOrCreate()));

    addProgramsToDraft(overwrittenQuestions);
    // TODO(#8613) migrate application statuses for the program
    applicationStatusesRepository.createOrUpdateStatusDefinitions(
        updatedProgram.adminName(), new StatusDefinitions());

    return ErrorAnd.of(savedProgram);
  }

  @VisibleForTesting
  void validateEnumeratorAndRepeatedQuestions(
      ImmutableList<QuestionDefinition> questions,
      ImmutableList<String> overwrittenQuestions,
      ImmutableList<String> duplicatedQuestions,
      ImmutableList<String> reusedQuestions) {
    ImmutableMap<Long, ImmutableList<QuestionDefinition>> repeatedQsByEnumeratorId =
        ImmutableMap.copyOf(
            questions.stream()
                .filter(QuestionDefinition::isRepeated)
                .collect(
                    Collectors.groupingBy(
                        question -> question.getEnumeratorId().get(),
                        ImmutableList.toImmutableList())));
    // Ensure that if an enumerator is duplicated its repeated questions are not overwritten/reused
    for (QuestionDefinition question : questions) {
      if (question.isEnumerator() && duplicatedQuestions.contains(question.getName())) {
        ImmutableList<QuestionDefinition> repeatedQs =
            repeatedQsByEnumeratorId.get(question.getId());
        for (QuestionDefinition repeatedQuestion : repeatedQs) {
          if (overwrittenQuestions.contains(repeatedQuestion.getName())
              || reusedQuestions.contains(repeatedQuestion.getName())) {
            throw new IllegalArgumentException(
                String.format(
                    "Cannot overwrite/reuse repeated question %s because enumerator %s is"
                        + " duplicated",
                    repeatedQuestion.getName(), question.getName()));
          }
        }
      }
    }
  }

  private boolean draftIsPopulated() {
    Optional<VersionModel> draftVersion = versionRepository.getDraftVersion();
    return draftVersion.isPresent()
        && (versionRepository.getProgramCountForVersion(draftVersion.get())
                + versionRepository.getQuestionCountForVersion(draftVersion.get())
            > 0);
  }

  /**
   * Save the specified questions and then update enumerator child questions with the correct ids of
   * their newly saved/reused parent questions.
   *
   * @param questionsToWrite questions that should be written to the repository, either because they
   *     have a new adminName or updated configuration
   * @param questionsToReuseFromBank full question definitions that are being reused from the bank
   * @param questionsOnJsonById a map of question IDs to the question definitions from the JSON
   * @return a map of question names to the fully updated question definitions
   */
  @VisibleForTesting
  ImmutableMap<String, QuestionDefinition> updateEnumeratorIdsAndSaveQuestions(
      ImmutableList<QuestionDefinition> questionsToWrite,
      ImmutableList<QuestionDefinition> questionsToReuseFromBank,
      ImmutableMap<Long, QuestionDefinition> questionsOnJsonById) {

    // Save all the questions
    ImmutableMap<String, QuestionDefinition> newlySavedQuestions =
        questionRepository.bulkCreateQuestions(questionsToWrite);

    // Get the question IDs of the questions we are reusing from the question bank
    ImmutableMap<String, QuestionDefinition> reusedQuestions =
        questionRepository.getExistingQuestions(
            questionsToReuseFromBank.stream()
                .map(QuestionDefinition::getName)
                .collect(ImmutableSet.toImmutableSet()));

    // Store all relevant questions in one map of name -> definition
    ImmutableMap<String, QuestionDefinition> allQuestionsByName =
        ImmutableMap.<String, QuestionDefinition>builder()
            .putAll(reusedQuestions)
            .putAll(newlySavedQuestions)
            .build();

    ImmutableMap<String, QuestionDefinition> fullyUpdatedQuestions =
        newlySavedQuestions.values().stream()
            .map(
                question -> {
                  if (question.getEnumeratorId().isPresent()) {
                    // The child question was saved with the incorrect enumerator id so we need to
                    // update it
                    Long oldEnumeratorId = question.getEnumeratorId().get();
                    // Use the old enumerator id to get the admin name of the parent question off
                    // the old question map
                    String parentQuestionAdminName =
                        questionsOnJsonById.get(oldEnumeratorId).getName();
                    // Use the admin name to get the updated id for the parent question off the new
                    // question map
                    Long newlySavedParentQuestionId =
                        allQuestionsByName.get(parentQuestionAdminName).getId();
                    // Update the child question with the correct id and save the question
                    question =
                        questionRepository.updateEnumeratorId(question, newlySavedParentQuestionId);
                    question =
                        questionRepository.createOrUpdateDraft(question).getQuestionDefinition();
                  }
                  return question;
                })
            .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getName, qd -> qd));
    return ImmutableMap.<String, QuestionDefinition>builder()
        .putAll(reusedQuestions)
        .putAll(fullyUpdatedQuestions)
        .build();
  }

  /** Adds only the relevant programs to the draft version. */
  @VisibleForTesting
  void addProgramsToDraft(ImmutableList<String> overwrittenAdminNames) {
    // If the admin has elected to overwrite some question definition(s), then we put only the
    // programs that use that question(s) into the new draft.
    ActiveAndDraftQuestions allQuestions =
        questionService.getReadOnlyQuestionServiceSync().getActiveAndDraftQuestions();
    ImmutableList<ProgramDefinition> referencingPrograms =
        overwrittenAdminNames.stream()
            .map(name -> allQuestions.getReferencingPrograms(name))
            .flatMap(references -> references.activeReferences().stream())
            .distinct()
            .collect(ImmutableList.toImmutableList());
    for (ProgramDefinition referencingProgram : referencingPrograms) {
      logger.info(
          "Creating draft for program {} (ID: {}) since it references the overwritten"
              + " question(s)",
          referencingProgram.adminName(),
          referencingProgram.id());
      programRepository.createOrUpdateDraft(referencingProgram);
    }
  }
}
