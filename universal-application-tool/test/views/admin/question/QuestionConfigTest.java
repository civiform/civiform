package views.admin.question;

import static j2html.TagCreator.div;
import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.stubMessagesApi;

import com.google.common.collect.ImmutableSet;
import forms.AddressQuestionForm;
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
    assertThat(QuestionConfig.buildQuestionConfig(new TextQuestionForm(), messages))
        .toString()
        .contains("text-question-min-length-input");

    assertThat(QuestionConfig.buildQuestionConfig(new AddressQuestionForm(), messages))
        .toString()
        .contains("address-question-default-state-select");

    assertThat(QuestionConfig.buildQuestionConfig(new DropdownQuestionForm(), messages))
        .toString()
        .contains("multi-select-question-config");

    assertThat(QuestionConfig.buildQuestionConfig(new DropdownQuestionForm(), messages))
        .toString()
        .contains("single-select-question-config");

    assertThat(QuestionConfig.buildQuestionConfig(new IdQuestionForm(), messages))
        .toString()
        .contains("id-question-min-length-input");

    assertThat(QuestionConfig.buildQuestionConfig(new NumberQuestionForm(), messages))
        .toString()
        .contains("number-question-min-value-input");

    assertThat(QuestionConfig.buildQuestionConfig(new RadioButtonQuestionForm(), messages))
        .toString()
        .contains("single-select-question-config");

    assertThat(QuestionConfig.buildQuestionConfig(new EnumeratorQuestionForm(), messages))
        .toString()
        .contains("enumerator-question-config");
  }

  @Test
  public void unhandledQuestionTypes_defaultsToDefaultConfig() {
    assertThat(QuestionConfig.buildQuestionConfig(new FileUploadQuestionForm(), messages))
        .isEqualTo(DEFAULT_CONFIG);

    assertThat(QuestionConfig.buildQuestionConfig(new NameQuestionForm(), messages))
        .isEqualTo(DEFAULT_CONFIG);
  }
}
