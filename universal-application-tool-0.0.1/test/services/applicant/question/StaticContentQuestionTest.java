package services.applicant.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import repository.WithPostgresContainer;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.question.types.StaticContentQuestionDefinition;
import support.QuestionAnswerer;

@RunWith(JUnitParamsRunner.class)
public class StaticContentQuestionTest extends WithPostgresContainer {

  private static final StaticContentQuestionDefinition questionDefinition =
    new StaticContentQuestionDefinition(
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

    assertThat(question.hasQuestionErrors()).isFalse();
    assertThat(question.hasTypeSpecificErrors()).isFalse();
  }

}
