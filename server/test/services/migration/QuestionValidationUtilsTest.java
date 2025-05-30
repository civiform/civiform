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
