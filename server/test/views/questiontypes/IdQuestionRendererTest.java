package views.questiontypes;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static support.FakeRequestBuilder.fakeRequest;

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
import play.mvc.Http.Request;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.program.ProgramQuestionDefinition;
import services.question.QuestionAnswerer;
import services.question.types.IdQuestionDefinition;
import services.question.types.IdQuestionDefinition.IdValidationPredicates;
import services.question.types.QuestionDefinitionConfig;

public class IdQuestionRendererTest extends ResetPostgres {
  private static final IdQuestionDefinition ID_QUESTION_DEFINITION =
      new IdQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("question name")
              .setDescription("description")
              .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
              .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
              .setValidationPredicates(IdValidationPredicates.create(2, 3))
              .setId(OptionalLong.of(1))
              .setLastModifiedTime(Optional.empty())
              .build());
  ;

  private final ApplicantData applicantData = new ApplicantData();

  private ApplicantQuestion question;
  private Messages messages;
  private ApplicantQuestionRendererParams params;
  private IdQuestionRenderer renderer;
  private final Request request = fakeRequest();

  @Before
  public void setUp() {
    question =
        new ApplicantQuestion(
            ProgramQuestionDefinition.create(ID_QUESTION_DEFINITION, Optional.empty())
                .setOptional(true),
            applicantData,
            Optional.empty());
    messages = instanceOf(MessagesApi.class).preferred(ImmutableSet.of(Lang.defaultLang()));
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setRequest(request)
            .setErrorDisplayMode(ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS)
            .build();
    renderer = new IdQuestionRenderer(question);
  }

  @Test
  public void render_withoutQuestionErrors() {
    DivTag result = renderer.render(params);

    assertThat(result.render()).doesNotContain("Must contain");
  }

  @Test
  public void render_withMinLengthError() {
    QuestionAnswerer.answerIdQuestion(applicantData, question.getContextualizedPath(), "1");

    DivTag result = renderer.render(params);

    assertThat(result.render()).contains("Must contain at least 2 characters.");
  }

  @Test
  public void render_withMaxLengthError() {
    QuestionAnswerer.answerIdQuestion(applicantData, question.getContextualizedPath(), "1234");

    DivTag result = renderer.render(params);

    assertThat(result.render()).contains("Must contain at most 3 characters.");
  }

  @Test
  public void render_withInvalidCharactersError() {
    QuestionAnswerer.answerIdQuestion(applicantData, question.getContextualizedPath(), "ab");

    DivTag result = renderer.render(params);

    assertThat(result.render()).contains("Must contain only numbers.");
  }

  @Test
  public void render_withAriaLabels() {
    DivTag result = renderer.render(params);
    // Remove invisible new line characters that break the regex match
    String cleanHtml = result.render().replace("\n", "");

    assertThat(
            cleanHtml.matches(
                ".*input type=\"text\" value=\"\""
                    + " aria-describedby=\"[A-Za-z]{8}-description\".*"))
        .isTrue();
  }

  @Test
  public void maybeFocusOnInputNameMatch_autofocusIsPresent() {
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setAutofocus(ApplicantQuestionRendererParams.AutoFocusTarget.FIRST_FIELD)
            .build();

    DivTag result = renderer.render(params);

    assertThat(result.render()).contains(Attr.AUTOFOCUS);
  }

  @Test
  public void maybeFocusOnInputNameMismatch_autofocusIsNotPresent() {
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setAutofocus(ApplicantQuestionRendererParams.AutoFocusTarget.NONE)
            .build();

    DivTag result = renderer.render(params);
    assertThat(result.render()).doesNotContain(Attr.AUTOFOCUS);
  }

  @Test
  public void maybeFocusOnInputNameIsAreBlank_autofocusIsNotPresent() {
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setErrorDisplayMode(ApplicantQuestionRendererParams.ErrorDisplayMode.HIDE_ERRORS)
            .build();

    DivTag result = renderer.render(params);

    assertThat(result.render()).doesNotContain(Attr.AUTOFOCUS);
  }
}
