package views.questiontypes;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.google.common.collect.ImmutableMap;
import j2html.tags.Tag;
import java.util.Locale;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import repository.WithPostgresContainer;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.question.types.TextQuestionDefinition;
import services.question.types.TextQuestionDefinition.TextValidationPredicates;
import support.QuestionAnswerer;

public class TextQuestionRendererTest extends WithPostgresContainer {
  private static final TextQuestionDefinition TEXT_QUESTION_DEFINITION =
      new TextQuestionDefinition(
          "question name",
          Path.create("applicant.my.path"),
          Optional.empty(),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"),
          TextValidationPredicates.create(2, 3));

  private final ApplicantData applicantData = new ApplicantData();

  private ApplicantQuestion question;
  private TextQuestionRenderer renderer;

  @Before
  public void setUp() {
    question = new ApplicantQuestion(TEXT_QUESTION_DEFINITION, applicantData, ApplicantData.APPLICANT_PATH);
    renderer = new TextQuestionRenderer(question);
  }

  @Test
  public void render_withoutQuestionErrors() {
    Tag result = renderer.render();

    assertThat(result.render()).doesNotContain("Must contain at");
  }

  @Test
  public void render_withMinLengthError() {
    QuestionAnswerer.answerTextQuestion(applicantData, question.getContextualizedPath(), "a");

    Tag result = renderer.render();

    assertThat(result.render()).contains("Must contain at least 2 characters.");
  }

  @Test
  public void render_withMaxLengthError() {
    QuestionAnswerer.answerTextQuestion(applicantData, question.getContextualizedPath(), "abcd");

    Tag result = renderer.render();

    assertThat(result.render()).contains("Must contain at most 3 characters.");
  }
}
