package services.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static support.TestQuestionBank.createDropdownQuestionDefinition;
import static support.TestQuestionBank.createQuestionDefinition;
import static support.TestQuestionBank.createYesNoQuestionDefinition;

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
import services.question.types.QuestionDefinitionConfig;
import services.question.types.QuestionType;
import support.ProgramBuilder;

public final class QuestionValidationUtilsTest extends ResetPostgres {
  // Test configuration for multi-option questions
  private static final QuestionDefinitionConfig QUESTION_CONFIG =
      QuestionDefinitionConfig.builder()
          .setName("applicant ice cream")
          .setDescription("Select your favorite ice cream flavor")
          .setQuestionText(LocalizedStrings.of(Locale.US, "Ice cream?"))
          .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help"))
          .build();

  // Use TestQuestionBank static methods for creating test questions
  private static final QuestionDefinition NAME_QUESTION =
      createQuestionDefinition("name", 1L, QuestionType.TEXT, Optional.empty());
  private static final QuestionDefinition AGE_QUESTION =
      createQuestionDefinition("age", 2L, QuestionType.TEXT, Optional.empty());
  private static final QuestionDefinition REPEATED_NAME_QUESTION =
      createQuestionDefinition("repeated_name", 3L, QuestionType.TEXT, Optional.of(1L));
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
            .withRequiredQuestionDefinition(
                createQuestionDefinition("phone", 3L, QuestionType.TEXT, Optional.empty()))
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

  // YES/NO validation tests
  @Test
  public void validateYesNoQuestions_allValidOptions_noErrors() {
    QuestionDefinition yesNoQuestion =
        createYesNoQuestionDefinition(
            "valid-yes-no-question", 1L, ImmutableList.of("yes", "no", "maybe", "not-sure"));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(ImmutableList.of(yesNoQuestion));

    assertThat(errors).isEmpty();
  }

  @Test
  public void validateYesNoQuestions_minimalValidOptions_noErrors() {
    QuestionDefinition yesNoQuestion =
        createYesNoQuestionDefinition("minimal-yes-no-question", 1L, ImmutableList.of("yes", "no"));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(ImmutableList.of(yesNoQuestion));

    assertThat(errors).isEmpty();
  }

  @Test
  public void validateYesNoQuestions_invalidOption_returnsError() {
    QuestionDefinition yesNoQuestion =
        createYesNoQuestionDefinition(
            "invalid-yes-no-question", 1L, ImmutableList.of("yes", "no", "absolutely"));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(ImmutableList.of(yesNoQuestion));

    assertThat(errors).hasSize(1);
    assertThat(errors.iterator().next().message())
        .contains(
            "YES_NO question 'invalid-yes-no-question' contains invalid option 'absolutely'. "
                + "Only 'yes', 'no', 'maybe', and 'not-sure' options are allowed.");
  }

  @Test
  public void validateYesNoQuestions_multipleInvalidOptions_returnsAllErrors() {
    QuestionDefinition yesNoQuestion =
        createYesNoQuestionDefinition(
            "invalid-yes-no-question", 1L, ImmutableList.of("yes", "absolutely", "definitely-not"));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(ImmutableList.of(yesNoQuestion));

    assertThat(errors).hasSize(3);
    assertThat(errors.stream().map(CiviFormError::message))
        .contains(
            "YES_NO question 'invalid-yes-no-question' contains invalid option 'absolutely'. Only"
                + " 'yes', 'no', 'maybe', and 'not-sure' options are allowed.",
            "YES_NO question 'invalid-yes-no-question' contains invalid option 'definitely-not'."
                + " Only 'yes', 'no', 'maybe', and 'not-sure' options are allowed.",
            "YES_NO question 'invalid-yes-no-question' is missing required 'no' option.");
  }

  @Test
  public void validateYesNoQuestions_mixedQuestionTypes_onlyValidatesYesNo() {
    QuestionDefinition textQuestion =
        createQuestionDefinition("text-question", 1L, QuestionType.TEXT, Optional.empty());
    QuestionDefinition dropdownQuestion =
        createDropdownQuestionDefinition(
            "dropdown-question", 2L, ImmutableList.of("custom-option-1", "custom-option-2"));
    QuestionDefinition validYesNoQuestion =
        createYesNoQuestionDefinition("valid-yes-no-question", 3L, ImmutableList.of("yes", "no"));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(
            ImmutableList.of(textQuestion, dropdownQuestion, validYesNoQuestion));

    assertThat(errors).isEmpty();
  }

  @Test
  public void validateYesNoQuestions_mixedValidAndInvalidYesNo_returnsInvalidError() {
    QuestionDefinition validYesNoQuestion =
        createYesNoQuestionDefinition("valid-yes-no-question", 1L, ImmutableList.of("yes", "no"));
    QuestionDefinition invalidYesNoQuestion =
        createYesNoQuestionDefinition(
            "invalid-yes-no-question", 2L, ImmutableList.of("yes", "no", "perhaps"));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(
            ImmutableList.of(validYesNoQuestion, invalidYesNoQuestion));

    assertThat(errors).hasSize(1);
    assertThat(errors.iterator().next().message())
        .contains(
            "YES_NO question 'invalid-yes-no-question' contains invalid option 'perhaps'. "
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
    QuestionDefinition textQuestion =
        createQuestionDefinition("text-question", 1L, QuestionType.TEXT, Optional.empty());
    QuestionDefinition dropdownQuestion =
        createDropdownQuestionDefinition(
            "dropdown-question", 2L, ImmutableList.of("option-1", "option-2"));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(
            ImmutableList.of(textQuestion, dropdownQuestion));

    assertThat(errors).isEmpty();
  }

  @Test
  public void validateYesNoQuestions_caseExactMatch_validatesCorrectly() {
    QuestionDefinition yesNoQuestion =
        createYesNoQuestionDefinition("case-sensitive-question", 1L, ImmutableList.of("Yes", "No"));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(ImmutableList.of(yesNoQuestion));

    assertThat(errors).hasSize(4);
    assertThat(errors.stream().map(CiviFormError::message))
        .contains(
            "YES_NO question 'case-sensitive-question' contains invalid option 'Yes'. Only 'yes',"
                + " 'no', 'maybe', and 'not-sure' options are allowed.",
            "YES_NO question 'case-sensitive-question' contains invalid option 'No'. Only 'yes',"
                + " 'no', 'maybe', and 'not-sure' options are allowed.",
            "YES_NO question 'case-sensitive-question' is missing required 'yes' option.",
            "YES_NO question 'case-sensitive-question' is missing required 'no' option.");
  }

  @Test
  public void validateYesNoQuestions_missingYesOption_returnsError() {
    QuestionDefinition yesNoQuestion =
        createYesNoQuestionDefinition("missing-yes-question", 1L, ImmutableList.of("no", "maybe"));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(ImmutableList.of(yesNoQuestion));

    assertThat(errors).hasSize(1);
    assertThat(errors.stream().map(CiviFormError::message))
        .contains("YES_NO question 'missing-yes-question' is missing required 'yes' option.");
  }

  @Test
  public void validateYesNoQuestions_missingNoOption_returnsError() {
    QuestionDefinition yesNoQuestion =
        createYesNoQuestionDefinition(
            "missing-no-question", 1L, ImmutableList.of("yes", "not-sure"));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(ImmutableList.of(yesNoQuestion));

    assertThat(errors).hasSize(1);
    assertThat(errors.stream().map(CiviFormError::message))
        .contains("YES_NO question 'missing-no-question' is missing required 'no' option.");
  }

  @Test
  public void validateYesNoQuestions_missingBothRequiredOptions_returnsBothErrors() {
    QuestionDefinition yesNoQuestion =
        createYesNoQuestionDefinition(
            "missing-both-required-question", 1L, ImmutableList.of("maybe", "not-sure"));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(ImmutableList.of(yesNoQuestion));

    assertThat(errors).hasSize(2);
    assertThat(errors.stream().map(CiviFormError::message))
        .contains(
            "YES_NO question 'missing-both-required-question' is missing required 'yes' option.",
            "YES_NO question 'missing-both-required-question' is missing required 'no' option.");
  }

  @Test
  public void validateYesNoQuestions_hasRequiredOptionsAndInvalidOnes_returnsOnlyInvalidErrors() {
    QuestionDefinition yesNoQuestion =
        createYesNoQuestionDefinition(
            "valid-required-invalid-extra-question",
            1L,
            ImmutableList.of("yes", "no", "invalid-option"));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(ImmutableList.of(yesNoQuestion));

    assertThat(errors).hasSize(1);
    assertThat(errors.stream().map(CiviFormError::message))
        .contains(
            "YES_NO question 'valid-required-invalid-extra-question' contains invalid option"
                + " 'invalid-option'. Only 'yes', 'no', 'maybe', and 'not-sure' options are"
                + " allowed.");
  }

  @Test
  public void validateYesNoQuestions_missingRequiredAndHasInvalid_returnsAllErrors() {
    QuestionDefinition yesNoQuestion =
        createYesNoQuestionDefinition(
            "missing-and-invalid-question", 1L, ImmutableList.of("yes", "invalid-option"));

    ImmutableSet<CiviFormError> errors =
        QuestionValidationUtils.validateYesNoQuestions(ImmutableList.of(yesNoQuestion));

    assertThat(errors).hasSize(2);
    assertThat(errors.stream().map(CiviFormError::message))
        .contains(
            "YES_NO question 'missing-and-invalid-question' contains invalid option"
                + " 'invalid-option'. Only 'yes', 'no', 'maybe', and 'not-sure' options are"
                + " allowed.",
            "YES_NO question 'missing-and-invalid-question' is missing required 'no' option.");
  }
}
