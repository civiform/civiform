package views.questiontypes;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableSet;
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
import services.question.types.PhoneQuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode;

public class PhoneQuestionRendererTest {
  private static final QuestionDefinitionConfig CONFIG =
      QuestionDefinitionConfig.builder()
          .setName("question name")
          .setDescription("description")
          .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
          .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
          .setId(OptionalLong.of(1))
          .setLastModifiedTime(Optional.empty())
          .build();
  private static final PhoneQuestionDefinition QUESTION = new PhoneQuestionDefinition(CONFIG);

  private final Messages messages =
      stubMessagesApi().preferred(ImmutableSet.of(Lang.defaultLang()));
  private ApplicantData applicantData;
  private ApplicantQuestion question;
  private PhoneQuestionRenderer renderer;
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
    renderer = new PhoneQuestionRenderer(question);
  }

  @Test
  public void applicantSelectedQuestionNameAndTypeMatch_hasAutoFocus() {
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setErrorDisplayMode(ApplicantQuestionRendererParams.ErrorDisplayMode.HIDE_ERRORS)
            .setQuestionName(Optional.of("question name"))
            .setQuestionType(Optional.of("PHONE"))
            .build();

    DivTag result = renderer.render(params);

    assertThat(result.render()).contains("autofocus");
  }

  @Test
  public void applicantSelectedQuestionNameMismatch_hasNoAutoFocus() {
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setErrorDisplayMode(ApplicantQuestionRendererParams.ErrorDisplayMode.HIDE_ERRORS)
            .setQuestionName(Optional.of("wrong name"))
            .setQuestionType(Optional.of("PHONE"))
            .build();

    DivTag result = renderer.render(params);

    assertThat(result.render()).doesNotContain("autofocus");
  }

  @Test
  public void applicantSelectedQuestionTypeMismatch_hasNoAutoFocus() {
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setErrorDisplayMode(ApplicantQuestionRendererParams.ErrorDisplayMode.HIDE_ERRORS)
            .setQuestionName(Optional.of("question name"))
            .setQuestionType(Optional.of("TEXT"))
            .build();

    DivTag result = renderer.render(params);

    assertThat(result.render()).doesNotContain("autofocus");
  }

  @Test
  public void maybeFocusOnInputNameAndTypeAreBlank_autofocusIsNotPresent() {
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
            .setErrorDisplayMode(ApplicantQuestionRendererParams.ErrorDisplayMode.HIDE_ERRORS)
            .build();

    DivTag result = renderer.render(params);

    assertThat(result.render()).doesNotContain("autofocus");
  }
}
