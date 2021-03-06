package services.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import services.question.AddressQuestionDefinition;
import services.question.NameQuestionDefinition;
import services.question.TextQuestionDefinition;

public class ApplicantQuestionTest {

  private static final TextQuestionDefinition textQuestionDefinition =
      new TextQuestionDefinition(
          1L,
          "question name",
          "applicant.my.path.name",
          "description",
          ImmutableMap.of(Locale.ENGLISH, "question?"),
          ImmutableMap.of(Locale.ENGLISH, "help text"));
  private static final NameQuestionDefinition nameQuestionDefinition =
      new NameQuestionDefinition(
          1L,
          "question name",
          "applicant.my.path.name",
          "description",
          ImmutableMap.of(Locale.ENGLISH, "question?"),
          ImmutableMap.of(Locale.ENGLISH, "help text"));
  private static final AddressQuestionDefinition addressQuestionDefinition =
      new AddressQuestionDefinition(
          1L,
          "question name",
          "applicant.my.path.name",
          "description",
          ImmutableMap.of(Locale.ENGLISH, "question?"),
          ImmutableMap.of(Locale.ENGLISH, "help text"));

  private Applicant applicant;
  private ApplicantData applicantData;

  @Before
  public void setUp() {
    applicant = new Applicant();
    applicantData = applicant.getApplicantData();
  }

  @Test
  public void textQuestion_withEmptyApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(textQuestionDefinition, applicantData);

    assertThat(applicantQuestion.getTextQuestion())
        .isInstanceOf(ApplicantQuestion.TextQuestion.class);
    assertThat(applicantQuestion.getQuestionText()).isEqualTo("question?");
    assertThat(applicantQuestion.hasErrors()).isFalse();
  }

  @Test
  public void textQuestion_withPresentApplicantData() {
    applicantData.putString(Path.create(textQuestionDefinition.getPath()), "hello");
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(textQuestionDefinition, applicantData);
    ApplicantQuestion.TextQuestion textQuestion = applicantQuestion.getTextQuestion();

    assertThat(textQuestion.hasErrors()).isFalse();
    assertThat(textQuestion.getTextValue().get()).isEqualTo("hello");
  }

  @Test
  public void nameQuestion_withEmptyApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(nameQuestionDefinition, applicantData);

    assertThat(applicantQuestion.getNameQuestion())
        .isInstanceOf(ApplicantQuestion.NameQuestion.class);
    assertThat(applicantQuestion.getQuestionText()).isEqualTo("question?");
    assertThat(applicantQuestion.hasErrors()).isFalse();
  }

  @Test
  public void nameQuestion_withInvalidApplicantData() {
    applicantData.putString(Path.create(nameQuestionDefinition.getFirstNamePath()), "");
    applicantData.putString(Path.create(nameQuestionDefinition.getLastNamePath()), "");
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(nameQuestionDefinition, applicantData);
    ApplicantQuestion.NameQuestion nameQuestion = applicantQuestion.getNameQuestion();

    assertThat(applicantQuestion.hasErrors()).isTrue();
    assertThat(nameQuestion.getFirstNameErrors())
        .contains(ValidationErrorMessage.create("First name is required."));
    assertThat(nameQuestion.getLastNameErrors())
        .contains(ValidationErrorMessage.create("Last name is required."));
  }

  @Test
  public void nameQuestion_withValidApplicantData() {
    applicantData.putString(Path.create(nameQuestionDefinition.getFirstNamePath()), "Wendel");
    applicantData.putString(Path.create(nameQuestionDefinition.getLastNamePath()), "Patrick");
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(nameQuestionDefinition, applicantData);
    ApplicantQuestion.NameQuestion nameQuestion = applicantQuestion.getNameQuestion();

    assertThat(applicantQuestion.hasErrors()).isFalse();
    assertThat(nameQuestion.getFirstNameValue().get()).isEqualTo("Wendel");
    assertThat(nameQuestion.getLastNameValue().get()).isEqualTo("Patrick");
  }

  @Test
  public void addressQuestion_withEmptyApplicantData() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(addressQuestionDefinition, applicantData);

    assertThat(applicantQuestion.getAddressQuestion())
        .isInstanceOf(ApplicantQuestion.AddressQuestion.class);
    assertThat(applicantQuestion.getQuestionText()).isEqualTo("question?");
    assertThat(applicantQuestion.hasErrors()).isFalse();
  }

  @Test
  public void addressQuestion_withInvalidApplicantData() {
    applicantData.putString(Path.create(addressQuestionDefinition.getStreetPath()), "");
    applicantData.putString(Path.create(addressQuestionDefinition.getCityPath()), "");
    applicantData.putString(Path.create(addressQuestionDefinition.getStatePath()), "");
    applicantData.putString(Path.create(addressQuestionDefinition.getZipPath()), "");

    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(addressQuestionDefinition, applicantData);
    ApplicantQuestion.AddressQuestion addressQuestion = applicantQuestion.getAddressQuestion();

    assertThat(applicantQuestion.hasErrors()).isTrue();
    assertThat(addressQuestion.getStreetErrors())
        .contains(ValidationErrorMessage.create("Street is required."));
    assertThat(addressQuestion.getCityErrors())
        .contains(ValidationErrorMessage.create("City is required."));
    assertThat(addressQuestion.getStateErrors())
        .contains(ValidationErrorMessage.create("State is required."));
    assertThat(addressQuestion.getZipErrors())
        .contains(ValidationErrorMessage.create("Zip code is required."));

    applicantData.putString(Path.create(addressQuestionDefinition.getZipPath()), "not a zip code");
    addressQuestion =
        new ApplicantQuestion(addressQuestionDefinition, applicantData).getAddressQuestion();

    assertThat(addressQuestion.getZipErrors())
        .contains(ValidationErrorMessage.create("Invalid zip code."));
  }

  @Test
  public void addressQuestion_withValidApplicantData() {
    applicantData.putString(Path.create(addressQuestionDefinition.getStreetPath()), "85 Pike St");
    applicantData.putString(Path.create(addressQuestionDefinition.getCityPath()), "Seattle");
    applicantData.putString(Path.create(addressQuestionDefinition.getStatePath()), "WA");
    applicantData.putString(Path.create(addressQuestionDefinition.getZipPath()), "98101");

    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(addressQuestionDefinition, applicantData);
    ApplicantQuestion.AddressQuestion addressQuestion = applicantQuestion.getAddressQuestion();

    assertThat(applicantQuestion.hasErrors()).isFalse();
    assertThat(addressQuestion.getStreetValue().get()).isEqualTo("85 Pike St");
    assertThat(addressQuestion.getCityValue().get()).isEqualTo("Seattle");
    assertThat(addressQuestion.getStateValue().get()).isEqualTo("WA");
    assertThat(addressQuestion.getZipValue().get()).isEqualTo("98101");
  }
}
