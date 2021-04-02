package views.questiontypes;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.google.common.collect.ImmutableMap;
import j2html.tags.Tag;
import java.util.Locale;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import repository.WithPostgresContainer;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ApplicantQuestion;
import services.question.TextQuestionDefinition;
import services.question.TextQuestionDefinition.TextValidationPredicates;

public class TextQuestionRendererTest extends WithPostgresContainer {
  private static final TextQuestionDefinition TEXT_QUESTION_DEFINITION =
      new TextQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path"),
          "description",
          LifecycleStage.ACTIVE,
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"),
          TextValidationPredicates.create(2, 3));

  private final ApplicantData applicantData = new ApplicantData();

  private TextQuestionRenderer renderer;

  @Before
  public void setUp() {
    ApplicantQuestion question = new ApplicantQuestion(TEXT_QUESTION_DEFINITION, applicantData);
    renderer = new TextQuestionRenderer(question);
  }

  @Test
  public void render_withoutQuestionErrors() {
    Tag result = renderer.render();

    assertThat(result.render()).doesNotContain("This answer must be");
  }

  @Test
  public void render_withMinLengthError() {
    applicantData.putString(TEXT_QUESTION_DEFINITION.getTextPath(), "a");

    Tag result = renderer.render();

    assertThat(result.render()).contains("This answer must be at least 2 characters long.");
  }

  @Test
  public void render_withMaxLengthError() {
    applicantData.putString(TEXT_QUESTION_DEFINITION.getTextPath(), "abcd");

    Tag result = renderer.render();

    assertThat(result.render()).contains("This answer must be at most 3 characters long.");
  }
}
