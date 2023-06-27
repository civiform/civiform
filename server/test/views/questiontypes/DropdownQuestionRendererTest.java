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
import views.questiontypes.ApplicantQuestionRendererParams.ErrorDisplayMode;

public class DropdownQuestionRendererTest extends ResetPostgres {

  private static final QuestionDefinitionConfig CONFIG =
      QuestionDefinitionConfig.builder()
          .setName("favorite ice cream")
          .setDescription("description")
          .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
          .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
          .setValidationPredicates(MultiOptionValidationPredicates.create())
          .setLastModifiedTime(Optional.empty())
          .setId(OptionalLong.of(1))
          .build();
  private static final ImmutableList<QuestionOption> QUESTION_OPTIONS =
      ImmutableList.of(
          QuestionOption.create(1L, LocalizedStrings.of(Locale.US, "chocolate")),
          QuestionOption.create(2L, LocalizedStrings.of(Locale.US, "peanut butter")),
          QuestionOption.create(3L, LocalizedStrings.of(Locale.US, "vanilla")),
          QuestionOption.create(4L, LocalizedStrings.of(Locale.US, "raspberry")));
  private static final MultiOptionQuestionDefinition QUESTION =
      new MultiOptionQuestionDefinition(CONFIG, QUESTION_OPTIONS, MultiOptionQuestionType.DROPDOWN);

  private final ApplicantData applicantData = new ApplicantData();

  private ApplicantQuestion question;
  private Messages messages;
  private ApplicantQuestionRendererParams params;
  private DropdownQuestionRenderer renderer;

  @Before
  public void setup() {
    question = new ApplicantQuestion(QUESTION, applicantData, Optional.empty());
    messages = instanceOf(MessagesApi.class).preferred(ImmutableSet.of(Lang.defaultLang()));
    params =
        ApplicantQuestionRendererParams.builder()
            .setMessages(messages)
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

    assertThat(result.render().matches(".*select aria-describedby=\"[A-Za-z]{8}-description\".*"))
        .isTrue();
  }
}
