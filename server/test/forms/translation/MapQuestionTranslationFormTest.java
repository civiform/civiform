package forms.translation;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.LocalizedQuestionSetting;
import services.question.MapSettingType;
import services.question.QuestionSetting;
import services.question.types.MapQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;

public class MapQuestionTranslationFormTest {

  @Test
  public void buildsQuestion_newLocale_savesUpdates() throws Exception {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("test")
            .setDescription("desc")
            .setQuestionText(LocalizedStrings.empty())
            .setQuestionHelpText(LocalizedStrings.empty())
            .setQuestionSettings(
                ImmutableSet.of(
                    QuestionSetting.create(
                        "filter1",
                        MapSettingType.LOCATION_FILTER_GEO_JSON_KEY,
                        Optional.of(LocalizedStrings.withDefaultValue("Original Filter")))))
            .build();
    QuestionDefinition question = new MapQuestionDefinition(config);

    MapQuestionTranslationForm form = new MapQuestionTranslationForm();
    form.setFilters(ImmutableList.of("Translated Filter"));
    MapQuestionDefinition updated =
        (MapQuestionDefinition) form.builderWithUpdates(question, Locale.CHINA).build();

    assertThat(updated.getSettingsForLocaleOrDefault(Locale.CHINA).orElse(ImmutableSet.of()))
        .containsExactly(
            LocalizedQuestionSetting.create(
                /* settingValue= */ "filter1",
                /* settingType= */ MapSettingType.LOCATION_FILTER_GEO_JSON_KEY,
                /* settingDisplayName= */ "Translated Filter",
                /* locale= */ Locale.CHINA));
  }

  @Test
  public void buildsQuestion_existingLocale_savesUpdates() throws Exception {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("test")
            .setDescription("desc")
            .setQuestionText(LocalizedStrings.empty())
            .setQuestionHelpText(LocalizedStrings.empty())
            .setQuestionSettings(
                ImmutableSet.of(
                    QuestionSetting.create(
                        "filter1",
                        MapSettingType.LOCATION_FILTER_GEO_JSON_KEY,
                        Optional.of(LocalizedStrings.of(Locale.FRANCE, "Existing Filter")))))
            .build();
    QuestionDefinition question = new MapQuestionDefinition(config);

    MapQuestionTranslationForm form = new MapQuestionTranslationForm();
    form.setFilters(ImmutableList.of("Updated Filter"));
    MapQuestionDefinition updated =
        (MapQuestionDefinition) form.builderWithUpdates(question, Locale.FRANCE).build();

    assertThat(updated.getSettingsForLocaleOrDefault(Locale.FRANCE).orElse(ImmutableSet.of()))
        .containsExactly(
            LocalizedQuestionSetting.create(
                /* settingValue= */ "filter1",
                /* settingType= */ MapSettingType.LOCATION_FILTER_GEO_JSON_KEY,
                /* settingDisplayName= */ "Updated Filter",
                /* locale= */ Locale.FRANCE));
  }
}
