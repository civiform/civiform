package views.admin.question;

import static j2html.TagCreator.div;
import static org.assertj.core.api.Assertions.assertThat;

import forms.AddressQuestionForm;
import forms.DropdownQuestionForm;
import forms.NameQuestionForm;
import forms.NumberQuestionForm;
import forms.RadioButtonQuestionForm;
import forms.RepeaterQuestionForm;
import forms.TextQuestionForm;
import j2html.tags.ContainerTag;
import org.junit.Test;
import views.admin.questions.QuestionConfig;

public class QuestionConfigTest {

  private static final ContainerTag DEFAULT_CONFIG = div();

  @Test
  public void allHandledTypesHaveCustomConfig() {
    assertThat(QuestionConfig.buildQuestionConfig(new TextQuestionForm()))
        .toString()
        .contains("text-question-min-length-input");

    assertThat(QuestionConfig.buildQuestionConfig(new AddressQuestionForm()))
        .toString()
        .contains("address-question-default-state-select");

    assertThat(QuestionConfig.buildQuestionConfig(new DropdownQuestionForm()))
        .toString()
        .contains("multi-select-question-config");

    assertThat(QuestionConfig.buildQuestionConfig(new DropdownQuestionForm()))
        .toString()
        .contains("single-select-question-config");

    assertThat(QuestionConfig.buildQuestionConfig(new NumberQuestionForm()))
        .toString()
        .contains("number-question-min-value-input");

    assertThat(QuestionConfig.buildQuestionConfig(new RadioButtonQuestionForm()))
        .toString()
        .contains("single-select-question-config");
  }

  @Test
  public void unhandledQuestionTypes_defaultsToDefaultConfig() {
    assertThat(QuestionConfig.buildQuestionConfig(new NameQuestionForm()))
        .isEqualTo(DEFAULT_CONFIG);

    assertThat(QuestionConfig.buildQuestionConfig(new RepeaterQuestionForm()))
        .isEqualTo(DEFAULT_CONFIG);
  }
}
