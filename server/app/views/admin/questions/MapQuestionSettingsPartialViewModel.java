package views.admin.questions;

import forms.MapQuestionForm;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import views.admin.BaseViewModel;

@Builder
public record MapQuestionSettingsPartialViewModel(
    MapQuestionForm.Setting locationName,
    MapQuestionForm.Setting locationAddress,
    MapQuestionForm.Setting locationDetailsUrl,
    List<MapQuestionForm.Setting> filters,
    Set<String> possibleKeys)
    implements BaseViewModel {}
