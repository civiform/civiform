package views.admin.question;

import static j2html.TagCreator.div;
import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import forms.AddressQuestionForm;
import forms.CheckboxQuestionForm;
import forms.DropdownQuestionForm;
import forms.EnumeratorQuestionForm;
import forms.FileUploadQuestionForm;
import forms.IdQuestionForm;
import forms.NameQuestionForm;
import forms.NumberQuestionForm;
import forms.RadioButtonQuestionForm;
import forms.TextQuestionForm;
import j2html.tags.ContainerTag;
import org.junit.Test;
import play.i18n.Lang;
import play.i18n.Messages;
import views.admin.questions.QuestionConfig;

public class QuestionConfigTest {

  private static final ContainerTag DEFAULT_CONFIG = div();

  private final Messages messages =
      stubMessagesApi().preferred(ImmutableSet.of(Lang.defaultLang()));

  @Test
  public void allHandledTypesHaveCustomConfig() {
    assertThat(
            QuestionConfig.buildQuestionConfig(new TextQuestionForm(), messages).renderFormatted())
        .contains("text-question-min-length-input");

    assertThat(
            QuestionConfig.buildQuestionConfig(new AddressQuestionForm(), messages)
                .renderFormatted())
        .contains("address-question-default-state-select");

    assertThat(
            QuestionConfig.buildQuestionConfig(new CheckboxQuestionForm(), messages)
                .renderFormatted())
        .contains("multi-select-question-config");

    assertThat(
            QuestionConfig.buildQuestionConfig(new DropdownQuestionForm(), messages)
                .renderFormatted())
        .contains("single-select-question-config");

    assertThat(QuestionConfig.buildQuestionConfig(new IdQuestionForm(), messages).renderFormatted())
        .contains("id-question-min-length-input");

    assertThat(
            QuestionConfig.buildQuestionConfig(new NumberQuestionForm(), messages)
                .renderFormatted())
        .contains("number-question-min-value-input");

    assertThat(
            QuestionConfig.buildQuestionConfig(new RadioButtonQuestionForm(), messages)
                .renderFormatted())
        .contains("single-select-question-config");

    assertThat(
            QuestionConfig.buildQuestionConfig(new EnumeratorQuestionForm(), messages)
                .renderFormatted())
        .contains("enumerator-question-config");
  }

  @Test
  public void unhandledQuestionTypes_defaultsToDefaultConfig() {
    assertThat(QuestionConfig.buildQuestionConfig(new FileUploadQuestionForm(), messages))
        .isEqualTo(DEFAULT_CONFIG);

    assertThat(QuestionConfig.buildQuestionConfig(new NameQuestionForm(), messages))
        .isEqualTo(DEFAULT_CONFIG);
  }

  @Test
  public void checkboxForm_preservesNewOptions() {
    CheckboxQuestionForm form = new CheckboxQuestionForm();
    form.setOptions(ImmutableList.of("existing-option-a", "existing-option-b"));
    form.setOptionIds(ImmutableList.of(1L, 2L));
    form.setNewOptions(ImmutableList.of("new-option-c", "new-option-d"));

    String result = QuestionConfig.buildQuestionConfig(form, messages).renderFormatted();

    assertThat(result).contains("existing-option-a");
    assertThat(result).contains("existing-option-b");
    assertThat(result).contains("new-option-c");
    assertThat(result).contains("new-option-d");
  }
}
