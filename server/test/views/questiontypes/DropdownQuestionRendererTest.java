package views.questiontypes;

import static org.assertj.core.api.Assertions.assertThat;
import static support.FakeRequestBuilder.fakeRequest;

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
import play.i18n.MessagesApi;
import play.mvc.Http.Request;
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.question.QuestionAnswerer;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.QuestionDefinitionConfig;
import views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode;

public class DropdownQuestionRendererTest extends ResetPostgres {

  private static final QuestionDefinitionConfig CONFIG =
      QuestionDefinitionConfig.builder()
          .setName("favorite ice cream")
          .setDescription("description")
          .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
          .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
          .setLastModifiedTime(Optional.empty())
          .setId(OptionalLong.of(1))
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
      new MultiOptionQuestionDefinition(CONFIG, QUESTION_OPTIONS, MultiOptionQuestionType.DROPDOWN);

  private final ApplicantData applicantData = new ApplicantData();

  private ApplicantQuestion question;
  private Messages messages;
  private ApplicantQuestionRendererParams params;
  private DropdownQuestionRenderer renderer;
  private final Request request = fakeRequest();

  @Before
  public void setup() {
    question = new ApplicantQuestion(QUESTION, applicantData, Optional.empty());
    messages = instanceOf(MessagesApi.class).preferred(ImmutableSet.of(Lang.defaultLang()));
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setRequest(request)
            .setErrorDisplayMode(ErrorDisplayMode.HIDE_ERRORS)
            .build();
    renderer = new DropdownQuestionRenderer(question);
  }

  @Test
  public void render_generatesCorrectInputNames() {
    DivTag result = renderer.render(params);

    assertThat(result.render()).contains("name=\"applicant.favorite_ice_cream.selection\"");
    assertThat(result.render()).contains("value=\"2\"");
  }

  @Test
  public void render_withExistingAnswer_selectsThatOption() {
    QuestionAnswerer.answerSingleSelectQuestion(
        applicantData, question.getContextualizedPath(), 2L);
    DivTag result = renderer.render(params);

    assertThat(result.render()).contains("value=\"2\" selected");
  }

  @Test
  public void render_noExistingAnswer_selectsPlaceholder() {
    DivTag result = renderer.render(params);

    assertThat(result.render()).contains("hidden selected");
    assertThat(result.render()).contains("Choose an option");
  }

  @Test
  public void render_withAriaLabels() {
    DivTag result = renderer.render(params);
    // Remove invisible new line characters that break the regex match
    String cleanHtml = result.render().replace("\n", "");

    assertThat(cleanHtml.matches(".*select aria-describedby=\"[A-Za-z]{8}-description\".*"))
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
  public void maybeFocusOnInputNameIsBlank_autofocusIsNotPresent() {
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setErrorDisplayMode(ApplicantQuestionRendererParams.ErrorDisplayMode.HIDE_ERRORS)
            .build();

    DivTag result = renderer.render(params);

    assertThat(result.render()).doesNotContain(Attr.AUTOFOCUS);
  }
}
