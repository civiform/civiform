package views.admin.questions;

import forms.MapQuestionForm;
import forms.QuestionSettingUtils;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import lombok.Builder;
import services.question.QuestionSetting;
import views.admin.BaseViewModel;

@Builder
public record MapQuestionSettingsPartialViewModel(
    OptionalInt maxLocationSelections,
    QuestionSetting locationName,
    QuestionSetting locationAddress,
    QuestionSetting locationDetailsUrl,
    List<QuestionSetting> filters,
    Set<String> possibleKeys)
    implements BaseViewModel {

  /** Creates an empty MapQuestionSettingsPartialViewModel with default placeholders. */
  public static MapQuestionSettingsPartialViewModel withEmptyDefaults(Set<String> possibleKeys) {
    return new MapQuestionSettingsPartialViewModel(
        OptionalInt.empty(),
        QuestionSettingUtils.emptySettingWithDisplayName(MapQuestionForm.LOCATION_NAME_DISPLAY),
        QuestionSettingUtils.emptySettingWithDisplayName(MapQuestionForm.LOCATION_ADDRESS_DISPLAY),
        QuestionSettingUtils.emptySettingWithDisplayName(
            MapQuestionForm.LOCATION_DETAILS_URL_DISPLAY),
        MapQuestionForm.emptyFilters(),
        possibleKeys);
  }
}
