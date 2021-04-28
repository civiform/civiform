package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.testing.EqualsTester;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import services.Path;
import services.applicant.ApplicantData;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.question.types.ScalarType;
import support.TestQuestionBank;

@RunWith(JUnitParamsRunner.class)
public class ApplicantQuestionTest {

  private static final TestQuestionBank testQuestionBank = new TestQuestionBank(false);

  @Test
  public void getContextualizedScalars_returnsContextualizedScalarsAndMetadataForType() {
    ApplicantQuestion testApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantFavoriteColor().getQuestionDefinition(),
            new ApplicantData(),
            ApplicantData.APPLICANT_PATH);

    ImmutableMap<Path, ScalarType> expected =
        ImmutableMap.of(
            Path.create("applicant.applicant_favorite_color").join(Scalar.TEXT),
            ScalarType.STRING,
            Path.create("applicant.applicant_favorite_color").join(Scalar.UPDATED_AT),
            ScalarType.LONG,
            Path.create("applicant.applicant_favorite_color").join(Scalar.PROGRAM_UPDATED_IN),
            ScalarType.LONG);

    assertThat(testApplicantQuestion.getContextualizedScalars().entrySet())
        .containsExactlyElementsOf(expected.entrySet());
  }

  @Test
  public void getContextualizedScalars_forEnumerationQuestion_throwsInvalidQuestionTypeException() {
    ApplicantQuestion enumerationApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantHouseholdMembers().getQuestionDefinition(),
            new ApplicantData(),
            ApplicantData.APPLICANT_PATH);

    assertThatThrownBy(() -> enumerationApplicantQuestion.getContextualizedScalars())
        .isInstanceOf(RuntimeException.class)
        .hasCauseInstanceOf(InvalidQuestionTypeException.class);
  }

  @Test
  @Parameters(source = QuestionType.class)
  public void errorsPresenterExtendedForAllTypes(QuestionType questionType) {
    QuestionDefinition definition =
        testQuestionBank.getSampleQuestionsForAllTypes().get(questionType).getQuestionDefinition();
    ApplicantQuestion question = new ApplicantQuestion(definition, new ApplicantData(), ApplicantData.APPLICANT_PATH);

    assertThat(question.errorsPresenter().hasTypeSpecificErrors()).isFalse();
  }

  @Test
  public void getsExpectedQuestionType() {
    ApplicantQuestion addressApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantAddress().getQuestionDefinition(), new ApplicantData(), ApplicantData.APPLICANT_PATH);
    assertThat(addressApplicantQuestion.createAddressQuestion())
        .isInstanceOf(AddressQuestion.class);

    ApplicantQuestion checkboxApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantKitchenTools().getQuestionDefinition(), new ApplicantData(), ApplicantData.APPLICANT_PATH);
    assertThat(checkboxApplicantQuestion.createMultiSelectQuestion())
        .isInstanceOf(MultiSelectQuestion.class);

    ApplicantQuestion dropdownApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantIceCream().getQuestionDefinition(), new ApplicantData(), ApplicantData.APPLICANT_PATH);
    assertThat(dropdownApplicantQuestion.createSingleSelectQuestion())
        .isInstanceOf(SingleSelectQuestion.class);

    ApplicantQuestion nameApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantName().getQuestionDefinition(), new ApplicantData(), ApplicantData.APPLICANT_PATH);
    assertThat(nameApplicantQuestion.createNameQuestion()).isInstanceOf(NameQuestion.class);

    ApplicantQuestion numberApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
            new ApplicantData(), ApplicantData.APPLICANT_PATH);
    assertThat(numberApplicantQuestion.createNumberQuestion()).isInstanceOf(NumberQuestion.class);

    ApplicantQuestion radioApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantSeason().getQuestionDefinition(), new ApplicantData(), ApplicantData.APPLICANT_PATH);
    assertThat(radioApplicantQuestion.createSingleSelectQuestion())
        .isInstanceOf(SingleSelectQuestion.class);

    ApplicantQuestion textApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantFavoriteColor().getQuestionDefinition(), new ApplicantData(), ApplicantData.APPLICANT_PATH);
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
                testQuestionBank.applicantAddress().getQuestionDefinition(), new ApplicantData(), ApplicantData.APPLICANT_PATH),
            new ApplicantQuestion(
                testQuestionBank.applicantAddress().getQuestionDefinition(), new ApplicantData(), ApplicantData.APPLICANT_PATH))
        .addEqualityGroup(
            // Checkbox
            new ApplicantQuestion(
                testQuestionBank.applicantKitchenTools().getQuestionDefinition(),
                new ApplicantData(), ApplicantData.APPLICANT_PATH),
            new ApplicantQuestion(
                testQuestionBank.applicantKitchenTools().getQuestionDefinition(),
                new ApplicantData(), ApplicantData.APPLICANT_PATH))
        .addEqualityGroup(
            // Dropdown
            new ApplicantQuestion(
                testQuestionBank.applicantIceCream().getQuestionDefinition(), new ApplicantData(), ApplicantData.APPLICANT_PATH),
            new ApplicantQuestion(
                testQuestionBank.applicantIceCream().getQuestionDefinition(), new ApplicantData(), ApplicantData.APPLICANT_PATH))
        .addEqualityGroup(
            // Name
            new ApplicantQuestion(
                testQuestionBank.applicantName().getQuestionDefinition(), new ApplicantData(), ApplicantData.APPLICANT_PATH),
            new ApplicantQuestion(
                testQuestionBank.applicantName().getQuestionDefinition(), new ApplicantData(), ApplicantData.APPLICANT_PATH))
        .addEqualityGroup(
            // Number
            new ApplicantQuestion(
                testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
                new ApplicantData(), ApplicantData.APPLICANT_PATH),
            new ApplicantQuestion(
                testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
                new ApplicantData(), ApplicantData.APPLICANT_PATH))
        .addEqualityGroup(
            // Radio button
            new ApplicantQuestion(
                testQuestionBank.applicantSeason().getQuestionDefinition(), new ApplicantData(), ApplicantData.APPLICANT_PATH),
            new ApplicantQuestion(
                testQuestionBank.applicantSeason().getQuestionDefinition(), new ApplicantData(), ApplicantData.APPLICANT_PATH))
        .addEqualityGroup(
            // Text
            new ApplicantQuestion(
                testQuestionBank.applicantFavoriteColor().getQuestionDefinition(),
                new ApplicantData(), ApplicantData.APPLICANT_PATH),
            new ApplicantQuestion(
                testQuestionBank.applicantFavoriteColor().getQuestionDefinition(),
                new ApplicantData(), ApplicantData.APPLICANT_PATH))
        .addEqualityGroup(
            // Text with answered data
            new ApplicantQuestion(
                testQuestionBank.applicantFavoriteColor().getQuestionDefinition(), dataWithAnswers, ApplicantData.APPLICANT_PATH),
            new ApplicantQuestion(
                testQuestionBank.applicantFavoriteColor().getQuestionDefinition(), dataWithAnswers, ApplicantData.APPLICANT_PATH))
        .testEquals();
  }
}
