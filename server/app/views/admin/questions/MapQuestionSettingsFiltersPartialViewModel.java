package views.admin.questions;

import forms.MapQuestionForm;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import lombok.Builder;
import views.admin.BaseViewModel;

@Builder
public record MapQuestionSettingsFiltersPartialViewModel(
    Set<String> possibleKeys)
    implements BaseViewModel {
}
