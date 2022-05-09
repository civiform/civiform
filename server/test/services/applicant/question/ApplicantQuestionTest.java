package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.testing.EqualsTester;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.RepeatedEntity;
import services.program.ProgramQuestionDefinition;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import services.question.types.ScalarType;
import support.QuestionAnswerer;
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
            Optional.empty());

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
            Optional.empty());

    assertThatThrownBy(() -> enumerationApplicantQuestion.getContextualizedScalars())
        .isInstanceOf(RuntimeException.class)
        .hasCauseInstanceOf(InvalidQuestionTypeException.class);
  }

  @Test
  @Parameters(source = QuestionType.class)
  public void errorsPresenterExtendedForAllTypes(QuestionType questionType) {
    QuestionDefinition definition =
        testQuestionBank.getSampleQuestionsForAllTypes().get(questionType).getQuestionDefinition();
    ApplicantQuestion question =
        new ApplicantQuestion(
            ProgramQuestionDefinition.create(definition, Optional.empty()).setOptional(true),
            new ApplicantData(),
            Optional.empty());

    assertThat(question.errorsPresenter().getValidationErrors().isEmpty()).isTrue();
  }

  @Test
  public void getsExpectedQuestionType() {
    ApplicantQuestion addressApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantAddress().getQuestionDefinition(),
            new ApplicantData(),
            Optional.empty());
    assertThat(addressApplicantQuestion.createAddressQuestion())
        .isInstanceOf(AddressQuestion.class);

    ApplicantQuestion checkboxApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantKitchenTools().getQuestionDefinition(),
            new ApplicantData(),
            Optional.empty());
    assertThat(checkboxApplicantQuestion.createMultiSelectQuestion())
        .isInstanceOf(MultiSelectQuestion.class);

    ApplicantQuestion dropdownApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantIceCream().getQuestionDefinition(),
            new ApplicantData(),
            Optional.empty());
    assertThat(dropdownApplicantQuestion.createSingleSelectQuestion())
        .isInstanceOf(SingleSelectQuestion.class);

    ApplicantQuestion enumeratorApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantHouseholdMembers().getQuestionDefinition(),
            new ApplicantData(),
            Optional.empty());
    assertThat(enumeratorApplicantQuestion.createEnumeratorQuestion())
        .isInstanceOf(EnumeratorQuestion.class);

    ApplicantQuestion nameApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantName().getQuestionDefinition(),
            new ApplicantData(),
            Optional.empty());
    assertThat(nameApplicantQuestion.createNameQuestion()).isInstanceOf(NameQuestion.class);

    ApplicantQuestion numberApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
            new ApplicantData(),
            Optional.empty());
    assertThat(numberApplicantQuestion.createNumberQuestion()).isInstanceOf(NumberQuestion.class);

    ApplicantQuestion radioApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantSeason().getQuestionDefinition(),
            new ApplicantData(),
            Optional.empty());
    assertThat(radioApplicantQuestion.createSingleSelectQuestion())
        .isInstanceOf(SingleSelectQuestion.class);

    ApplicantQuestion textApplicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantFavoriteColor().getQuestionDefinition(),
            new ApplicantData(),
            Optional.empty());
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
                testQuestionBank.applicantAddress().getQuestionDefinition(),
                new ApplicantData(),
                Optional.empty()),
            new ApplicantQuestion(
                testQuestionBank.applicantAddress().getQuestionDefinition(),
                new ApplicantData(),
                Optional.empty()))
        .addEqualityGroup(
            // Checkbox
            new ApplicantQuestion(
                testQuestionBank.applicantKitchenTools().getQuestionDefinition(),
                new ApplicantData(),
                Optional.empty()),
            new ApplicantQuestion(
                testQuestionBank.applicantKitchenTools().getQuestionDefinition(),
                new ApplicantData(),
                Optional.empty()))
        .addEqualityGroup(
            // Dropdown
            new ApplicantQuestion(
                testQuestionBank.applicantIceCream().getQuestionDefinition(),
                new ApplicantData(),
                Optional.empty()),
            new ApplicantQuestion(
                testQuestionBank.applicantIceCream().getQuestionDefinition(),
                new ApplicantData(),
                Optional.empty()))
        .addEqualityGroup(
            // Name
            new ApplicantQuestion(
                testQuestionBank.applicantName().getQuestionDefinition(),
                new ApplicantData(),
                Optional.empty()),
            new ApplicantQuestion(
                testQuestionBank.applicantName().getQuestionDefinition(),
                new ApplicantData(),
                Optional.empty()))
        .addEqualityGroup(
            // Number
            new ApplicantQuestion(
                testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
                new ApplicantData(),
                Optional.empty()),
            new ApplicantQuestion(
                testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
                new ApplicantData(),
                Optional.empty()))
        .addEqualityGroup(
            // Radio button
            new ApplicantQuestion(
                testQuestionBank.applicantSeason().getQuestionDefinition(),
                new ApplicantData(),
                Optional.empty()),
            new ApplicantQuestion(
                testQuestionBank.applicantSeason().getQuestionDefinition(),
                new ApplicantData(),
                Optional.empty()))
        .addEqualityGroup(
            // Text
            new ApplicantQuestion(
                testQuestionBank.applicantFavoriteColor().getQuestionDefinition(),
                new ApplicantData(),
                Optional.empty()),
            new ApplicantQuestion(
                testQuestionBank.applicantFavoriteColor().getQuestionDefinition(),
                new ApplicantData(),
                Optional.empty()))
        .addEqualityGroup(
            // Text with answered data
            new ApplicantQuestion(
                testQuestionBank.applicantFavoriteColor().getQuestionDefinition(),
                dataWithAnswers,
                Optional.empty()),
            new ApplicantQuestion(
                testQuestionBank.applicantFavoriteColor().getQuestionDefinition(),
                dataWithAnswers,
                Optional.empty()))
        .testEquals();
  }

  @Test
  public void questionTextAndHelpText_areContextualizedByRepeatedEntity() {
    ApplicantData applicantData = new ApplicantData();
    Path householdMembersPath =
        ApplicantData.APPLICANT_PATH.join(
            testQuestionBank
                .applicantHouseholdMembers()
                .getQuestionDefinition()
                .getQuestionPathSegment());
    QuestionAnswerer.answerEnumeratorQuestion(
        applicantData, householdMembersPath, ImmutableList.of("Jonathan"));
    Path householdMembersJobsPath =
        householdMembersPath
            .atIndex(0)
            .join(
                testQuestionBank
                    .applicantHouseholdMemberJobs()
                    .getQuestionDefinition()
                    .getQuestionPathSegment());
    QuestionAnswerer.answerEnumeratorQuestion(
        applicantData, householdMembersJobsPath, ImmutableList.of("JonCo"));
    RepeatedEntity jonathan =
        RepeatedEntity.createRepeatedEntities(
                (EnumeratorQuestionDefinition)
                    testQuestionBank.applicantHouseholdMembers().getQuestionDefinition(),
                Optional.empty(),
                applicantData)
            .get(0);
    RepeatedEntity jonCo =
        jonathan
            .createNestedRepeatedEntities(
                (EnumeratorQuestionDefinition)
                    testQuestionBank.applicantHouseholdMemberJobs().getQuestionDefinition(),
                Optional.empty(),
                applicantData)
            .get(0);

    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantHouseholdMemberDaysWorked().getQuestionDefinition(),
            applicantData,
            Optional.of(jonCo));

    assertThat(applicantQuestion.getQuestionText())
        .isEqualTo("How many days has Jonathan worked at JonCo?");
    assertThat(applicantQuestion.getQuestionHelpText())
        .isEqualTo("How many days has Jonathan worked at JonCo?");
  }

  @Test
  public void getMetadata_forEnumerator_withNoRepeatedEntities() {
    ApplicantData applicantData = new ApplicantData();
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(
            testQuestionBank.applicantHouseholdMembers().getQuestionDefinition(),
            applicantData,
            Optional.empty());
    QuestionAnswerer.addMetadata(
        applicantData, applicantQuestion.getContextualizedPath().withoutArrayReference(), 1L, 2L);

    assertThat(applicantQuestion.getUpdatedInProgramMetadata()).contains(1L);
    assertThat(applicantQuestion.getLastUpdatedTimeMetadata()).contains(2L);
  }

  @Test
  public void isRequiredButWasSkippedInCurrentProgram_returnsTrue() {
    ApplicantData applicantData = new ApplicantData();
    long programId = 5L;
    ProgramQuestionDefinition pqd =
        ProgramQuestionDefinition.create(
            testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
            Optional.of(programId));
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(pqd, applicantData, Optional.empty());
    QuestionAnswerer.addMetadata(
        applicantData, applicantQuestion.getContextualizedPath(), programId, 1L);

    assertThat(applicantQuestion.isRequiredButWasSkippedInCurrentProgram()).isTrue();
  }

  @Test
  public void isRequiredButWasSkippedInCurrentProgram_leftSkippedInDifferentProgram_returnsFalse() {
    ApplicantData applicantData = new ApplicantData();
    long programId = 5L;
    ProgramQuestionDefinition pqd =
        ProgramQuestionDefinition.create(
            testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
            Optional.of(programId));
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(pqd, applicantData, Optional.empty());
    QuestionAnswerer.addMetadata(
        applicantData, applicantQuestion.getContextualizedPath(), programId + 1, 1L);

    assertThat(applicantQuestion.isRequiredButWasSkippedInCurrentProgram()).isFalse();
  }

  @Test
  public void isRequiredButWasSkippedInCurrentProgram_isAnswered_returnsFalse() {
    ApplicantData applicantData = new ApplicantData();
    long programId = 5L;
    ProgramQuestionDefinition pqd =
        ProgramQuestionDefinition.create(
            testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
            Optional.of(programId));
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(pqd, applicantData, Optional.empty());
    QuestionAnswerer.answerNumberQuestion(
        applicantData, applicantQuestion.getContextualizedPath(), "5");
    QuestionAnswerer.addMetadata(
        applicantData, applicantQuestion.getContextualizedPath(), programId, 1L);

    assertThat(applicantQuestion.isRequiredButWasSkippedInCurrentProgram()).isFalse();
  }

  @Test
  public void isRequiredButWasSkippedInCurrentProgram_isOptional_returnsFalse() {
    ApplicantData applicantData = new ApplicantData();
    long programId = 5L;
    ProgramQuestionDefinition pqd =
        ProgramQuestionDefinition.create(
                testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
                Optional.of(programId))
            .setOptional(true);
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(pqd, applicantData, Optional.empty());
    QuestionAnswerer.addMetadata(
        applicantData, applicantQuestion.getContextualizedPath(), programId, 1L);

    assertThat(applicantQuestion.isRequiredButWasSkippedInCurrentProgram()).isFalse();
  }

  @Test
  public void isAnsweredOrSkippedInProgram_forSkippedOptional_isTrue() {
    ApplicantData applicantData = new ApplicantData();
    long programId = 5L;
    Path questionPath =
        ApplicantData.APPLICANT_PATH.join(
            testQuestionBank
                .applicantJugglingNumber()
                .getQuestionDefinition()
                .getQuestionPathSegment());
    QuestionAnswerer.addMetadata(applicantData, questionPath, programId, 0L);
    ProgramQuestionDefinition pqd =
        ProgramQuestionDefinition.create(
                testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
                Optional.of(programId))
            .setOptional(true);

    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(pqd, applicantData, Optional.empty());

    assertThat(applicantQuestion.isAnsweredOrSkippedOptionalInProgram()).isTrue();
  }

  @Test
  public void isAnsweredOrSkippedOptionalInProgram_forSkippedOptionalInDifferentProgram_isFalse() {
    ApplicantData applicantData = new ApplicantData();
    long programId = 5L;
    Path questionPath =
        ApplicantData.APPLICANT_PATH.join(
            testQuestionBank
                .applicantJugglingNumber()
                .getQuestionDefinition()
                .getQuestionPathSegment());
    QuestionAnswerer.addMetadata(applicantData, questionPath, programId + 1, 0L);
    ProgramQuestionDefinition pqd =
        ProgramQuestionDefinition.create(
                testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
                Optional.of(programId))
            .setOptional(true);

    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(pqd, applicantData, Optional.empty());

    assertThat(applicantQuestion.isAnsweredOrSkippedOptionalInProgram()).isFalse();
  }

  @Test
  public void isAnsweredOrSkippedOptionalInProgram_forRequiredSkipped_isFalse() {
    ApplicantData applicantData = new ApplicantData();
    long programId = 5L;
    Path questionPath =
        ApplicantData.APPLICANT_PATH.join(
            testQuestionBank
                .applicantJugglingNumber()
                .getQuestionDefinition()
                .getQuestionPathSegment());
    QuestionAnswerer.addMetadata(applicantData, questionPath, programId, 0L);
    ProgramQuestionDefinition pqd =
        ProgramQuestionDefinition.create(
            testQuestionBank.applicantJugglingNumber().getQuestionDefinition(),
            Optional.of(programId));

    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(pqd, applicantData, Optional.empty());

    assertThat(applicantQuestion.isAnsweredOrSkippedOptionalInProgram()).isFalse();
  }
}
