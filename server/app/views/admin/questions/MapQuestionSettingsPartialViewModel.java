package views.admin.questions;

import java.util.Set;
import lombok.Builder;
import views.admin.BaseViewModel;

@Builder
public record MapQuestionSettingsPartialViewModel(Set<String> possibleKeys)
    implements BaseViewModel {}
