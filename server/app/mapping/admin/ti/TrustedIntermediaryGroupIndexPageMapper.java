package mapping.admin.ti;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import models.TrustedIntermediaryGroupModel;
import views.admin.ti.TrustedIntermediaryGroupIndexPageViewModel;
import views.admin.ti.TrustedIntermediaryGroupIndexPageViewModel.Group;
import views.admin.ti.TrustedIntermediaryGroupIndexPageViewModel.SortOption;
import views.components.QuestionSortOption;

/** Maps the trusted intermediary groups to the TrustedIntermediaryGroupIndexPageViewModel. */
public final class TrustedIntermediaryGroupIndexPageMapper {

  /**
   * Maps the groups and the flash state of the last create attempt to the view model.
   *
   * @param tiGroups the groups to list, in display order
   * @param providedName the name submitted by a failed create, kept sticky in the form
   * @param providedDescription the description submitted by a failed create, kept sticky in the
   *     form
   * @param errorMessage the flash error of a failed create, shown as an error toast
   */
  public TrustedIntermediaryGroupIndexPageViewModel map(
      List<TrustedIntermediaryGroupModel> tiGroups,
      Optional<String> providedName,
      Optional<String> providedDescription,
      Optional<String> errorMessage) {
    ImmutableList<Group> groups =
        tiGroups.stream().map(this::buildGroup).collect(ImmutableList.toImmutableList());

    return TrustedIntermediaryGroupIndexPageViewModel.builder()
        .createGroupUrl(
            controllers.admin.routes.TrustedIntermediaryManagementController.create().url())
        .providedName(providedName.orElse(""))
        .providedDescription(providedDescription.orElse(""))
        // The legacy ToastMessage.errorNonLocalized prefixed the flash text.
        .errorMessage(errorMessage.map(error -> "Error: " + error).orElse(null))
        .sortOptions(buildSortOptions())
        .groups(groups)
        .build();
  }

  private ImmutableList<SortOption> buildSortOptions() {
    ImmutableList<QuestionSortOption> sortOptions =
        ImmutableList.of(QuestionSortOption.TI_NAME, QuestionSortOption.TI_NUM_MEMBERS);
    ImmutableList.Builder<SortOption> builder = ImmutableList.builder();
    boolean first = true;

    for (QuestionSortOption sortOption : sortOptions) {
      for (var optionValue : sortOption.getSelectOptions()) {
        builder.add(
            SortOption.builder()
                .value(optionValue.value())
                .label(optionValue.label())
                // The first option (name A-Z) is the default sort order.
                .selected(first)
                .build());
        first = false;
      }
    }

    return builder.build();
  }

  private Group buildGroup(TrustedIntermediaryGroupModel tiGroup) {
    return Group.builder()
        .name(tiGroup.getName())
        .description(tiGroup.getDescription())
        .memberCount(tiGroup.getMembersCount())
        .clientCount(tiGroup.getManagedAccountsCount())
        .editUrl(
            controllers.admin.routes.TrustedIntermediaryManagementController.edit(tiGroup.id).url())
        .deleteUrl(
            controllers.admin.routes.TrustedIntermediaryManagementController.delete(tiGroup.id)
                .url())
        .build();
  }
}
