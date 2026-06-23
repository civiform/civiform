package services.migration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import services.CiviFormError;
import services.program.ProgramDefinition;
import services.question.YesNoQuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;

/** Utility class to validate questions during the program import flow. */
final class QuestionValidationUtils {

  /**
   * Validates attributes of the question, including admin name, help text, and question options.
   */
  static ImmutableSet<CiviFormError> validateQuestionOptionAdminNames(
      ImmutableList<QuestionDefinition> questions) {
    return questions.stream()
        .map(
            question -> {
              if (question.getQuestionType().isMultiOptionType()) {
                MultiOptionQuestionDefinition multiOptionQuestion =
                    (MultiOptionQuestionDefinition) question;
                return multiOptionQuestion.setValidateQuestionOptionAdminNames(false).validate();
              }
              return question.validate();
            })
        .flatMap(errors -> errors.stream())
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Ensures all questions contained in the program are defined with a {@link QuestionDefinition}.
   */
  static ImmutableSet<CiviFormError> validateAllProgramQuestionsPresent(
      ProgramDefinition program, ImmutableList<QuestionDefinition> questions) {
    ImmutableList<Long> questionDefinitionIds =
        questions.stream().map(QuestionDefinition::getId).collect(ImmutableList.toImmutableList());
    return program.getQuestionIdsInProgram().stream()
        .filter(qid -> !questionDefinitionIds.contains(qid))
        .map(qid -> CiviFormError.of("Question ID " + qid + " is not defined"))
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Validates YES_NO questions and returns errors for any that contain invalid options.
   *
   * <p>Only validates YES_NO question types. Other question types are ignored. Valid YES_NO options
   * are: 'yes', 'no', 'maybe', and 'not-sure'.
   *
   * @param questions the list of questions to validate
   * @return a set of validation errors for YES_NO questions with invalid options, empty if all
   *     YES_NO questions have valid options
   */
  static ImmutableSet<CiviFormError> validateYesNoQuestions(
      ImmutableList<QuestionDefinition> questions) {

    return questions.stream()
        .filter(question -> question.getQuestionType() == QuestionType.YES_NO)
        .map(question -> (MultiOptionQuestionDefinition) question)
        .flatMap(yesNoQuestion -> validateSingleYesNoQuestion(yesNoQuestion))
        .collect(ImmutableSet.toImmutableSet());
  }

  private static Stream<CiviFormError> validateSingleYesNoQuestion(
      MultiOptionQuestionDefinition yesNoQuestion) {

    ImmutableSet<String> optionAdminNames =
        yesNoQuestion.getOptions().stream()
            .map(option -> option.adminName())
            .collect(ImmutableSet.toImmutableSet());

    Stream<CiviFormError> invalidOptionErrors =
        optionAdminNames.stream()
            .filter(optionName -> !YesNoQuestionOption.getAllAdminNames().contains(optionName))
            .map(
                optionName ->
                    CiviFormError.of(
                        String.format(
                            "YES_NO question '%s' contains invalid option '%s'. "
                                + "Only 'yes', 'no', 'maybe', and 'not-sure' options are allowed.",
                            yesNoQuestion.getName(), optionName)));

    Stream<CiviFormError> missingRequiredErrors =
        YesNoQuestionOption.getRequiredAdminNames().stream()
            .filter(requiredOption -> !optionAdminNames.contains(requiredOption))
            .map(
                missingOption ->
                    CiviFormError.of(
                        String.format(
                            "YES_NO question '%s' is missing required '%s' option.",
                            yesNoQuestion.getName(), missingOption)));

    return Stream.concat(invalidOptionErrors, missingRequiredErrors);
  }

  /**
   * Ensures that repeated questions (1) each have an associated enumerator, and (2) only exists in
   * the question bank if the associated enumerator also already exists in the question bank.
   *
   * <p>Note: This in effect only validates old-flow enums for backwards compatibility, however
   * these checks are also necessary and complementary for the new-flow, such as ensuring the
   * enumerator is in the program.
   */
  static ImmutableSet<CiviFormError> validateRepeatedQuestions(
      ProgramDefinition program,
      ImmutableList<QuestionDefinition> questions,
      ImmutableList<String> existingAdminNames) {
    ImmutableMap<QuestionDefinition, Optional<QuestionDefinition>> repeatedQsToEnumerators =
        questions.stream()
            .filter(q -> q.getEnumeratorId().isPresent())
            .collect(
                ImmutableMap.toImmutableMap(
                    q -> q,
                    q ->
                        questions.stream()
                            .filter(parentEnum -> parentEnum.getId() == q.getEnumeratorId().get())
                            .findFirst()));
    ImmutableSet.Builder<CiviFormError> errors = ImmutableSet.builder();
    ImmutableSet.Builder<String> repeatedQsMissingEnumeratorsBuilder = ImmutableSet.builder();

    // Check that all repeated questions have an enumerator set.
    repeatedQsMissingEnumeratorsBuilder.addAll(
        repeatedQsToEnumerators.entrySet().stream()
            .filter(e -> e.getValue().isEmpty())
            .map(Map.Entry::getKey)
            .map(QuestionDefinition::getName)
            .collect(ImmutableSet.toImmutableSet()));
    // Check that the enumerator IDs are present in the program.
    HashSet<Long> programQuestionIds = new HashSet<>(program.getQuestionIdsInProgram());
    repeatedQsMissingEnumeratorsBuilder.addAll(
        repeatedQsToEnumerators.keySet().stream()
            .filter(q -> !programQuestionIds.contains(q.getEnumeratorId().get()))
            .map(QuestionDefinition::getName)
            .collect(ImmutableSet.toImmutableSet()));
    ImmutableSet<String> repeatedQsMissingEnumerators = repeatedQsMissingEnumeratorsBuilder.build();
    if (!repeatedQsMissingEnumerators.isEmpty()) {
      errors.add(
          CiviFormError.of(
              "Some repeated questions reference enumerators that could not be found in the program"
                  + " and/or question definitions: "
                  + String.join(", ", repeatedQsMissingEnumerators)));
    }

    // Check that repeated questions only exist in the question bank if their enumerator
    // does too.
    ImmutableList<QuestionDefinition> repeatedQsAlreadyExistWithoutExistingEnumerators =
        repeatedQsToEnumerators.keySet().stream()
            .filter(
                q ->
                    existingAdminNames.contains(q.getName())
                        && !repeatedQsMissingEnumerators.contains(q.getName()))
            .filter(
                q -> !existingAdminNames.contains(repeatedQsToEnumerators.get(q).get().getName()))
            .collect(ImmutableList.toImmutableList());
    if (!repeatedQsAlreadyExistWithoutExistingEnumerators.isEmpty()) {
      errors.add(
          CiviFormError.of(
              "The following repeated questions already exist in the question bank, and must"
                  + " reference an enumerator that also already exists: "
                  + repeatedQsAlreadyExistWithoutExistingEnumerators.stream()
                      .map(QuestionDefinition::getName)
                      .collect(Collectors.joining(", "))));
    }

    return errors.build();
  }

  /**
   * Validates that new-flow enumerators (circa Q3 2026) are correctly configured.
   *
   * <p>New-flow enumerators are identified by enumerators with {@code enumeratorInitialQuestionId}
   * set.
   *
   * <p>Validation:
   *
   * <ul>
   *   <li>1. The question identified by {@code enumeratorInitialQuestionId} has an {@code
   *       enumeratorId} set to the enumerator's ID.
   *   <li>2. The enumerator and initial question must both be only present newly in the import or
   *       pre-existing in the question bank, they can't be mixed between the two as it would
   *       change the semantics of which ever is in the question bank and that may break existing
   *       uses.
   * </ul>
   */
  static ImmutableSet<CiviFormError> validateNewFlowEnumerators(
      ImmutableList<QuestionDefinition> questions, ImmutableList<String> existingAdminNames) {
    ImmutableMap<Long, QuestionDefinition> questionsById =
        questions.stream().collect(ImmutableMap.toImmutableMap(QuestionDefinition::getId, q -> q));
    ImmutableSet.Builder<CiviFormError> errors = ImmutableSet.builder();

    for (QuestionDefinition question : questions) {
      Optional<Long> maybeInitialQuestionId = question.getEnumeratorInitialQuestionId();

      // Find new-flow enums by the presence of an initial question.
      if (maybeInitialQuestionId.isEmpty()) {
        continue;
      }

      // Only enumerators can have an initial question.
      Long initialQuestionId = maybeInitialQuestionId.get();
      if (!question.isEnumerator()) {
        errors.add(
            CiviFormError.of(
                """
                Question '%s' has an enumeratorInitialQuestionId but is not an enumerator \
                question.\
                """
                    .formatted(question.getName())));
        continue;
      }

      // The initial question must be present.
      Optional<QuestionDefinition> maybeInitialQuestion =
          Optional.ofNullable(questionsById.get(initialQuestionId));
      if (maybeInitialQuestion.isEmpty()) {
        errors.add(
            CiviFormError.of(
                """
                Enumerator question '%s' references an enumeratorInitialQuestionId %d that is \
                not in the import.\
                """
                    .formatted(question.getName(), initialQuestionId)));
        continue;
      }

      // The initial question can not be an enumerator.
      QuestionDefinition initialQuestion = maybeInitialQuestion.get();
      if (initialQuestion.isEnumerator()) {
        errors.add(
            CiviFormError.of(
                """
                Enumerator question '%s' references question '%s' as its initial question, \
                but that question is itself an enumerator.\
                """
                    .formatted(question.getName(), initialQuestion.getName())));
        continue;
      }

      // The initial question must reference the enumerator.
      Long newEnumQuestionId = question.getId();
      if (initialQuestion.getEnumeratorId().filter(newEnumQuestionId::equals).isEmpty()) {
        errors.add(
            CiviFormError.of(
                """
                Enumerator question '%s' references question '%s' as its initial question, \
                but that question does not reference it back as its enumeratorId.\
                """
                    .formatted(question.getName(), initialQuestion.getName())));
      }

      // The enumerator and initial question must both be in the question
      // bank or both in the import.
      boolean enumInQB = existingAdminNames.contains(question.getName());
      boolean initialQInQB = existingAdminNames.contains(initialQuestion.getName());
      if (enumInQB != initialQInQB) {
        errors.add(
            CiviFormError.of(
                """
                Enumerator question '%s' (%s) and its initial question '%s' (%s) are in \
                different data sources and must be in the same.\
                """
                    .formatted(
                        question.getName(),
                        enumInQB ? "question bank" : "import",
                        initialQuestion.getName(),
                        initialQInQB ? "question bank" : "import")));
      }
    }
    return errors.build();
  }
}
