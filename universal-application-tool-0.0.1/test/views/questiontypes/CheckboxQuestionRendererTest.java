package views.questiontypes;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import j2html.tags.Tag;
import java.util.Locale;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import repository.WithPostgresContainer;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.question.QuestionOption;
import services.question.types.CheckboxQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import support.QuestionAnswerer;

public class CheckboxQuestionRendererTest extends WithPostgresContainer {

  private static final CheckboxQuestionDefinition CHECKBOX_QUESTION =
      new CheckboxQuestionDefinition(
          "question name",
          Path.create("applicant.my.path"),
          Optional.empty(),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"),
          ImmutableList.of(
              QuestionOption.builder()
                  .setId(1L)
                  .setOptionText(ImmutableMap.of(Locale.US, "hello"))
                  .build(),
              QuestionOption.builder()
                  .setId(2L)
                  .setOptionText(ImmutableMap.of(Locale.US, "happy"))
                  .build(),
              QuestionOption.builder()
                  .setId(3L)
                  .setOptionText(ImmutableMap.of(Locale.US, "world"))
                  .build()),
          MultiOptionQuestionDefinition.MultiOptionValidationPredicates.create(1, 2));

  private final ApplicantData applicantData = new ApplicantData();

  private ApplicantQuestion question;
  private Messages messages;
  private ApplicantQuestionRendererParams params;
  private CheckboxQuestionRenderer renderer;

  @Before
  public void setup() {
    question =
        new ApplicantQuestion(CHECKBOX_QUESTION, applicantData, ApplicantData.APPLICANT_PATH);
    messages = instanceOf(MessagesApi.class).preferred(ImmutableSet.of(Lang.defaultLang()));
    params = ApplicantQuestionRendererParams.sample(messages);
    renderer = new CheckboxQuestionRenderer(question);
  }

  @Test
  public void render_usesCorrectInputName() {
    Tag result = renderer.render(params);

    assertThat(result.render()).contains("applicant.question_name.selection[]");
  }

  @Test
  public void render_includesErrorMessages() {
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, question.getContextualizedPath(), 0, 1L);
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, question.getContextualizedPath(), 1, 2L);
    QuestionAnswerer.answerMultiSelectQuestion(
        applicantData, question.getContextualizedPath(), 2, 3L);

    Tag result = renderer.render(params);

    assertThat(result.render()).contains("Please select fewer than 2");
  }
}
