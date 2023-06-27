package forms.translation;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.LocalizedQuestionOption;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinitionConfig;
import services.question.types.MultiOptionQuestionDefinitionConfig.MultiOptionQuestionType;
import services.question.types.QuestionDefinition;

public class MultiOptionQuestionTranslationFormTest {

  @Test
  public void buildsQuestion_newLocale_savesUpdates() throws Exception {
    MultiOptionQuestionDefinitionConfig config =
        MultiOptionQuestionDefinitionConfig.builder()
            .setMultiOptionQuestionType(MultiOptionQuestionType.RADIO_BUTTON)
            .setName("test")
            .setDescription("desc")
            .setQuestionText(LocalizedStrings.empty())
            .setQuestionHelpText(LocalizedStrings.empty())
            .setQuestionOptions(
                ImmutableList.of(
                    QuestionOption.create(1L, LocalizedStrings.withDefaultValue("other"))))
            .build();
    QuestionDefinition question =
        new MultiOptionQuestionDefinition(
            config.questionDefinitionConfig(),
            config.questionOptions(),
            config.multiOptionQuestionType());

    MultiOptionQuestionTranslationForm form = new MultiOptionQuestionTranslationForm();
    form.setOptions(ImmutableList.of("new"));
    MultiOptionQuestionDefinition updated =
        (MultiOptionQuestionDefinition) form.builderWithUpdates(question, Locale.CHINA).build();

    assertThat(updated.getOptionsForLocale(Locale.CHINA))
        .containsExactly(LocalizedQuestionOption.create(1L, 1L, "new", Locale.CHINA));
  }

  @Test
  public void buildsQuestion_existingLocale_savesUpdates() throws Exception {
    MultiOptionQuestionDefinitionConfig config =
        MultiOptionQuestionDefinitionConfig.builder()
            .setMultiOptionQuestionType(MultiOptionQuestionType.RADIO_BUTTON)
            .setName("test")
            .setDescription("desc")
            .setQuestionText(LocalizedStrings.empty())
            .setQuestionHelpText(LocalizedStrings.empty())
            .setQuestionOptions(
                ImmutableList.of(
                    QuestionOption.create(1L, LocalizedStrings.of(Locale.FRANCE, "existing"))))
            .build();
    QuestionDefinition question =
        new MultiOptionQuestionDefinition(
            config.questionDefinitionConfig(),
            config.questionOptions(),
            config.multiOptionQuestionType());

    MultiOptionQuestionTranslationForm form = new MultiOptionQuestionTranslationForm();
    form.setOptions(ImmutableList.of("new"));
    MultiOptionQuestionDefinition updated =
        (MultiOptionQuestionDefinition) form.builderWithUpdates(question, Locale.FRANCE).build();

    assertThat(updated.getOptionsForLocale(Locale.FRANCE))
        .containsExactly(LocalizedQuestionOption.create(1L, 1L, "new", Locale.FRANCE));
  }
}
