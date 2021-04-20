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
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;
import support.TestQuestionBank;

@RunWith(JUnitParamsRunner.class)
public class ApplicantQuestionTest {

  private static final TestQuestionBank testQuestionBank = new TestQuestionBank(false);

  @Test
  @Parameters(source = QuestionType.class)
  public void errorsPresenterExtendedForAllTypes(QuestionType type)
      throws UnsupportedQuestionTypeException {
    QuestionDefinitionBuilder builder = QuestionDefinitionBuilder.sample(type);
    ApplicantQuestion question = new ApplicantQuestion(builder.build(), new ApplicantData());

    assertThat(question.errorsPresenter().hasTypeSpecificErrors()).isFalse();
  }

  @Test
  public void getsExpectedQuestionType() {
    ApplicantQuestion addressApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantAddress().getQuestionDefinition(), new ApplicantData());
    assertThat(addressApplicantQuestion.createAddressQuestion())
        .isInstanceOf(AddressQuestion.class);

    ApplicantQuestion checkboxApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantKitchenTools().getQuestionDefinition(), new ApplicantData());
    assertThat(checkboxApplicantQuestion.createMultiSelectQuestion())
        .isInstanceOf(MultiSelectQuestion.class);

    ApplicantQuestion dropdownApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantIceCream().getQuestionDefinition(), new ApplicantData());
    assertThat(dropdownApplicantQuestion.createSingleSelectQuestion())
        .isInstanceOf(SingleSelectQuestion.class);

    ApplicantQuestion fileUploadApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantFile().getQuestionDefinition(), new ApplicantData());
    assertThat(fileUploadApplicantQuestion.createFileUploadQuestion())
        .isInstanceOf(FileUploadQuestion.class);

    ApplicantQuestion nameApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantName().getQuestionDefinition(), new ApplicantData());
    assertThat(nameApplicantQuestion.createNameQuestion()).isInstanceOf(NameQuestion.class);

    ApplicantQuestion numberApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
            new ApplicantData());
    assertThat(numberApplicantQuestion.createNumberQuestion()).isInstanceOf(NumberQuestion.class);

    ApplicantQuestion radioApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantSeason().getQuestionDefinition(), new ApplicantData());
    assertThat(radioApplicantQuestion.createSingleSelectQuestion())
        .isInstanceOf(SingleSelectQuestion.class);

    ApplicantQuestion textApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantFavoriteColor().getQuestionDefinition(), new ApplicantData());
    assertThat(textApplicantQuestion.createTextQuestion()).isInstanceOf(TextQuestion.class);
  }

  @Test
  public void equals() {
    ApplicantData dataWithAnswers = new ApplicantData();
    dataWithAnswers.putString(Path.create("applicant.color"), "blue");

    new EqualsTester()
        .addEqualityGroup(
            // Address
            new ApplicantQuestion(
                testQuestionBank.applicantAddress().getQuestionDefinition(), new ApplicantData()),
            new ApplicantQuestion(
                testQuestionBank.applicantAddress().getQuestionDefinition(), new ApplicantData()))
        .addEqualityGroup(
            // Checkbox
            new ApplicantQuestion(
                testQuestionBank.applicantKitchenTools().getQuestionDefinition(),
                new ApplicantData()),
            new ApplicantQuestion(
                testQuestionBank.applicantKitchenTools().getQuestionDefinition(),
                new ApplicantData()))
        .addEqualityGroup(
            // Dropdown
            new ApplicantQuestion(
                testQuestionBank.applicantIceCream().getQuestionDefinition(), new ApplicantData()),
            new ApplicantQuestion(
                testQuestionBank.applicantIceCream().getQuestionDefinition(), new ApplicantData()))
        .addEqualityGroup(
            // File upload
            new ApplicantQuestion(
                testQuestionBank.applicantFile().getQuestionDefinition(), new ApplicantData()),
            new ApplicantQuestion(
                testQuestionBank.applicantFile().getQuestionDefinition(), new ApplicantData()))
        .addEqualityGroup(
            // Name
            new ApplicantQuestion(
                testQuestionBank.applicantName().getQuestionDefinition(), new ApplicantData()),
            new ApplicantQuestion(
                testQuestionBank.applicantName().getQuestionDefinition(), new ApplicantData()))
        .addEqualityGroup(
            // Number
            new ApplicantQuestion(
                testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
                new ApplicantData()),
            new ApplicantQuestion(
                testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
                new ApplicantData()))
        .addEqualityGroup(
            // Radio button
            new ApplicantQuestion(
                testQuestionBank.applicantSeason().getQuestionDefinition(), new ApplicantData()),
            new ApplicantQuestion(
                testQuestionBank.applicantSeason().getQuestionDefinition(), new ApplicantData()))
        .addEqualityGroup(
            // Text
            new ApplicantQuestion(
                testQuestionBank.applicantFavoriteColor().getQuestionDefinition(),
                new ApplicantData()),
            new ApplicantQuestion(
                testQuestionBank.applicantFavoriteColor().getQuestionDefinition(),
                new ApplicantData()))
        .addEqualityGroup(
            // Text with answered data
            new ApplicantQuestion(
                testQuestionBank.applicantFavoriteColor().getQuestionDefinition(), dataWithAnswers),
            new ApplicantQuestion(
                testQuestionBank.applicantFavoriteColor().getQuestionDefinition(), dataWithAnswers))
        .testEquals();
  }
}
