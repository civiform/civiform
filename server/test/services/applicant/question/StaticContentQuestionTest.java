package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import junitparams.JUnitParamsRunner;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.question.types.StaticContentQuestionDefinition;

@RunWith(JUnitParamsRunner.class)
public class StaticContentQuestionTest extends ResetPostgres {

  private static final StaticContentQuestionDefinition questionDefinition =
      new StaticContentQuestionDefinition(
          OptionalLong.of(1),
          "name",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "Some text. Not an actual question."),
          LocalizedStrings.empty());

  private Applicant applicant;
  private ApplicantData applicantData;

  @Before
  public void setUp() {
    applicant = new Applicant();
    applicantData = applicant.getApplicantData();
  }

  @Test
  public void defaultState() {
    ApplicantQuestion applicantQuestion =
        new ApplicantQuestion(questionDefinition, applicantData, Optional.empty());

    StaticContentQuestion question = new StaticContentQuestion(applicantQuestion);

    assertThat(question.getQuestionErrors().isEmpty()).isTrue();
    assertThat(question.getAllTypeSpecificErrors().isEmpty()).isTrue();
  }
}
