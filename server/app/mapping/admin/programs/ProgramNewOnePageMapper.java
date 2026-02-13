package mapping.admin.programs;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import models.CategoryModel;
import models.ProgramNotificationPreference;
import models.TrustedIntermediaryGroupModel;
import views.admin.programs.ProgramNewOnePageViewModel;

/** Maps data to the ProgramNewOnePageViewModel. */
public final class ProgramNewOnePageMapper {

  public ProgramNewOnePageViewModel map(
      List<CategoryModel> categories,
      List<TrustedIntermediaryGroupModel> tiGroups,
      boolean externalProgramCardsEnabled,
      Optional<String> errorMessage) {
    ImmutableList<ProgramNewOnePageViewModel.CategoryOption> categoryOptions =
        categories.stream()
            .map(
                cat ->
                    ProgramNewOnePageViewModel.CategoryOption.builder()
                        .id(cat.getId())
                        .name(cat.getDefaultName())
                        .build())
            .collect(ImmutableList.toImmutableList());

    ImmutableList<ProgramNewOnePageViewModel.TiGroupOption> tiGroupOptions =
        tiGroups.stream()
            .map(
                tiGroup ->
                    ProgramNewOnePageViewModel.TiGroupOption.builder()
                        .id(tiGroup.id)
                        .name(tiGroup.getName())
                        .build())
            .collect(ImmutableList.toImmutableList());

    return ProgramNewOnePageViewModel.builder()
        .externalProgramCardsEnabled(externalProgramCardsEnabled)
        .categoryOptions(categoryOptions)
        .tiGroupOptions(tiGroupOptions)
        .defaultNotificationPreferenceValue(
            ProgramNotificationPreference.EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS.getValue())
        .errorMessage(errorMessage)
        .build();
  }
}
