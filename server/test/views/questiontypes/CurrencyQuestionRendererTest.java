package views.questiontypes;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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
import play.i18n.MessagesApi;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.question.QuestionAnswerer;
import services.question.types.CurrencyQuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode;

public class CurrencyQuestionRendererTest extends ResetPostgres {
  private static final CurrencyQuestionDefinition CURRENCY_QUESTION_DEFINITION =
      new CurrencyQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("question name")
              .setDescription("description")
              .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
              .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
              .setId(OptionalLong.of(1))
              .setLastModifiedTime(Optional.empty())
              .build());
  ;

  private final ApplicantData applicantData = new ApplicantData();

  private ApplicantQuestion question;
  private Messages messages;
  private ApplicantQuestionRendererParams params;
  private CurrencyQuestionRenderer renderer;

  @Before
  public void setUp() {
    question = new ApplicantQuestion(CURRENCY_QUESTION_DEFINITION, applicantData, Optional.empty());
    messages = instanceOf(MessagesApi.class).preferred(ImmutableSet.of(Lang.defaultLang()));
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setErrorDisplayMode(ErrorDisplayMode.HIDE_ERRORS)
            .build();
    renderer = new CurrencyQuestionRenderer(question);
  }

  @Test
  public void render_withoutQuestionErrors() {
    DivTag result = renderer.render(params);

    // Error message is hidden.
    assertThat(result.render()).contains("hidden");
  }

  @Test
  public void render_withValue_withoutQuestionErrors() {
    QuestionAnswerer.answerCurrencyQuestion(
        applicantData, question.getContextualizedPath(), "1,234.56");

    DivTag result = renderer.render(params);

    // Error message is hidden.
    assertThat(result.render()).contains("hidden");
  }

  @Test
  public void render_withAriaLabels() {
    DivTag result = renderer.render(params);
    // Remove invisible new line characters that break the regex match
    String cleanHtml = result.render().replace("\n", "");

    assertThat(
            cleanHtml.matches(
                ".*input type=\"text\" currency inputmode=\"decimal\" value=\"\""
                    + " aria-describedby=\"[A-Za-z]{8}-description\".*"))
        .isTrue();
  }

  @Test
  public void maybeFocusOnInputNameMatch_autofocusIsPresent() {
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setAutofocus(ApplicantQuestionRendererParams.AutoFocusTarget.FIRST_FIELD)
            .setErrorDisplayMode(ErrorDisplayMode.DISPLAY_ERRORS)
            .build();

    DivTag result = renderer.render(params);

    assertThat(result.render()).contains(Attr.AUTOFOCUS);
  }

  @Test
  public void maybeFocusOnInputNameIsBlank_autofocusIsNotPresent() {
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setAutofocus(ApplicantQuestionRendererParams.AutoFocusTarget.NONE)
            .setErrorDisplayMode(ErrorDisplayMode.DISPLAY_ERRORS)
            .build();

    DivTag result = renderer.render(params);

    assertThat(result.render()).doesNotContain(Attr.AUTOFOCUS);
  }
}
