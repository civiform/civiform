package views.questiontypes;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.google.common.collect.ImmutableSet;
import j2html.tags.Tag;
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
import services.question.types.CurrencyQuestionDefinition;
import support.QuestionAnswerer;
import views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode;

public class CurrencyQuestionRendererTest extends ResetPostgres {
  private static final CurrencyQuestionDefinition CURRENCY_QUESTION_DEFINITION =
      new CurrencyQuestionDefinition(
          OptionalLong.of(1),
          "question name",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "question?"),
          LocalizedStrings.of(Locale.US, "help text"));

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
    Tag result = renderer.render(params);

    // Error message is hidden.
    assertThat(result.render()).contains("hidden");
  }

  @Test
  public void render_withValue_withoutQuestionErrors() {
    QuestionAnswerer.answerCurrencyQuestion(
        applicantData, question.getContextualizedPath(), "1,234.56");

    Tag result = renderer.render(params);

    // Error message is hidden.
    assertThat(result.render()).contains("hidden");
  }
}
