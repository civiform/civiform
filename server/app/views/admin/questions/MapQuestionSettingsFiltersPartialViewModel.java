package views.admin.questions;

import java.util.Set;
import lombok.Builder;
import views.admin.BaseViewModel;

@Builder
public record MapQuestionSettingsFiltersPartialViewModel(Set<String> possibleKeys, int currentIndex)
    implements BaseViewModel {}
