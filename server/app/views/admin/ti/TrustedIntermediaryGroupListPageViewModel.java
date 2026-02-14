package views.admin.ti;

import com.google.common.collect.ImmutableList;
import java.util.Map;
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

  // Form action URL for creating a new TI group.
  private final String newTiGroupUrl;

  // Map of groupIds to edit URLs
  private final Map<Long, String> editGroupUrls;

  // Map of groupIds to delete URLs
  private final Map<Long, String> deleteGroupUrls;

  // URL for the TI group sorting
  private final String tiGroupListUrl;
}
