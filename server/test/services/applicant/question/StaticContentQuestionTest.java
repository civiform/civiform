package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import junitparams.JUnitParamsRunner;
import models.ApplicantModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.StaticContentQuestionDefinition;

@RunWith(JUnitParamsRunner.class)
public class StaticContentQuestionTest extends ResetPostgres {

  private static final StaticContentQuestionDefinition questionDefinition =
      new StaticContentQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("name")
              .setDescription("description")
              .setQuestionText(LocalizedStrings.of(Locale.US, "Some text. Not an actual question."))
              .setQuestionHelpText(LocalizedStrings.empty())
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
  public void defaultState() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(questionDefinition, applicantData, Optional.empty());

    StaticContentQuestion question = new StaticContentQuestion(applicantQuestion);

    assertThat(question.getValidationErrors().isEmpty()).isTrue();
  }
}
