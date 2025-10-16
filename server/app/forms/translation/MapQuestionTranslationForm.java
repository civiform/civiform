package forms.translation;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import services.LocalizedStrings;
import services.question.MapSettingType;
import services.question.QuestionSetting;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;

/** Form for updating translation for map questions. */
@Setter
@Getter
public class MapQuestionTranslationForm extends QuestionTranslationForm {

  // These will be in the same order as the filter settings.
  private List<String> filters;
  private String tagDisplayName;
  private String tagText;

  public MapQuestionTranslationForm() {
    super();
    this.filters = new ArrayList<>();
    this.tagDisplayName = "";
    this.tagText = "";
  }

  @Override
  public QuestionDefinitionBuilder builderWithUpdates(
      QuestionDefinition toUpdate, Locale updatedLocale) throws UnsupportedQuestionTypeException {
    QuestionDefinitionBuilder partiallyUpdated = super.builderWithUpdates(toUpdate, updatedLocale);
    ImmutableSet.Builder<QuestionSetting> updatedSettingsBuilder = ImmutableSet.builder();

    ImmutableSet<QuestionSetting> settings =
        toUpdate.getQuestionSettings().orElse(ImmutableSet.of());
    int i = 0;
    for (QuestionSetting setting : settings) {
      if (setting.settingType().equals(MapSettingType.LOCATION_FILTER_GEO_JSON_KEY)) {
        LocalizedStrings updatedTranslations =
            setting
                .localizedSettingDisplayName()
                .orElse(LocalizedStrings.of())
                .updateTranslation(updatedLocale, this.filters.get(i));
        updatedSettingsBuilder.add(
            setting.toBuilder()
                .setLocalizedSettingDisplayName(Optional.of(updatedTranslations))
                .build());
        i++;
      } else if (setting.settingType().equals(MapSettingType.LOCATION_TAG_GEO_JSON_KEY)) {
        LocalizedStrings updatedDisplayNameTranslations =
            setting
                .localizedSettingDisplayName()
                .orElse(LocalizedStrings.of())
                .updateTranslation(updatedLocale, this.tagDisplayName);
        LocalizedStrings updatedTextTranslations =
            setting
                .localizedSettingText()
                .orElse(LocalizedStrings.of())
                .updateTranslation(updatedLocale, this.tagText);
        updatedSettingsBuilder.add(
            setting.toBuilder()
                .setLocalizedSettingDisplayName(Optional.of(updatedDisplayNameTranslations))
                .setLocalizedSettingText(Optional.of(updatedTextTranslations))
                .build());
      } else {
        updatedSettingsBuilder.add(setting);
      }
    }

    return partiallyUpdated.setQuestionSettings(updatedSettingsBuilder.build());
  }
}
