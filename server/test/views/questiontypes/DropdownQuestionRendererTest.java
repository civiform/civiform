package views.questiontypes;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
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
import repository.ResetPostgres;
import services.LocalizedStrings;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.question.QuestionOption;
import services.question.types.DropdownQuestionDefinition;
import support.QuestionAnswerer;

public class DropdownQuestionRendererTest extends ResetPostgres {

  private static final DropdownQuestionDefinition QUESTION =
      new DropdownQuestionDefinition(
          OptionalLong.of(1),
          "favorite ice cream",
          Optional.empty(),
          "description",
          LocalizedStrings.of(Locale.US, "question?"),
          LocalizedStrings.of(Locale.US, "help text"),
          ImmutableList.of(
              QuestionOption.create(1L, LocalizedStrings.of(Locale.US, "chocolate")),
              QuestionOption.create(2L, LocalizedStrings.of(Locale.US, "peanut butter")),
              QuestionOption.create(3L, LocalizedStrings.of(Locale.US, "vanilla")),
              QuestionOption.create(4L, LocalizedStrings.of(Locale.US, "raspberry"))));

  private final ApplicantData applicantData = new ApplicantData();

  private ApplicantQuestion question;
  private Messages messages;
  private ApplicantQuestionRendererParams params;
  private DropdownQuestionRenderer renderer;

  @Before
  public void setup() {
    question = new ApplicantQuestion(QUESTION, applicantData, Optional.empty());
    messages = instanceOf(MessagesApi.class).preferred(ImmutableSet.of(Lang.defaultLang()));
    params = ApplicantQuestionRendererParams.sample(messages);
    renderer = new DropdownQuestionRenderer(question);
  }

  @Test
  public void render_generatesCorrectInputNames() {
    Tag result = renderer.render(params);

    assertThat(result.render()).contains("name=\"applicant.favorite_ice_cream.selection\"");
    assertThat(result.render()).contains("value=\"2\"");
  }

  @Test
  public void render_withExistingAnswer_selectsThatOption() {
    QuestionAnswerer.answerSingleSelectQuestion(
        applicantData, question.getContextualizedPath(), 2L);
    Tag result = renderer.render(params);

    assertThat(result.render()).contains("<option value=\"2\" selected");
  }

  @Test
  public void render_noExistingAnswer_selectsPlaceholder() {
    Tag result = renderer.render(params);

    assertThat(result.render()).contains("hidden selected");
    assertThat(result.render()).contains("Choose an option");
  }

  @Test
  public void render_withAriaLabels() {
    Tag result = renderer.render(params);

    String id = question.getContextualizedPath().toString();
    assertThat(result.render())
        .contains("aria-describedBy=" + String.format("\"%s-description\"", id));
  }
}
