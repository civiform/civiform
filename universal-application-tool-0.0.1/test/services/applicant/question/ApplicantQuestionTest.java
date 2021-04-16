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
import support.TestQuestionDefinitionBank;

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
        new ApplicantQuestion(TestQuestionDefinitionBank.address(), new ApplicantData());
    assertThat(addressApplicantQuestion.createAddressQuestion())
        .isInstanceOf(AddressQuestion.class);

    ApplicantQuestion checkboxApplicantQuestion =
        new ApplicantQuestion(TestQuestionDefinitionBank.checkbox(), new ApplicantData());
    assertThat(checkboxApplicantQuestion.createMultiSelectQuestion())
        .isInstanceOf(MultiSelectQuestion.class);

    ApplicantQuestion dropdownApplicantQuestion =
            new ApplicantQuestion(TestQuestionDefinitionBank.dropdown(), new ApplicantData());
    assertThat(dropdownApplicantQuestion.createSingleSelectQuestion())
            .isInstanceOf(SingleSelectQuestion.class);

    ApplicantQuestion nameApplicantQuestion =
        new ApplicantQuestion(TestQuestionDefinitionBank.name(), new ApplicantData());
    assertThat(nameApplicantQuestion.createNameQuestion()).isInstanceOf(NameQuestion.class);

    ApplicantQuestion numberApplicantQuestion =
        new ApplicantQuestion(TestQuestionDefinitionBank.number(), new ApplicantData());
    assertThat(numberApplicantQuestion.createNumberQuestion()).isInstanceOf(NumberQuestion.class);

    ApplicantQuestion radioApplicantQuestion =
        new ApplicantQuestion(TestQuestionDefinitionBank.radioButton(), new ApplicantData());
    assertThat(radioApplicantQuestion.createSingleSelectQuestion())
        .isInstanceOf(SingleSelectQuestion.class);

    ApplicantQuestion textApplicantQuestion =
        new ApplicantQuestion(TestQuestionDefinitionBank.text(), new ApplicantData());
    assertThat(textApplicantQuestion.createTextQuestion()).isInstanceOf(TextQuestion.class);
  }

  @Test
  public void equals() {
    ApplicantData dataWithAnswers = new ApplicantData();
    dataWithAnswers.putString(Path.create("applicant.color"), "blue");

    new EqualsTester()
        .addEqualityGroup(
            new ApplicantQuestion(TestQuestionDefinitionBank.address(), new ApplicantData()),
            new ApplicantQuestion(TestQuestionDefinitionBank.address(), new ApplicantData()))
        .addEqualityGroup(
            new ApplicantQuestion(TestQuestionDefinitionBank.text(), new ApplicantData()),
            new ApplicantQuestion(TestQuestionDefinitionBank.text(), new ApplicantData()))
        .addEqualityGroup(
            new ApplicantQuestion(TestQuestionDefinitionBank.text(), dataWithAnswers),
            new ApplicantQuestion(TestQuestionDefinitionBank.text(), dataWithAnswers))
        .testEquals();
  }
}
