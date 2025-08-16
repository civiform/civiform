package services.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import repository.ResetPostgres;
import services.CiviFormError;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import services.question.QuestionOption;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.QuestionType;
import support.ProgramBuilder;

public final class QuestionValidationUtilsTest extends ResetPostgres {
  private static final QuestionDefinitionConfig QUESTION_CONFIG =
      QuestionDefinitionConfig.builder()
          .setName("applicant ice cream")
          .setDescription("Select your favorite ice cream flavor")
          .setQuestionText(LocalizedStrings.of(Locale.US, "Ice cream?"))
          .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help"))
          .build();
  private static final QuestionDefinition NAME_QUESTION = createQuestion("name", 1L);
  private static final QuestionDefinition AGE_QUESTION = createQuestion("age", 2L);
  private static final QuestionDefinition REPEATED_NAME_QUESTION =
      createQuestionWithEnumerator("repeated_name", 3L, Optional.of(1L));
  private static final String PROGRAM_NAME_1 = "Program 1";

  @Test
  public void validateQuestionOptionAdminNames_validMultiOption_noErrors() {
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(
                1L, "chocolate_admin", LocalizedStrings.of(Locale.US, "chocolate")),
            QuestionOption.create(
                2L, "strawberry_admin", LocalizedStrings.of(Locale.US, "strawberry")),
            QuestionOption.create(3L, "vanilla_admin", LocalizedStrings.of(Locale.US, "vanilla")));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateQuestionOptionAdminNames(
            ImmutableList.of(
                new MultiOptionQuestionDefinition(
                    QUESTION_CONFIG, questionOptions, MultiOptionQuestionType.CHECKBOX)));

    assertThat(errors).isEmpty();
  }

  @Test
  public void validateQuestionOptionAdminNames_duplicateAdminNames_returnsError()
      throws UnsupportedQuestionTypeException {
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(
                1L, "chocolate_admin", LocalizedStrings.of(Locale.US, "chocolate")),
            QuestionOption.create(
                2L, "chocolate_admin", LocalizedStrings.of(Locale.US, "strawberry")),
            QuestionOption.create(3L, "vanilla_admin", LocalizedStrings.of(Locale.US, "vanilla")));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateQuestionOptionAdminNames(
            ImmutableList.of(
                new MultiOptionQuestionDefinition(
                    QUESTION_CONFIG, questionOptions, MultiOptionQuestionType.CHECKBOX)));

    assertThat(errors).hasSize(1);
    assertThat(errors.iterator().next().message())
        .contains("Multi-option question admin names must be unique");
  }

  @Test
  public void validateAllProgramQuestionsPresent_allPresent_noErrors() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram(PROGRAM_NAME_1)
            .withProgramType(ProgramType.DEFAULT)
            .withBlock()
            .withOptionalQuestion(NAME_QUESTION)
            .withRequiredQuestionDefinition(AGE_QUESTION)
            .buildDefinition();
    ImmutableList<QuestionDefinition> questions = ImmutableList.of(NAME_QUESTION, AGE_QUESTION);

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateAllProgramQuestionsPresent(programDefinition, questions);

    assertThat(errors).isEmpty();
  }

  @Test
  public void validateAllProgramQuestionsPresent_questionMissing_returnsError() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram(PROGRAM_NAME_1)
            .withProgramType(ProgramType.DEFAULT)
            .withBlock()
            .withOptionalQuestion(NAME_QUESTION)
            .withRequiredQuestionDefinition(AGE_QUESTION)
            .withRequiredQuestionDefinition(createQuestion("phone", 3L))
            .buildDefinition();
    ImmutableList<QuestionDefinition> questions = ImmutableList.of(NAME_QUESTION);

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateAllProgramQuestionsPresent(programDefinition, questions);

    assertThat(errors).hasSize(2); // age, phone
    assertThat(errors.stream().map(CiviFormError::message))
        .contains(
            "Question ID " + AGE_QUESTION.getId() + " is not defined",
            "Question ID 3 is not defined");
  }

  @Test
  public void validateRepeatedQuestions_validRepeatedQuestion_noErrors() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram(PROGRAM_NAME_1)
            .withProgramType(ProgramType.DEFAULT)
            .withBlock()
            .withOptionalQuestion(NAME_QUESTION)
            .withRequiredQuestionDefinition(REPEATED_NAME_QUESTION)
            .buildDefinition();
    ImmutableList<QuestionDefinition> questions =
        ImmutableList.of(NAME_QUESTION, REPEATED_NAME_QUESTION);

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateRepeatedQuestions(
            programDefinition,
            questions,
            ImmutableList.of(NAME_QUESTION.getName(), REPEATED_NAME_QUESTION.getName()));

    assertThat(errors).isEmpty();
  }

  @Test
  public void validateRepeatedQuestions_enumeratorMissingFromQuestionDefinitions_returnsError() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram(PROGRAM_NAME_1)
            .withProgramType(ProgramType.DEFAULT)
            .withBlock()
            .withOptionalQuestion(NAME_QUESTION)
            .withRequiredQuestionDefinition(REPEATED_NAME_QUESTION)
            .buildDefinition();
    ImmutableList<QuestionDefinition> questions = ImmutableList.of(REPEATED_NAME_QUESTION);

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateRepeatedQuestions(
            programDefinition, questions, ImmutableList.of());

    assertThat(errors).hasSize(1);
    assertThat(errors.iterator().next().message())
        .contains("Some repeated questions reference enumerators that could not be found");
    assertThat(errors.iterator().next().message()).contains(REPEATED_NAME_QUESTION.getName());
  }

  @Test
  public void validateRepeatedQuestions_enumeratorMissingFromProgram_returnsError() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram(PROGRAM_NAME_1)
            .withProgramType(ProgramType.DEFAULT)
            .withBlock()
            .withRequiredQuestionDefinition(REPEATED_NAME_QUESTION)
            .buildDefinition();
    ImmutableList<QuestionDefinition> questions =
        ImmutableList.of(NAME_QUESTION, REPEATED_NAME_QUESTION);

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateRepeatedQuestions(
            programDefinition, questions, ImmutableList.of(NAME_QUESTION.getName()));

    assertThat(errors).hasSize(1);
    assertThat(errors.iterator().next().message())
        .contains("Some repeated questions reference enumerators that could not be found");
    assertThat(errors.iterator().next().message()).contains(REPEATED_NAME_QUESTION.getName());
  }

  @Test
  public void validateRepeatedQuestions_repeatedExistsInBank_enumeratorNot_returnsError() {
    ProgramDefinition programDefinition =
        ProgramBuilder.newActiveProgram(PROGRAM_NAME_1)
            .withProgramType(ProgramType.DEFAULT)
            .withBlock()
            .withOptionalQuestion(NAME_QUESTION)
            .withRequiredQuestionDefinition(REPEATED_NAME_QUESTION)
            .buildDefinition();
    ImmutableList<QuestionDefinition> questions =
        ImmutableList.of(NAME_QUESTION, REPEATED_NAME_QUESTION);

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateRepeatedQuestions(
            programDefinition, questions, ImmutableList.of(REPEATED_NAME_QUESTION.getName()));

    assertThat(errors).hasSize(1);
    assertThat(errors.iterator().next().message())
        .contains(
            "The following repeated questions already exist in the question bank, and must"
                + " reference an enumerator that also already exists");
    assertThat(errors.iterator().next().message()).contains(REPEATED_NAME_QUESTION.getName());
  }

  @Test
  public void validateYesNoQuestions_allValidOptions_noErrors() {
    QuestionDefinition yesNoQuestion =
        createYesNoQuestionWithOptions(
            "valid-yes-no-question", ImmutableList.of("yes", "no", "maybe", "not-sure"));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(ImmutableList.of(yesNoQuestion));

    assertThat(errors).isEmpty();
  }

  @Test
  public void validateYesNoQuestions_minimalValidOptions_noErrors() {
    QuestionDefinition yesNoQuestion =
        createYesNoQuestionWithOptions("minimal-yes-no-question", ImmutableList.of("yes", "no"));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(ImmutableList.of(yesNoQuestion));

    assertThat(errors).isEmpty();
  }

  @Test
  public void validateYesNoQuestions_invalidOption_returnsError() {
    QuestionDefinition yesNoQuestion =
        createYesNoQuestionWithOptions(
            "invalid-yes-no-question", ImmutableList.of("yes", "no", "absolutely"));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(ImmutableList.of(yesNoQuestion));

    assertThat(errors).hasSize(1);
    assertThat(errors.iterator().next().message())
        .contains(
            "Yes/No question 'invalid-yes-no-question' contains unsupported option: 'absolutely'. "
                + "Only 'yes', 'no', 'maybe', and 'not-sure' options are allowed.");
  }

  @Test
  public void validateYesNoQuestions_multipleInvalidOptions_returnsFirstError() {
    QuestionDefinition yesNoQuestion =
        createYesNoQuestionWithOptions(
            "invalid-yes-no-question", ImmutableList.of("yes", "absolutely", "definitely-not"));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(ImmutableList.of(yesNoQuestion));

    assertThat(errors).hasSize(1);
    assertThat(errors.iterator().next().message())
        .contains(
            "Yes/No question 'invalid-yes-no-question' contains unsupported option: 'absolutely'. "
                + "Only 'yes', 'no', 'maybe', and 'not-sure' options are allowed.");
  }

  @Test
  public void validateYesNoQuestions_mixedQuestionTypes_onlyValidatesYesNo() {
    QuestionDefinition textQuestion = createQuestion("text-question", 1L);
    QuestionDefinition dropdownQuestion =
        createDropdownQuestionWithOptions(
            "dropdown-question", ImmutableList.of("custom-option-1", "custom-option-2"));
    QuestionDefinition validYesNoQuestion =
        createYesNoQuestionWithOptions("valid-yes-no-question", ImmutableList.of("yes", "no"));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(
            ImmutableList.of(textQuestion, dropdownQuestion, validYesNoQuestion));

    assertThat(errors).isEmpty();
  }

  @Test
  public void validateYesNoQuestions_mixedValidAndInvalidYesNo_returnsFirstInvalidError() {
    QuestionDefinition validYesNoQuestion =
        createYesNoQuestionWithOptions("valid-yes-no-question", ImmutableList.of("yes", "no"));
    QuestionDefinition invalidYesNoQuestion =
        createYesNoQuestionWithOptions(
            "invalid-yes-no-question", ImmutableList.of("yes", "no", "perhaps"));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(
            ImmutableList.of(validYesNoQuestion, invalidYesNoQuestion));

    assertThat(errors).hasSize(1);
    assertThat(errors.iterator().next().message())
        .contains(
            "Yes/No question 'invalid-yes-no-question' contains unsupported option: 'perhaps'. "
                + "Only 'yes', 'no', 'maybe', and 'not-sure' options are allowed.");
  }

  @Test
  public void validateYesNoQuestions_emptyQuestionList_noErrors() {
    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(ImmutableList.of());

    assertThat(errors).isEmpty();
  }

  @Test
  public void validateYesNoQuestions_noYesNoQuestions_noErrors() {
    QuestionDefinition textQuestion = createQuestion("text-question", 1L);
    QuestionDefinition dropdownQuestion =
        createDropdownQuestionWithOptions(
            "dropdown-question", ImmutableList.of("option-1", "option-2"));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(
            ImmutableList.of(textQuestion, dropdownQuestion));

    assertThat(errors).isEmpty();
  }

  @Test
  public void validateYesNoQuestions_caseExactMatch_validatesCorrectly() {
    QuestionDefinition yesNoQuestion =
        createYesNoQuestionWithOptions("case-sensitive-question", ImmutableList.of("Yes", "No"));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(ImmutableList.of(yesNoQuestion));

    assertThat(errors).hasSize(1);
    assertThat(errors.iterator().next().message())
        .contains(
            "Yes/No question 'case-sensitive-question' contains unsupported option: 'Yes'. "
                + "Only 'yes', 'no', 'maybe', and 'not-sure' options are allowed.");
  }

  // Helper methods for YES/NO question
  private QuestionDefinition createYesNoQuestionWithOptions(
      String name, ImmutableList<String> optionNames) {
    return createMultiOptionQuestionWithOptions(name, QuestionType.YES_NO, optionNames);
  }

  private QuestionDefinition createDropdownQuestionWithOptions(
      String name, ImmutableList<String> optionNames) {
    return createMultiOptionQuestionWithOptions(name, QuestionType.DROPDOWN, optionNames);
  }

  private QuestionDefinition createMultiOptionQuestionWithOptions(
      String name, QuestionType type, ImmutableList<String> optionNames) {
    ImmutableList.Builder<QuestionOption> optionsBuilder = ImmutableList.builder();
    for (int i = 0; i < optionNames.size(); i++) {
      optionsBuilder.add(
          QuestionOption.create(
              (long) i, optionNames.get(i), LocalizedStrings.of(Locale.US, optionNames.get(i))));
    }

    MultiOptionQuestionType multiOptionType =
        switch (type) {
          case YES_NO -> MultiOptionQuestionType.YES_NO;
          case DROPDOWN -> MultiOptionQuestionType.DROPDOWN;
          case RADIO_BUTTON -> MultiOptionQuestionType.RADIO_BUTTON;
          case CHECKBOX -> MultiOptionQuestionType.CHECKBOX;
          default -> throw new IllegalArgumentException("Unsupported multi-option type: " + type);
        };

    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName(name)
            .setDescription("Test " + type.name() + " question")
            .setQuestionText(LocalizedStrings.of(Locale.US, "Select an option"))
            .build();

    return new MultiOptionQuestionDefinition(config, optionsBuilder.build(), multiOptionType);
  }

  // Helper methods to create test questions
  private static QuestionDefinition createQuestion(String name, Long id) {
    return createQuestionWithEnumerator(name, id, Optional.empty());
  }

  private static QuestionDefinition createQuestionWithEnumerator(
      String name, Long id, Optional<Long> enumeratorId) {
    try {
      return new QuestionDefinitionBuilder()
          .setName(name)
          .setId(id)
          .setQuestionType(QuestionType.TEXT)
          .setQuestionText(LocalizedStrings.withDefaultValue(name))
          .setDescription(name)
          .setEnumeratorId(enumeratorId)
          .build();
    } catch (UnsupportedQuestionTypeException e) {
      throw new RuntimeException(e);
    }
  }
}
