package services.migration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
    // First we check that all repeated questions have an enumerator
    repeatedQsMissingEnumeratorsBuilder.addAll(
        repeatedQsToEnumerators.keySet().stream()
            .filter(q -> !repeatedQsToEnumerators.get(q).isPresent())
            .map(QuestionDefinition::getName)
            .collect(ImmutableSet.toImmutableSet()));
    repeatedQsMissingEnumeratorsBuilder.addAll(
        repeatedQsToEnumerators.keySet().stream()
            .filter(q -> !program.getQuestionIdsInProgram().contains(q.getEnumeratorId().get()))
            .map(QuestionDefinition::getName)
            .collect(ImmutableSet.toImmutableSet()));
    ImmutableSet<String> repeatedQsMissingEnumerators = repeatedQsMissingEnumeratorsBuilder.build();
    if (!repeatedQsMissingEnumerators.isEmpty()) {
      errors.add(
          CiviFormError.of(
              "Some repeated questions reference enumerators that could not be found in the program"
                  + " and/or question definitions: "
                  + repeatedQsMissingEnumerators.stream().collect(Collectors.joining(", "))));
    }
    // Then we check that repeated questions only exist in the question bank if their enumerator
    // does too
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
}
