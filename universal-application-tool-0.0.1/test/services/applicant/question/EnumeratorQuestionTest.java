package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.Path;
import services.applicant.ApplicantData;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;
import support.QuestionAnswerer;
import support.TestQuestionBank;

@RunWith(JUnitParamsRunner.class)
public class EnumeratorQuestionTest extends ResetPostgres {
  private static final EnumeratorQuestionDefinition enumeratorQuestionDefinition =
      new EnumeratorQuestionDefinition(
          OptionalLong.of(1),
          "household members",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "question?"),
          LocalizedStrings.of(Locale.US, "help text"),
          LocalizedStrings.empty());

  private Applicant applicant;
  private ApplicantData applicantData;
  private Messages messages;

  private static final TestQuestionBank testQuestionBank = new TestQuestionBank(false);

  @Before
  public void setUp() {
    applicant = new Applicant();
    applicantData = applicant.getApplicantData();
    testQuestionBank.reset();
    messages = instanceOf(MessagesApi.class).preferred(ImmutableList.of(Lang.defaultLang()));
  }

  @Test
  public void withEmptyApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(enumeratorQuestionDefinition, applicantData, Optional.empty());

    EnumeratorQuestion enumeratorQuestion = new EnumeratorQuestion(applicantQuestion);

    assertThat(enumeratorQuestion.isAnswered()).isFalse();
    assertThat(enumeratorQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(enumeratorQuestion.hasConditionErrors()).isFalse();
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
    assertThat(enumeratorQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(enumeratorQuestion.hasConditionErrors()).isFalse();
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
    assertThat(enumeratorQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(enumeratorQuestion.hasConditionErrors()).isTrue();
    assertThat(enumeratorQuestion.getQuestionErrors()).hasSize(1);
    assertThat(enumeratorQuestion.getQuestionErrors().asList().get(0).getMessage(messages))
        .isEqualTo("Please enter a value for each line.");
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
    assertThat(enumeratorQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(enumeratorQuestion.hasConditionErrors()).isTrue();
    assertThat(enumeratorQuestion.getQuestionErrors()).hasSize(1);
    assertThat(enumeratorQuestion.getQuestionErrors().asList().get(0).getMessage(messages))
        .isEqualTo("Please enter a unique value for each line.");
  }

  @Test
  public void getMetadata_forEnumeratorQuestion() {
    ApplicantData applicantData = new ApplicantData();
    QuestionDefinition enumeratorQuestionDefinition =
        testQuestionBank.applicantHouseholdMembers().getQuestionDefinition();
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
