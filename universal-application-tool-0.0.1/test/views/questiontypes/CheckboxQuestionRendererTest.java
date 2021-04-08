package views.questiontypes;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import j2html.tags.Tag;
import java.util.Locale;
import java.util.Optional;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ApplicantQuestion;
import services.question.CheckboxQuestionDefinition;

public class CheckboxQuestionRendererTest {

  private static final CheckboxQuestionDefinition CHECKBOX_QUESTION =
      new CheckboxQuestionDefinition(
          1L,
          "question name",
          Path.create("applicant.my.path"),
          Optional.empty(),
          "description",
          LifecycleStage.ACTIVE,
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"),
          ImmutableListMultimap.of(Locale.US, "hello"));

  private final ApplicantData applicantData = new ApplicantData();

  private CheckboxQuestionRenderer renderer;

  @Before
  public void setup() {
    ApplicantQuestion question = new ApplicantQuestion(CHECKBOX_QUESTION, applicantData);
    renderer = new CheckboxQuestionRenderer(question);
  }

  @Test
  public void render_usesCorrectInputName() {
    Tag result = renderer.render();

    assertThat(result.render()).contains("applicant.my.path.selection[]");
  }
}
