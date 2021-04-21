package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.testing.EqualsTester;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.Path;
import services.applicant.ApplicantData;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.AddressQuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;
import services.question.types.TextQuestionDefinition;

@RunWith(JUnitParamsRunner.class)
public class ApplicantQuestionTest {

  @Test
  @Parameters(source = QuestionType.class)
  public void errorsPresenterExtendedForAllTypes(QuestionType type)
      throws UnsupportedQuestionTypeException {
    QuestionDefinitionBuilder builder = QuestionDefinitionBuilder.sample(type);
    ApplicantQuestion question = new ApplicantQuestion(builder.build(), new ApplicantData());

    assertThat(question.errorsPresenter().hasTypeSpecificErrors()).isFalse();
  }

  @Test
  public void getsExpectedQuestionType() throws UnsupportedQuestionTypeException {
    ApplicantQuestion addressApplicantQuestion =
        new ApplicantQuestion(
            QuestionDefinitionBuilder.sample(QuestionType.ADDRESS).build(), new ApplicantData());
    assertThat(addressApplicantQuestion.createAddressQuestion())
        .isInstanceOf(AddressQuestion.class);

    ApplicantQuestion checkboxApplicantQuestion =
        new ApplicantQuestion(
            QuestionDefinitionBuilder.sample(QuestionType.CHECKBOX).build(), new ApplicantData());
    assertThat(checkboxApplicantQuestion.createMultiSelectQuestion())
        .isInstanceOf(MultiSelectQuestion.class);

    ApplicantQuestion nameApplicantQuestion =
        new ApplicantQuestion(
            QuestionDefinitionBuilder.sample(QuestionType.NAME).build(), new ApplicantData());
    assertThat(nameApplicantQuestion.createNameQuestion()).isInstanceOf(NameQuestion.class);

    ApplicantQuestion numberApplicantQuestion =
        new ApplicantQuestion(
            QuestionDefinitionBuilder.sample(QuestionType.NUMBER).build(), new ApplicantData());
    assertThat(numberApplicantQuestion.createNumberQuestion()).isInstanceOf(NumberQuestion.class);

    ApplicantQuestion radioApplicantQuestion =
        new ApplicantQuestion(
            QuestionDefinitionBuilder.sample(QuestionType.RADIO_BUTTON).build(),
            new ApplicantData());
    assertThat(radioApplicantQuestion.createSingleSelectQuestion())
        .isInstanceOf(SingleSelectQuestion.class);

    ApplicantQuestion singleSelectApplicantQuestion =
        new ApplicantQuestion(
            QuestionDefinitionBuilder.sample(QuestionType.DROPDOWN).build(), new ApplicantData());
    assertThat(singleSelectApplicantQuestion.createSingleSelectQuestion())
        .isInstanceOf(SingleSelectQuestion.class);

    ApplicantQuestion textApplicantQuestion =
        new ApplicantQuestion(
            QuestionDefinitionBuilder.sample(QuestionType.TEXT).build(), new ApplicantData());
    assertThat(textApplicantQuestion.createTextQuestion()).isInstanceOf(TextQuestion.class);
  }

  @Test
  public void equals() throws UnsupportedQuestionTypeException {
    AddressQuestionDefinition addressQuestionDefinition =
        (AddressQuestionDefinition) QuestionDefinitionBuilder.sample(QuestionType.ADDRESS).build();
    TextQuestionDefinition textQuestionDefinition =
        (TextQuestionDefinition) QuestionDefinitionBuilder.sample(QuestionType.TEXT).build();
    ApplicantData dataWithAnswers = new ApplicantData();
    dataWithAnswers.putString(Path.create("applicant.color"), "blue");

    new EqualsTester()
        .addEqualityGroup(
            new ApplicantQuestion(addressQuestionDefinition, new ApplicantData()),
            new ApplicantQuestion(addressQuestionDefinition, new ApplicantData()))
        .addEqualityGroup(
            new ApplicantQuestion(textQuestionDefinition, new ApplicantData()),
            new ApplicantQuestion(textQuestionDefinition, new ApplicantData()))
        .addEqualityGroup(
            new ApplicantQuestion(textQuestionDefinition, dataWithAnswers),
            new ApplicantQuestion(textQuestionDefinition, dataWithAnswers))
        .testEquals();
  }
}
