package views.questiontypes;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import j2html.tags.Tag;
import java.util.Locale;
import models.LifecycleStage;
import org.junit.Before;
import org.junit.Test;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.question.RadioButtonQuestionDefinition;

public class RadioButtonQuestionRendererTest {

  private static final RadioButtonQuestionDefinition QUESTION =
      new RadioButtonQuestionDefinition(
          1L,
          "name",
          Path.create("applicant.favorite_ice_cream"),
          "description",
          LifecycleStage.ACTIVE,
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"),
          ImmutableListMultimap.of(
              Locale.US,
              "chocolate",
              Locale.US,
              "peanut   butter",
              Locale.US,
              "vanilla",
              Locale.US,
              "raspberry"));

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
    assertThat(result.render()).contains("value=\"peanut   butter\"");
  }

  @Test
  public void render_generatesIds_formatsWhitespaceAsUnderscore() {
    Tag result = renderer.render();

    assertThat(result.render()).contains("id=\"peanut_butter\"");
  }

  @Test
  public void render_withExistingAnswer_checksThatOption() {
    applicantData.putString(QUESTION.getSelectionPath(), "peanut   butter");
    Tag result = renderer.render();

    assertThat(result.render())
        .contains(
            "<input id=\"peanut_butter\" type=\"radio\""
                + " name=\"applicant.favorite_ice_cream.selection\""
                + " value=\"peanut   butter\" checked=\"\">");
  }
}
