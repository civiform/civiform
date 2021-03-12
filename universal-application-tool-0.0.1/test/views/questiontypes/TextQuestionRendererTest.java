package views.questiontypes;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.google.common.collect.ImmutableMap;
import j2html.tags.Tag;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import repository.WithPostgresContainer;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ApplicantQuestion;
import services.question.TextQuestionDefinition;

public class TextQuestionRendererTest extends WithPostgresContainer {
  private static final TextQuestionDefinition TEXT_QUESTION_DEFINITION =
      new TextQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path"),
          "description",
          ImmutableMap.of(Locale.ENGLISH, "question?"),
          ImmutableMap.of(Locale.ENGLISH, "help text"));

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
    TEXT_QUESTION_DEFINITION.setMinLength(1);
    applicantData.putString(TEXT_QUESTION_DEFINITION.getTextPath(), "");

    Tag result = renderer.render();

    assertThat(result.render()).contains("This answer must be at least 1 characters long.");
  }

  @Test
  public void render_withMaxLengthError() {
    TEXT_QUESTION_DEFINITION.setMaxLength(3);
    applicantData.putString(TEXT_QUESTION_DEFINITION.getTextPath(), "abcd");

    Tag result = renderer.render();

    assertThat(result.render()).contains("This answer must be at most 3 characters long.");
  }
}
