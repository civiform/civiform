package views.questiontypes;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.google.common.collect.ImmutableSet;
import j2html.tags.specialized.DivTag;
import java.util.Locale;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.program.ProgramQuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.TextQuestionDefinition;
import services.question.types.TextQuestionDefinition.TextValidationPredicates;
import support.QuestionAnswerer;

public class TextQuestionRendererTest extends ResetPostgres {
  private static final TextQuestionDefinition TEXT_QUESTION_DEFINITION =
      new TextQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("question name")
              .setDescription("description")
              .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
              .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
              .setLastModifiedTime(Optional.empty())
              .setValidationPredicates(TextValidationPredicates.create(2, 3))
              .setId(123L)
              .build());

  private final ApplicantData applicantData = new ApplicantData();

  private ApplicantQuestion question;
  private Messages messages;
  private ApplicantQuestionRendererParams params;
  private TextQuestionRenderer renderer;

  @Before
  public void setUp() {
    question =
        new ApplicantQuestion(
            ProgramQuestionDefinition.create(TEXT_QUESTION_DEFINITION, Optional.empty())
                .setOptional(true),
            applicantData,
            Optional.empty());
    messages = instanceOf(MessagesApi.class).preferred(ImmutableSet.of(Lang.defaultLang()));
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setErrorDisplayMode(ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS)
            .build();
    renderer = new TextQuestionRenderer(question);
  }

  @Test
  public void render_withoutQuestionErrors() {
    DivTag result = renderer.render(params);

    assertThat(result.render()).doesNotContain("Must contain at");
  }

  @Test
  public void render_withMinLengthError() {
    QuestionAnswerer.answerTextQuestion(applicantData, question.getContextualizedPath(), "a");

    DivTag result = renderer.render(params);

    assertThat(result.render()).contains("Must contain at least 2 characters.");
  }

  @Test
  public void render_withMaxLengthError() {
    QuestionAnswerer.answerTextQuestion(applicantData, question.getContextualizedPath(), "abcd");

    DivTag result = renderer.render(params);

    assertThat(result.render()).contains("Must contain at most 3 characters.");
  }

  @Test
  public void render_withAriaLabels() {
    DivTag result = renderer.render(params);

    assertThat(
            result
                .render()
                .matches(
                    ".*input type=\"text\" value=\"\""
                        + " aria-describedby=\"[A-Za-z]{8}-description\".*"))
        .isTrue();
  }
}
