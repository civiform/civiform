package views.questiontypes;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import j2html.tags.Tag;
import java.util.Locale;
import java.util.Optional;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.question.QuestionOption;
import services.question.types.RadioButtonQuestionDefinition;

public class RadioButtonQuestionRendererTest {

  private static final RadioButtonQuestionDefinition QUESTION =
      new RadioButtonQuestionDefinition(
          1L,
          "name",
          Path.create("applicant.favorite_ice_cream"),
          Optional.empty(),
          "description",
          LifecycleStage.ACTIVE,
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"),
          ImmutableList.of(
              QuestionOption.create(1L, ImmutableMap.of(Locale.US, "chocolate")),
              QuestionOption.create(2L, ImmutableMap.of(Locale.US, "peanut butter")),
              QuestionOption.create(3L, ImmutableMap.of(Locale.US, "vanilla")),
              QuestionOption.create(4L, ImmutableMap.of(Locale.US, "raspberry"))));

  private ApplicantData applicantData;
  private RadioButtonQuestionRenderer renderer;

  @Before
  public void setup() {
    applicantData = new ApplicantData();
    renderer = new RadioButtonQuestionRenderer(new ApplicantQuestion(QUESTION, applicantData));
  }

  @Test
  public void render_generatesCorrectInputNames() {
    Tag result = renderer.render();

    assertThat(result.render()).contains("name=\"applicant.favorite_ice_cream.selection\"");
    assertThat(result.render()).contains("value=\"2\"");
  }

  @Test
  public void render_generatesIds_formatsWhitespaceAsUnderscore() {
    Tag result = renderer.render();

    assertThat(result.render()).contains("id=\"peanut_butter\"");
  }

  @Test
  public void render_withExistingAnswer_checksThatOption() {
    applicantData.putLong(QUESTION.getSelectionPath(), 2L);
    Tag result = renderer.render();

    assertThat(result.render())
        .contains(
            "<input id=\"peanut_butter\" type=\"radio\""
                + " name=\"applicant.favorite_ice_cream.selection\""
                + " value=\"2\" checked=\"\">");
  }
}
