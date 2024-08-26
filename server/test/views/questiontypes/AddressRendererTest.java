package views.questiontypes;

import static org.assertj.core.api.Assertions.assertThat;
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
import services.question.QuestionAnswerer;
import services.question.types.AddressQuestionDefinition;
import services.question.types.QuestionDefinitionConfig;

public class AddressRendererTest extends ResetPostgres {

  private static final AddressQuestionDefinition ADDRESS_QUESTION =
      new AddressQuestionDefinition(
          QuestionDefinitionConfig.builder()
              .setName("Address Question")
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
  private AddressQuestionRenderer renderer;
  private final Request request = fakeRequest();

  @Before
  public void setup() {
    question = new ApplicantQuestion(ADDRESS_QUESTION, applicantData, Optional.empty());
    messages = instanceOf(MessagesApi.class).preferred(ImmutableSet.of(Lang.defaultLang()));
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setRequest(request)
            .setErrorDisplayMode(ApplicantQuestionRendererParams.ErrorDisplayMode.DISPLAY_ERRORS)
            .build();
    renderer = new AddressQuestionRenderer(question);
  }

  @Test
  public void render_withPreviousAnswerNotInOptionsBeEmpty() {
    QuestionAnswerer.answerAddressQuestion(
        applicantData,
        question.getContextualizedPath(),
        "123 street",
        "line 2",
        "city",
        "UnitedStates",
        "90000");

    DivTag result = renderer.render(params);
    assertThat(result.render()).contains("option value=\"\"");
  }

  @Test
  public void render_withPreviousAnswerInOptionsBePresent() {
    QuestionAnswerer.answerAddressQuestion(
        applicantData,
        question.getContextualizedPath(),
        "123 street",
        "line 2",
        "city",
        "WA",
        "90000");

    DivTag result = renderer.render(params);
    assertThat(result.render()).contains("selected>WA");
  }

  @Test
  public void maybeFocusOnInputNameMatch_autofocusIsPresent() {
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setErrorDisplayMode(ApplicantQuestionRendererParams.ErrorDisplayMode.HIDE_ERRORS)
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
            .setErrorDisplayMode(ApplicantQuestionRendererParams.ErrorDisplayMode.HIDE_ERRORS)
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
