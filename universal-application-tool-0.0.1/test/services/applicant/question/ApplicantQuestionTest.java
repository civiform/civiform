package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.testing.EqualsTester;
import java.util.EnumSet;
import java.util.Locale;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.AddressQuestionDefinition;
import services.question.DropdownQuestionDefinition;
import services.question.NameQuestionDefinition;
import services.question.NumberQuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.QuestionType;
import services.question.TextQuestionDefinition;
import services.question.UnsupportedQuestionTypeException;

@RunWith(JUnitParamsRunner.class)
public class ApplicantQuestionTest {

  private static final DropdownQuestionDefinition dropdownQuestionDefinition =
      new DropdownQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path.name"),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"),
          ImmutableListMultimap.of(
              Locale.US,
              "option 1",
              Locale.US,
              "option 2",
              Locale.FRANCE,
              "un",
              Locale.FRANCE,
              "deux"));
  private static final TextQuestionDefinition textQuestionDefinition =
      new TextQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path.name"),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"));
  private static final NameQuestionDefinition nameQuestionDefinition =
      new NameQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path.name"),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"));
  private static final NumberQuestionDefinition numberQuestionDefinition =
      new NumberQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path.name"),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"));
  private static final AddressQuestionDefinition addressQuestionDefinition =
      new AddressQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path.name"),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"));

  private Applicant applicant;
  private ApplicantData applicantData;

  @Before
  public void setUp() {
    applicant = new Applicant();
    applicantData = applicant.getApplicantData();
  }

  // TODO(https://github.com/seattle-uat/civiform/issues/405): Change this to just use
  // @Parameters(source = QuestionType.class) once RepeatedQuestionDefinition exists.
  @Test
  @Parameters(method = "types")
  public void errorsPresenterExtendedForAllTypes(QuestionType type)
      throws UnsupportedQuestionTypeException {
    QuestionDefinitionBuilder builder = QuestionDefinitionBuilder.sample(type);
    ApplicantQuestion question = new ApplicantQuestion(builder.build(), new ApplicantData());

    assertThat(question.errorsPresenter().hasTypeSpecificErrors()).isFalse();
  }

  private EnumSet<QuestionType> types() {
    return EnumSet.complementOf(EnumSet.of(QuestionType.REPEATER));
  }

  @Test
  public void singleSelectQuestion_withEmptyApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(dropdownQuestionDefinition, applicantData);

    assertThat(applicantQuestion.getSingleSelectQuestion())
        .isInstanceOf(SingleSelectQuestion.class);
    assertThat(applicantQuestion.getSingleSelectQuestion().getOptions())
        .containsOnly("option 1", "option 2");
    assertThat(applicantQuestion.hasErrors()).isFalse();
  }

  @Test
  public void singleSelectQuestion_withPresentApplicantData() {
    applicantData.putString(dropdownQuestionDefinition.getSelectionPath(), "answer");
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(dropdownQuestionDefinition, applicantData);
    SingleSelectQuestion singleSelectQuestion = applicantQuestion.getSingleSelectQuestion();

    assertThat(singleSelectQuestion.hasTypeSpecificErrors()).isFalse();
    assertThat(singleSelectQuestion.getSelectedOptionValue()).hasValue("answer");
  }

  // TODO(https://github.com/seattle-uat/civiform/issues/416): Add a test for validation failures.

  @Test
  public void addressQuestion_withEmptyApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(addressQuestionDefinition, applicantData);

    assertThat(applicantQuestion.getAddressQuestion()).isInstanceOf(AddressQuestion.class);
    assertThat(applicantQuestion.getQuestionText()).isEqualTo("question?");
    assertThat(applicantQuestion.hasErrors()).isFalse();
  }

  @Test
  public void addressQuestion_withInvalidApplicantData() {
    applicantData.putString(addressQuestionDefinition.getStreetPath(), "");
    applicantData.putString(addressQuestionDefinition.getCityPath(), "");
    applicantData.putString(addressQuestionDefinition.getStatePath(), "");
    applicantData.putString(addressQuestionDefinition.getZipPath(), "");

    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(addressQuestionDefinition, applicantData);
    AddressQuestion addressQuestion = applicantQuestion.getAddressQuestion();

    assertThat(applicantQuestion.hasErrors()).isTrue();
    assertThat(addressQuestion.getStreetErrors())
        .contains(ValidationErrorMessage.create("Street is required."));
    assertThat(addressQuestion.getCityErrors())
        .contains(ValidationErrorMessage.create("City is required."));
    assertThat(addressQuestion.getStateErrors())
        .contains(ValidationErrorMessage.create("State is required."));
    assertThat(addressQuestion.getZipErrors())
        .contains(ValidationErrorMessage.create("Zip code is required."));

    applicantData.putString(addressQuestionDefinition.getZipPath(), "not a zip code");
    addressQuestion =
        new ApplicantQuestion(addressQuestionDefinition, applicantData).getAddressQuestion();

    assertThat(addressQuestion.getZipErrors())
        .contains(ValidationErrorMessage.create("Invalid zip code."));
  }

  @Test
  public void addressQuestion_withValidApplicantData() {
    applicantData.putString(addressQuestionDefinition.getStreetPath(), "85 Pike St");
    applicantData.putString(addressQuestionDefinition.getCityPath(), "Seattle");
    applicantData.putString(addressQuestionDefinition.getStatePath(), "WA");
    applicantData.putString(addressQuestionDefinition.getZipPath(), "98101");

    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(addressQuestionDefinition, applicantData);
    AddressQuestion addressQuestion = applicantQuestion.getAddressQuestion();

    assertThat(applicantQuestion.hasErrors()).isFalse();
    assertThat(addressQuestion.getStreetValue().get()).isEqualTo("85 Pike St");
    assertThat(addressQuestion.getCityValue().get()).isEqualTo("Seattle");
    assertThat(addressQuestion.getStateValue().get()).isEqualTo("WA");
    assertThat(addressQuestion.getZipValue().get()).isEqualTo("98101");
  }

  @Test
  public void equals() {
    ApplicantData dataWithAnswers = new ApplicantData();
    dataWithAnswers.putString(Path.create("applicant.color"), "blue");

    new EqualsTester()
        .addEqualityGroup(
            new ApplicantQuestion(addressQuestionDefinition, new ApplicantData()),
            new ApplicantQuestion(addressQuestionDefinition, new ApplicantData()))
        .addEqualityGroup(
            new ApplicantQuestion(dropdownQuestionDefinition, new ApplicantData()),
            new ApplicantQuestion(dropdownQuestionDefinition, new ApplicantData()))
        .addEqualityGroup(
            new ApplicantQuestion(nameQuestionDefinition, new ApplicantData()),
            new ApplicantQuestion(nameQuestionDefinition, new ApplicantData()))
        .addEqualityGroup(
            new ApplicantQuestion(numberQuestionDefinition, new ApplicantData()),
            new ApplicantQuestion(numberQuestionDefinition, new ApplicantData()))
        .addEqualityGroup(
            new ApplicantQuestion(textQuestionDefinition, new ApplicantData()),
            new ApplicantQuestion(textQuestionDefinition, new ApplicantData()))
        .addEqualityGroup(
            new ApplicantQuestion(textQuestionDefinition, dataWithAnswers),
            new ApplicantQuestion(textQuestionDefinition, dataWithAnswers))
        .testEquals();
  }
}
