package views.admin.questions;

import java.util.List;
import lombok.Builder;
import views.admin.BaseViewModel;

@Builder
public record MapQuestionSettingsFiltersPartialViewModel(
    List<String> possibleKeys, long currentIndex) implements BaseViewModel {}
