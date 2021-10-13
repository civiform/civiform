package views.questiontypes;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

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
import repository.WithPostgresContainer;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.question.types.CurrencyQuestionDefinition;
import services.question.types.CurrencyQuestionDefinition.CurrencyValidationPredicates;
import support.QuestionAnswerer;

public class CurrencyQuestionRendererTest  extends WithPostgresContainer {
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
    params = ApplicantQuestionRendererParams.sample(messages);
    renderer = new CurrencyQuestionRenderer(question);
  }

  @Test
  public void render_withoutQuestionErrors() {
    Tag result = renderer.render(params);

    assertThat(result.render()).doesNotContain("Must contain at");
  }

  @Test
  public void render_withMisformatError() {
    QuestionAnswerer.answerCurrencyQuestion(applicantData, question.getContextualizedPath(), "a");

    Tag result = renderer.render(params);

    assertThat(result.render()).contains("Must contain at least 2 characters.");
  }
}
