package views.questiontypes;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import j2html.attributes.Attr;
import j2html.tags.specialized.DivTag;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import play.i18n.Messages;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.question.QuestionAnswerer;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.QuestionDefinitionConfig;
import views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode;

public class RadioButtonQuestionRendererTest {

  private static final QuestionDefinitionConfig CONFIG =
      QuestionDefinitionConfig.builder()
          .setName("favorite ice cream")
          .setDescription("description")
          .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
          .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
          .setId(OptionalLong.of(1))
          .setLastModifiedTime(Optional.empty())
          .build();
  private static final ImmutableList<QuestionOption> QUESTION_OPTIONS =
      ImmutableList.of(
          QuestionOption.create(1L, "chocolate admin", LocalizedStrings.of(Locale.US, "chocolate")),
          QuestionOption.create(
              2L, "peanut butter admin", LocalizedStrings.of(Locale.US, "peanut butter")),
          QuestionOption.create(3L, "vanilla admin", LocalizedStrings.of(Locale.US, "vanilla")),
          QuestionOption.create(
              4L, "raspberry admin", LocalizedStrings.of(Locale.US, "raspberry")));
  private static final MultiOptionQuestionDefinition QUESTION =
      new MultiOptionQuestionDefinition(
          CONFIG, QUESTION_OPTIONS, MultiOptionQuestionType.RADIO_BUTTON);

  private final Messages messages =
      stubMessagesApi().preferred(ImmutableSet.of(Lang.defaultLang()));
  private ApplicantData applicantData;
  private ApplicantQuestion question;
  private RadioButtonQuestionRenderer renderer;
  private ApplicantQuestionRendererParams params;

  @Before
  public void setup() {
    applicantData = new ApplicantData();
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setErrorDisplayMode(ErrorDisplayMode.HIDE_ERRORS)
            .build();
    question = new ApplicantQuestion(QUESTION, applicantData, Optional.empty());
    renderer = new RadioButtonQuestionRenderer(question);
  }

  @Test
  public void render_generatesCorrectInputNames() {
    DivTag result = renderer.render(params);

    assertThat(result.render()).contains("name=\"applicant.favorite_ice_cream.selection\"");
    assertThat(result.render()).contains("value=\"2\"");
  }

  @Test
  public void render_generatesIdsForExplicitLabels() {
    DivTag result = renderer.render(params);

    // Verify we use explicit labels linked to inputs by id for a11y.
    assertThat(result.render()).contains("<label for=");
    assertThat(result.render()).contains("<input id=");
  }

  @Test
  public void render_withExistingAnswer_checksThatOption() {
    QuestionAnswerer.answerSingleSelectQuestion(
        applicantData, question.getContextualizedPath(), 2L);
    DivTag result = renderer.render(params);

    assertThat(result.render())
        .contains(
            "type=\"radio\""
                + " name=\"applicant.favorite_ice_cream.selection\""
                + " value=\"2\" checked");
  }

  @Test
  public void render_withAriaLabels() {
    DivTag result = renderer.render(params);
    // Remove invisible new line characters that break the regex match
    String cleanHtml = result.render().replace("\n", "");

    assertThat(cleanHtml.matches(".*fieldset aria-describedby=\"[A-Za-z]{8}-description\".*"))
        .isTrue();
  }

  @Test
  public void renderWithErrors_andSingleErrorMode_hasAutofocus() {
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setErrorDisplayMode(ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS)
            .setAutofocus(ApplicantQuestionRendererParams.AutoFocusTarget.FIRST_FIELD)
            .build();

    DivTag result = renderer.render(params);

    assertThat(result.render()).contains(Attr.AUTOFOCUS);
  }

  @Test
  public void renderWithoutErrors_doesNotAutofocus() {
    DivTag result = renderer.render(params);

    assertThat(result.render()).doesNotContain(Attr.AUTOFOCUS);
  }

  @Test
  public void applicantSelectedQuestionNameMatch_hasAutoFocus() {
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setAutofocus(ApplicantQuestionRendererParams.AutoFocusTarget.FIRST_FIELD)
            .setErrorDisplayMode(ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS)
            .build();
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, question.getContextualizedPath(), 0, 0L);

    DivTag result = renderer.render(params);

    assertThat(result.render()).contains(Attr.AUTOFOCUS);
  }

  @Test
  public void applicantSelectedQuestionNameMismatch_hasNoAutoFocus() {
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setErrorDisplayMode(ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS)
            .setAutofocus(ApplicantQuestionRendererParams.AutoFocusTarget.NONE)
            .build();
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, question.getContextualizedPath(), 0, 0L);

    DivTag result = renderer.render(params);

    assertThat(result.render()).doesNotContain(Attr.AUTOFOCUS);
  }
}
