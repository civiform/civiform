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
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionValidationPredicates;
import services.question.types.QuestionDefinitionConfig;
import support.QuestionAnswerer;

public class CheckboxQuestionRendererTest extends ResetPostgres {

  private static final QuestionDefinitionConfig CONFIG =
      QuestionDefinitionConfig.builder()
          .setName("question name")
          .setDescription("description")
          .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
          .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
          .setValidationPredicates(MultiOptionValidationPredicates.create(1, 2))
          .setId(OptionalLong.of(1))
          .setLastModifiedTime(Optional.empty())
          .build();
  private static final ImmutableList<QuestionOption> QUESTION_OPTIONS =
      ImmutableList.of(
          QuestionOption.create(1L, LocalizedStrings.of(Locale.US, "hello")),
          QuestionOption.create(2L, LocalizedStrings.of(Locale.US, "happy")),
          QuestionOption.create(3L, LocalizedStrings.of(Locale.US, "world")));
  private static final MultiOptionQuestionDefinition CHECKBOX_QUESTION =
      new MultiOptionQuestionDefinition(CONFIG, QUESTION_OPTIONS, MultiOptionQuestionType.CHECKBOX);
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

    assertThat(result.render()).contains("Please select fewer than 3");
  }

  @Test
  public void render_withAriaLabels() {
    // Trigger question level error, since max of 2 answers are allowed.
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, question.getContextualizedPath(), 0, 1L);
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, question.getContextualizedPath(), 1, 2L);
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, question.getContextualizedPath(), 2, 3L);

    DivTag result = renderer.render(params);

    assertThat(
            result
                .render()
                .matches(
                    ".*fieldset aria-describedby=\"[A-Za-z]{8}-error"
                        + " [A-Za-z]{8}-description\".*"))
        .isTrue();
  }
}
