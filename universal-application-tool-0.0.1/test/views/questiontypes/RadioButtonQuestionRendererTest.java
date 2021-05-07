package views.questiontypes;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.Tag;
import java.util.Locale;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import play.i18n.Messages;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.question.QuestionOption;
import services.question.types.RadioButtonQuestionDefinition;
import support.QuestionAnswerer;

public class RadioButtonQuestionRendererTest {

  private static final RadioButtonQuestionDefinition QUESTION =
      new RadioButtonQuestionDefinition(
          "favorite ice cream",
          Path.create("applicant.favorite_ice_cream"),
          Optional.empty(),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"),
          ImmutableList.of(
              QuestionOption.builder()
                  .setId(1L)
                  .setOptionText(ImmutableMap.of(Locale.US, "chocolate"))
                  .build(),
              QuestionOption.builder()
                  .setId(2L)
                  .setOptionText(ImmutableMap.of(Locale.US, "peanut butter"))
                  .build(),
              QuestionOption.builder()
                  .setId(3L)
                  .setOptionText(ImmutableMap.of(Locale.US, "vanilla"))
                  .build(),
              QuestionOption.builder()
                  .setId(4L)
                  .setOptionText(ImmutableMap.of(Locale.US, "raspberry"))
                  .build()));

  private final Messages messages =
      stubMessagesApi().preferred(ImmutableSet.of(Lang.defaultLang()));
  private final ApplicantQuestionRendererParams params =
      ApplicantQuestionRendererParams.sample(messages);

  private ApplicantData applicantData;
  private ApplicantQuestion question;
  private RadioButtonQuestionRenderer renderer;

  @Before
  public void setup() {
    applicantData = new ApplicantData();
    question = new ApplicantQuestion(QUESTION, applicantData, ApplicantData.APPLICANT_PATH);
    renderer = new RadioButtonQuestionRenderer(question);
  }

  @Test
  public void render_generatesCorrectInputNames() {
    Tag result = renderer.render(params);

    assertThat(result.render()).contains("name=\"applicant.favorite_ice_cream.selection\"");
    assertThat(result.render()).contains("value=\"2\"");
  }

  @Test
  public void render_generatesIds_formatsWhitespaceAsUnderscore() {
    Tag result = renderer.render(params);

    assertThat(result.render()).contains("id=\"peanut_butter\"");
  }

  @Test
  public void render_withExistingAnswer_checksThatOption() {
    QuestionAnswerer.answerSingleSelectQuestion(
        applicantData, question.getContextualizedPath(), 2L);
    Tag result = renderer.render(params);

    assertThat(result.render())
        .contains(
            "<input id=\"peanut_butter\" type=\"radio\""
                + " name=\"applicant.favorite_ice_cream.selection\""
                + " value=\"2\" checked=\"\"");
  }
}
