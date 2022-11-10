package views.questiontypes;

import static org.assertj.core.api.Assertions.assertThat;

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
import services.question.types.AddressQuestionDefinition;
import support.QuestionAnswerer;

public class AddressRendererTest extends ResetPostgres {

  private static final AddressQuestionDefinition ADDRESS_QUESTION =
      new AddressQuestionDefinition(
          OptionalLong.of(1),
          "Address Question",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "question?"),
          LocalizedStrings.of(Locale.US, "help text"),
          AddressQuestionDefinition.AddressValidationPredicates.create(),
          Optional.empty());

  private final ApplicantData applicantData = new ApplicantData();

  private ApplicantQuestion question;
  private Messages messages;
  private ApplicantQuestionRendererParams params;
  private AddressQuestionRenderer renderer;

  @Before
  public void setup() {
    question = new ApplicantQuestion(ADDRESS_QUESTION, applicantData, Optional.empty());
    messages = instanceOf(MessagesApi.class).preferred(ImmutableSet.of(Lang.defaultLang()));
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
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
}
