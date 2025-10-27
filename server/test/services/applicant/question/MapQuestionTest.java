package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import models.ApplicantModel;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.question.types.MapQuestionDefinition;
import services.question.types.QuestionDefinitionConfig;

public class MapQuestionTest extends ResetPostgres {
  private static final MapQuestionDefinition mapQuestionDefinition =
      new MapQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("map question")
              .setDescription("map description")
              .setQuestionText(LocalizedStrings.of(Locale.US, "What is your location?"))
              .setQuestionHelpText(LocalizedStrings.of(Locale.US, "Select a location"))
              .setId(OptionalLong.of(1))
              .setLastModifiedTime(Optional.empty())
              .build());

  private ApplicantModel applicant;
  private ApplicantData applicantData;

  @Before
  public void setUp() {
    applicant = new ApplicantModel();
    applicantData = applicant.getApplicantData();
  }

  @Test
  public void createLocationJson_withNullName_usesUnknownLocation() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(mapQuestionDefinition, applicant, applicantData, Optional.empty());
    MapQuestion mapQuestion = applicantQuestion.createMapQuestion();

    String result = mapQuestion.createLocationJson("feature-123", null);

    assertThat(result).contains("\"featureId\":\"feature-123\"");
    assertThat(result).contains("\"locationName\":\"Unknown Location\"");
  }

  @Test
  public void createLocationJson_withProvidedName_usesProvidedName() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(mapQuestionDefinition, applicant, applicantData, Optional.empty());
    MapQuestion mapQuestion = applicantQuestion.createMapQuestion();

    String result = mapQuestion.createLocationJson("feature-456", "Seattle Downtown");

    assertThat(result).contains("\"featureId\":\"feature-456\"");
    assertThat(result).contains("\"locationName\":\"Seattle Downtown\"");
  }

  @Test
  public void createLocationJson_withEmptyName_usesEmptyLocationName() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(mapQuestionDefinition, applicant, applicantData, Optional.empty());
    MapQuestion mapQuestion = applicantQuestion.createMapQuestion();

    String result = mapQuestion.createLocationJson("feature-789", "");

    assertThat(result).contains("\"featureId\":\"feature-789\"");
    // Empty string is not null, so it should use the empty string
    assertThat(result).contains("\"locationName\":\"\"");
  }
}
