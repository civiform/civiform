package views.questiontypes;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.stubMessagesApi;

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
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.question.ApplicantQuestion;
import services.question.QuestionOption;
import services.question.types.CheckboxQuestionDefinition;

public class CheckboxQuestionRendererTest {

  private static final CheckboxQuestionDefinition CHECKBOX_QUESTION =
      new CheckboxQuestionDefinition(
          "question name",
          Path.create("applicant.my.path"),
          Optional.empty(),
          "description",
          ImmutableMap.of(Locale.US, "question?"),
          ImmutableMap.of(Locale.US, "help text"),
          ImmutableList.of(QuestionOption.create(1L, ImmutableMap.of(Locale.US, "hello"))));

  private final ApplicantData applicantData = new ApplicantData();
  private final Messages messages =
      stubMessagesApi().preferred(ImmutableSet.of(Lang.defaultLang()));

  private CheckboxQuestionRenderer renderer;

  @Before
  public void setup() {
    ApplicantQuestion question =
        new ApplicantQuestion(CHECKBOX_QUESTION, applicantData, ApplicantData.APPLICANT_PATH);
    renderer = new CheckboxQuestionRenderer(question);
  }

  @Test
  public void render_usesCorrectInputName() {
    Tag result = renderer.render(messages);

    assertThat(result.render()).contains("applicant.question_name.selection[]");
  }
}
