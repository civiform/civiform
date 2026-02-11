package views.admin.ti;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;
import views.components.QuestionSortOption;

@Data
@Builder
public class TrustedIntermediaryGroupListPageViewModel implements BaseViewModel {
  // List of all TI groups.
  private final ImmutableList<models.TrustedIntermediaryGroupModel> tiGroups;

  // Value for the name field in the create form (for sticky form).
  private final String providedName;

  // Value for the description field in the create form (for sticky form).
  private final String providedDescription;

  // Sort options for the group list, as QuestionSortOption enums.
  private final ImmutableList<QuestionSortOption> sortOptions;

  // Currently selected sort option.
  private final String selectedSortOption;
}
