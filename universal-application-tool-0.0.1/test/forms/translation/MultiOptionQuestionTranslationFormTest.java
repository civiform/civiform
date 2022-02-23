package forms.translation;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.LocalizedQuestionOption;
import services.question.QuestionOption;
import services.question.types.QuestionDefinition;
import services.question.types.RadioButtonQuestionDefinition;

public class MultiOptionQuestionTranslationFormTest {

  @Test
  public void buildsQuestion_newLocale_savesUpdates() throws Exception {
    QuestionDefinition question =
        new RadioButtonQuestionDefinition(
            "test",
            Optional.empty(),
            "desc",
            LocalizedStrings.empty(),
            LocalizedStrings.empty(),
            ImmutableList.of(
                QuestionOption.create(1L, LocalizedStrings.withDefaultValue("other"))));

    MultiOptionQuestionTranslationForm form = new MultiOptionQuestionTranslationForm();
    form.setOptions(ImmutableList.of("new"));
    RadioButtonQuestionDefinition updated =
        (RadioButtonQuestionDefinition) form.builderWithUpdates(question, Locale.CHINA).build();

    assertThat(updated.getOptionsForLocale(Locale.CHINA))
        .containsExactly(LocalizedQuestionOption.create(1L, 1L, "new", Locale.CHINA));
  }

  @Test
  public void buildsQuestion_existingLocale_savesUpdates() throws Exception {
    QuestionDefinition question =
        new RadioButtonQuestionDefinition(
            "test",
            Optional.empty(),
            "desc",
            LocalizedStrings.empty(),
            LocalizedStrings.empty(),
            ImmutableList.of(
                QuestionOption.create(1L, LocalizedStrings.of(Locale.FRANCE, "existing"))));

    MultiOptionQuestionTranslationForm form = new MultiOptionQuestionTranslationForm();
    form.setOptions(ImmutableList.of("new"));
    RadioButtonQuestionDefinition updated =
        (RadioButtonQuestionDefinition) form.builderWithUpdates(question, Locale.FRANCE).build();

    assertThat(updated.getOptionsForLocale(Locale.FRANCE))
        .containsExactly(LocalizedQuestionOption.create(1L, 1L, "new", Locale.FRANCE));
  }
}
