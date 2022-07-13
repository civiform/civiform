package views.questiontypes;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
import services.question.QuestionOption;
import services.question.types.CheckboxQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import support.QuestionAnswerer;

public class CheckboxQuestionRendererTest extends ResetPostgres {

  private static final CheckboxQuestionDefinition CHECKBOX_QUESTION =
      new CheckboxQuestionDefinition(
          OptionalLong.of(1),
          "question name",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "question?"),
          LocalizedStrings.of(Locale.US, "help text"),
          ImmutableList.of(
              QuestionOption.create(1L, LocalizedStrings.of(Locale.US, "hello")),
              QuestionOption.create(2L, LocalizedStrings.of(Locale.US, "happy")),
              QuestionOption.create(3L, LocalizedStrings.of(Locale.US, "world"))),
          MultiOptionQuestionDefinition.MultiOptionValidationPredicates.create(1, 2));

  private final ApplicantData applicantData = new ApplicantData();

  private ApplicantQuestion question;
  private Messages messages;
  private ApplicantQuestionRendererParams params;
  private CheckboxQuestionRenderer renderer;

  @Before
  public void setup() {
    question = new ApplicantQuestion(CHECKBOX_QUESTION, applicantData, Optional.empty());
    messages = instanceOf(MessagesApi.class).preferred(ImmutableSet.of(Lang.defaultLang()));
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setErrorDisplayMode(ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS)
            .build();
    renderer = new CheckboxQuestionRenderer(question);
  }

  @Test
  public void render_usesCorrectInputName() {
    DivTag result = renderer.render(params);

    assertThat(result.render()).contains("applicant.question_name.selections[]");
  }

  @Test
  public void render_includesErrorMessages() {
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, question.getContextualizedPath(), 0, 1L);
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, question.getContextualizedPath(), 1, 2L);
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, question.getContextualizedPath(), 2, 3L);

    DivTag result = renderer.render(params);

    assertThat(result.render()).contains("Please select fewer than 2");
  }
}
