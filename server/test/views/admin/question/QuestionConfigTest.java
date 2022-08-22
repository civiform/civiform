package views.admin.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import forms.CheckboxQuestionForm;
import forms.QuestionForm;
import forms.QuestionFormBuilder;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import play.i18n.Lang;
import play.i18n.Messages;
import services.question.types.QuestionType;
import views.admin.questions.QuestionConfig;

@RunWith(JUnitParamsRunner.class)
public class QuestionConfigTest {

  private final Messages messages =
      stubMessagesApi().preferred(ImmutableSet.of(Lang.defaultLang()));

  @Test
  @Parameters(source = QuestionType.class)
  public void resultForAllQuestions(QuestionType questionType) throws Exception {
    QuestionForm questionForm = QuestionFormBuilder.create(questionType);
    Optional<DivTag> maybeConfig = QuestionConfig.buildQuestionConfig(questionForm, messages);
    switch (questionType) {
      case ADDRESS:
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("Disallow post office boxes");
        break;
      case CHECKBOX:
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted())
            .contains("Minimum number of choices required");
        assertThat(maybeConfig.get().renderFormatted()).contains("Add answer option");
        break;
      case CURRENCY:
        assertThat(maybeConfig).isEmpty();
        break;
      case DATE:
        assertThat(maybeConfig).isEmpty();
        break;
      case DROPDOWN:
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("Add answer option");
        break;
      case EMAIL:
        assertThat(maybeConfig).isEmpty();
        break;
      case ENUMERATOR:
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("What are we enumerating");
        break;
      case FILEUPLOAD:
        assertThat(maybeConfig).isEmpty();
        break;
      case ID:
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("Minimum length");
        break;
      case NAME:
        assertThat(maybeConfig).isEmpty();
        break;
      case NUMBER:
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("Minimum value");
        break;
      case RADIO_BUTTON:
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("Add answer option");
        break;
      case STATIC:
        assertThat(maybeConfig).isEmpty();
        break;
      case TEXT:
        assertThat(maybeConfig).isPresent();
        assertThat(maybeConfig.get().renderFormatted()).contains("Minimum length");
        break;
      default:
        fail(
            "Unhandled question type: %s. Please add a configuration in"
                + " QuestionConfig.buildQuestionConfig and add an explicit case statement for the"
                + " type.",
            questionType);
    }
  }

  @Test
  public void checkboxForm_preservesNewOptions() {
    CheckboxQuestionForm form = new CheckboxQuestionForm();
    form.setOptions(ImmutableList.of("existing-option-a", "existing-option-b"));
    form.setOptionIds(ImmutableList.of(1L, 2L));
    form.setNewOptions(ImmutableList.of("new-option-c", "new-option-d"));

    Optional<DivTag> maybeConfig = QuestionConfig.buildQuestionConfig(form, messages);
    assertThat(maybeConfig).isPresent();
    String result = maybeConfig.get().renderFormatted();

    assertThat(result).contains("existing-option-a");
    assertThat(result).contains("existing-option-b");
    assertThat(result).contains("new-option-c");
    assertThat(result).contains("new-option-d");
  }
}
