package views.admin.question;

import static j2html.TagCreator.div;
import static org.assertj.core.api.Assertions.assertThat;

import forms.DropdownQuestionForm;
import forms.QuestionForm;
import forms.RadioButtonQuestionForm;
import forms.TextQuestionForm;
import j2html.tags.ContainerTag;
import java.util.EnumSet;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.question.types.QuestionType;
import views.admin.questions.QuestionConfig;

@RunWith(JUnitParamsRunner.class)
public class QuestionConfigTest {

  private static final ContainerTag DEFAULT_CONFIG = div();
  // TODO(https://github.com/seattle-uat/civiform/issues/395): Implement dropdown rendering.
  private static final EnumSet<QuestionType> TYPES_WITH_NO_CONFIG =
      EnumSet.of(QuestionType.NAME, QuestionType.REPEATER);

  @Test
  public void allHandledTypesHaveCustomConfig() {
    assertThat(QuestionConfig.buildQuestionConfig(QuestionType.TEXT, new TextQuestionForm()))
        .toString()
        .contains("text-question-min-length-input");

    assertThat(QuestionConfig.buildQuestionConfig(QuestionType.ADDRESS, new QuestionForm()))
        .toString()
        .contains("address-question-default-state-select");

    assertThat(
            QuestionConfig.buildQuestionConfig(QuestionType.CHECKBOX, new DropdownQuestionForm()))
        .toString()
        .contains("multi-select-question-config");

    assertThat(
            QuestionConfig.buildQuestionConfig(QuestionType.DROPDOWN, new DropdownQuestionForm()))
        .toString()
        .contains("single-select-question-config");

    assertThat(QuestionConfig.buildQuestionConfig(QuestionType.NUMBER, new QuestionForm()))
        .toString()
        .contains("number-question-min-value-input");

    assertThat(
            QuestionConfig.buildQuestionConfig(
                QuestionType.RADIO_BUTTON, new RadioButtonQuestionForm()))
        .toString()
        .contains("single-select-question-config");
  }

  @Test
  @Parameters(method = "defaultTypes")
  public void unhandledQuestionTypesDefaultToDefaultConfig(QuestionType type) {
    assertThat(QuestionConfig.buildQuestionConfig(type, new QuestionForm()))
        .isEqualTo(DEFAULT_CONFIG);
  }

  private EnumSet<QuestionType> defaultTypes() {
    return TYPES_WITH_NO_CONFIG;
  }
}
