package views.questiontypes;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.stubMessagesApi;

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
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.question.types.DateQuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode;

public class DateQuestionRendererTest {
  private static final QuestionDefinitionConfig CONFIG =
      QuestionDefinitionConfig.builder()
          .setName("question name")
          .setDescription("description")
          .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
          .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
          .setId(OptionalLong.of(1))
          .setLastModifiedTime(Optional.empty())
          .build();
  private static final DateQuestionDefinition QUESTION = new DateQuestionDefinition(CONFIG);

  private final Messages messages =
      stubMessagesApi().preferred(ImmutableSet.of(Lang.defaultLang()));
  private ApplicantData applicantData;
  private ApplicantQuestion question;
  private DateQuestionRenderer renderer;
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
    renderer = new DateQuestionRenderer(question);
  }

  @Test
  public void applicantSelectedQuestionNameMatch_hasAutoFocus() {
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
  public void applicantSelectedQuestionNameMismatch_hasNoAutoFocus() {
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
