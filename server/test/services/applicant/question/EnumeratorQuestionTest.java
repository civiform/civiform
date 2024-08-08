package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.ApplicantModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.MessageKey;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.QuestionAnswerer;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import support.TestQuestionBank;

@RunWith(JUnitParamsRunner.class)
public class EnumeratorQuestionTest extends ResetPostgres {
  private static final EnumeratorQuestionDefinition enumeratorQuestionDefinition =
      new EnumeratorQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("household members")
              .setDescription("description")
              .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
              .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
              .setId(OptionalLong.of(1))
              .setLastModifiedTime(Optional.empty())
              .build(),
          LocalizedStrings.empty());

  private static final EnumeratorQuestionDefinition enumeratorQuestionDefinitionWithLimits =
      new EnumeratorQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("household members")
              .setDescription("description")
              .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
              .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
              .setId(OptionalLong.of(1))
              .setLastModifiedTime(Optional.empty())
              .setValidationPredicates(
                  EnumeratorQuestionDefinition.EnumeratorValidationPredicates.create(2, 3))
              .build(),
          LocalizedStrings.empty());

  private ApplicantModel applicant;
  private ApplicantData applicantData;

  private static final TestQuestionBank testQuestionBank = new TestQuestionBank(false);

  @Before
  public void setUp() {
    applicant = new ApplicantModel();
    applicantData = applicant.getApplicantData();
    testQuestionBank.reset();
  }

  @Test
  public void withEmptyApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(enumeratorQuestionDefinition, applicantData, Optional.empty());

    EnumeratorQuestion enumeratorQuestion = new EnumeratorQuestion(applicantQuestion);

    assertThat(enumeratorQuestion.isAnswered()).isFalse();
    assertThat(enumeratorQuestion.getValidationErrors()).isEmpty();
  }

  @Test
  public void withApplicantData_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(enumeratorQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerEnumeratorQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        ImmutableList.of("first", "second", "third"));

    EnumeratorQuestion enumeratorQuestion = new EnumeratorQuestion(applicantQuestion);

    assertThat(enumeratorQuestion.isAnswered()).isTrue();
    assertThat(enumeratorQuestion.getEntityNames()).contains("first", "second", "third");
    assertThat(enumeratorQuestion.getValidationErrors()).isEmpty();
  }

  @Test
  @Parameters({"", " "})
  public void withBlankStrings_hasValidationErrors(String value) {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(enumeratorQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerEnumeratorQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), ImmutableList.of(value));

    EnumeratorQuestion enumeratorQuestion = new EnumeratorQuestion(applicantQuestion);

    assertThat(enumeratorQuestion.isAnswered()).isTrue();
    assertThat(enumeratorQuestion.getEntityNames()).containsExactly(value);
    assertThat(enumeratorQuestion.getValidationErrors())
        .isEqualTo(
            ImmutableMap.of(
                applicantQuestion.getContextualizedPath(),
                ImmutableSet.of(
                    ValidationErrorMessage.create(
                        MessageKey.ENUMERATOR_VALIDATION_ENTITY_REQUIRED))));
  }

  @Test
  public void withDuplicateNames_hasValidationErrors() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(enumeratorQuestionDefinition, applicantData, Optional.empty());
    QuestionAnswerer.answerEnumeratorQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        ImmutableList.of("hello", "hello"));

    EnumeratorQuestion enumeratorQuestion = new EnumeratorQuestion(applicantQuestion);

    assertThat(enumeratorQuestion.isAnswered()).isTrue();
    assertThat(enumeratorQuestion.getEntityNames()).containsExactly("hello", "hello");
    assertThat(enumeratorQuestion.getValidationErrors())
        .isEqualTo(
            ImmutableMap.of(
                applicantQuestion.getContextualizedPath(),
                ImmutableSet.of(
                    ValidationErrorMessage.create(
                        MessageKey.ENUMERATOR_VALIDATION_DUPLICATE_ENTITY_NAME))));
  }

  @Test
  public void withTooManyEntities_hasValidationError() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            enumeratorQuestionDefinitionWithLimits, applicantData, Optional.empty());
    QuestionAnswerer.answerEnumeratorQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        ImmutableList.of("one", "two", "three", "four"));

    EnumeratorQuestion enumeratorQuestion = new EnumeratorQuestion(applicantQuestion);

    assertThat(enumeratorQuestion.isAnswered()).isTrue();
    assertThat(enumeratorQuestion.getEntityNames()).containsExactly("one", "two", "three", "four");
    assertThat(enumeratorQuestion.getValidationErrors())
        .isEqualTo(
            ImmutableMap.of(
                applicantQuestion.getContextualizedPath(),
                ImmutableSet.of(
                    ValidationErrorMessage.create(
                        MessageKey.ENUMERATOR_VALIDATION_TOO_MANY_ENTITIES, 3))));
  }

  @Test
  public void withTooFewEntities_hasValidationError() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            enumeratorQuestionDefinitionWithLimits, applicantData, Optional.empty());
    QuestionAnswerer.answerEnumeratorQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), ImmutableList.of("one"));

    EnumeratorQuestion enumeratorQuestion = new EnumeratorQuestion(applicantQuestion);

    assertThat(enumeratorQuestion.isAnswered()).isTrue();
    assertThat(enumeratorQuestion.getEntityNames()).containsExactly("one");
    assertThat(enumeratorQuestion.getValidationErrors())
        .isEqualTo(
            ImmutableMap.of(
                applicantQuestion.getContextualizedPath(),
                ImmutableSet.of(
                    ValidationErrorMessage.create(
                        MessageKey.ENUMERATOR_VALIDATION_TOO_FEW_ENTITIES, 2))));
  }

  @Test
  public void withOkayNumberOfEntities_passesValidation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            enumeratorQuestionDefinitionWithLimits, applicantData, Optional.empty());
    QuestionAnswerer.answerEnumeratorQuestion(
        applicantData,
        applicantQuestion.getContextualizedPath(),
        ImmutableList.of("one", "two", "three"));

    EnumeratorQuestion enumeratorQuestion = new EnumeratorQuestion(applicantQuestion);

    assertThat(enumeratorQuestion.isAnswered()).isTrue();
    assertThat(enumeratorQuestion.getEntityNames()).containsExactly("one", "two", "three");
    assertThat(enumeratorQuestion.getValidationErrors()).isEmpty();
    assertThat(enumeratorQuestion.getValidationErrors());
  }

  @Test
  public void getMetadata_forEnumeratorQuestion() {
    ApplicantData applicantData = new ApplicantData();
    QuestionDefinition enumeratorQuestionDefinition =
        testQuestionBank.enumeratorApplicantHouseholdMembers().getQuestionDefinition();
    Path enumeratorPath =
        ApplicantData.APPLICANT_PATH.join(enumeratorQuestionDefinition.getQuestionPathSegment());
    applicantData.putLong(enumeratorPath.atIndex(0).join(Scalar.UPDATED_AT), 123L);
    applicantData.putLong(enumeratorPath.atIndex(0).join(Scalar.PROGRAM_UPDATED_IN), 5L);

    ApplicantQuestion question =
        new ApplicantQuestion(enumeratorQuestionDefinition, applicantData, Optional.empty());

    assertThat(question.getLastUpdatedTimeMetadata()).contains(123L);
    assertThat(question.getUpdatedInProgramMetadata()).contains(5L);
  }
}
