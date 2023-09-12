package forms.translation;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.LocalizedQuestionOption;
import services.question.QuestionOption;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;

public class MultiOptionQuestionTranslationFormTest {

  @Test
  public void buildsQuestion_newLocale_savesUpdates() throws Exception {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("test")
            .setDescription("desc")
            .setQuestionText(LocalizedStrings.empty())
            .setQuestionHelpText(LocalizedStrings.empty())
            .build();
    QuestionDefinition question =
        new MultiOptionQuestionDefinition(
            config,
            ImmutableList.of(
                QuestionOption.create(1L, "opt1", LocalizedStrings.withDefaultValue("other"))),
            MultiOptionQuestionType.RADIO_BUTTON);

    MultiOptionQuestionTranslationForm form = new MultiOptionQuestionTranslationForm();
    form.setOptions(ImmutableList.of("new"));
    MultiOptionQuestionDefinition updated =
        (MultiOptionQuestionDefinition) form.builderWithUpdates(question, Locale.CHINA).build();

    assertThat(updated.getOptionsForLocale(Locale.CHINA))
        .containsExactly(LocalizedQuestionOption.create(1L, 1L, "opt1", "new", Locale.CHINA));
  }

  @Test
  public void buildsQuestion_existingLocale_savesUpdates() throws Exception {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("test")
            .setDescription("desc")
            .setQuestionText(LocalizedStrings.empty())
            .setQuestionHelpText(LocalizedStrings.empty())
            .build();
    QuestionDefinition question =
        new MultiOptionQuestionDefinition(
            config,
            ImmutableList.of(
                QuestionOption.create(1L, "opt1", LocalizedStrings.of(Locale.FRANCE, "existing"))),
            MultiOptionQuestionType.RADIO_BUTTON);

    MultiOptionQuestionTranslationForm form = new MultiOptionQuestionTranslationForm();
    form.setOptions(ImmutableList.of("new"));
    MultiOptionQuestionDefinition updated =
        (MultiOptionQuestionDefinition) form.builderWithUpdates(question, Locale.FRANCE).build();

    assertThat(updated.getOptionsForLocale(Locale.FRANCE))
        .containsExactly(LocalizedQuestionOption.create(1L, 1L, "opt1", "new", Locale.FRANCE));
  }
}
