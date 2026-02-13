package mapping.admin.programs;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import models.CategoryModel;
import models.ProgramNotificationPreference;
import models.TrustedIntermediaryGroupModel;
import services.program.ProgramDefinition;
import services.program.ProgramType;
import views.admin.programs.ProgramEditPageViewModel;
import views.admin.programs.ProgramEditStatus;
import views.admin.programs.ProgramNewOnePageViewModel;

/** Maps data to the ProgramEditPageViewModel. */
public final class ProgramEditPageMapper {

  public ProgramEditPageViewModel map(
      ProgramDefinition program,
      ProgramEditStatus editStatus,
      ImmutableList<CategoryModel> categories,
      List<TrustedIntermediaryGroupModel> tiGroups,
      boolean externalProgramCardsEnabled,
      String baseUrl,
      Optional<String> errorMessage) {
    ProgramType programType = program.programType();
    boolean isDefault = programType.equals(ProgramType.DEFAULT);
    boolean isPreScreener = programType.equals(ProgramType.PRE_SCREENER_FORM);
    boolean isExternal = programType.equals(ProgramType.EXTERNAL);

    ImmutableList<ProgramNewOnePageViewModel.CategoryOption> categoryOptions =
        buildCategoryOptions(categories);

    ImmutableList<ProgramNewOnePageViewModel.TiGroupOption> tiGroupOptions =
        buildTiGroupOptions(tiGroups);

    ImmutableList<ProgramEditPageViewModel.ApplicationStepData> applicationSteps =
        program.applicationSteps().stream()
            .map(
                step ->
                    ProgramEditPageViewModel.ApplicationStepData.builder()
                        .title(step.getTitle().getDefault())
                        .description(step.getDescription().getDefault())
                        .build())
            .collect(ImmutableList.toImmutableList());

    return ProgramEditPageViewModel.builder()
        .programName(program.localizedName().getDefault())
        .programId(program.id())
        .editStatus(editStatus.name())
        .externalProgramCardsEnabled(externalProgramCardsEnabled)
        .baseUrl(baseUrl)
        .adminName(program.adminName())
        .adminDescription(program.adminDescription())
        .displayName(program.localizedName().getDefault())
        .shortDescription(program.localizedShortDescription().getDefault())
        .displayDescription(program.localizedDescription().getDefault())
        .externalLink(program.externalLink())
        .confirmationMessage(program.localizedConfirmationMessage().getDefault())
        .displayMode(program.displayMode().getValue())
        .programTypeValue(programType.getValue())
        .eligibilityIsGating(program.eligibilityIsGating())
        .loginOnly(program.loginOnly())
        .notificationPreferences(
            program.notificationPreferences().stream()
                .map(ProgramNotificationPreference::getValue)
                .collect(ImmutableList.toImmutableList()))
        .selectedTiGroups(program.acls().getTiProgramViewAcls())
        .selectedCategories(
            program.categories().stream()
                .map(c -> c.getId())
                .collect(ImmutableList.toImmutableList()))
        .applicationSteps(applicationSteps)
        .categoryOptions(categoryOptions)
        .tiGroupOptions(tiGroupOptions)
        .defaultNotificationPreferenceValue(
            ProgramNotificationPreference.EMAIL_PROGRAM_ADMIN_ALL_SUBMISSIONS.getValue())
        .isDefaultProgram(isDefault)
        .isPreScreenerForm(isPreScreener)
        .isExternalProgram(isExternal)
        .errorMessage(errorMessage)
        .build();
  }

  private ImmutableList<ProgramNewOnePageViewModel.CategoryOption> buildCategoryOptions(
      ImmutableList<CategoryModel> categories) {
    return categories.stream()
        .map(
            cat ->
                ProgramNewOnePageViewModel.CategoryOption.builder()
                    .id(cat.getId())
                    .name(cat.getDefaultName())
                    .build())
        .collect(ImmutableList.toImmutableList());
  }

  private ImmutableList<ProgramNewOnePageViewModel.TiGroupOption> buildTiGroupOptions(
      List<TrustedIntermediaryGroupModel> tiGroups) {
    return tiGroups.stream()
        .map(
            tiGroup ->
                ProgramNewOnePageViewModel.TiGroupOption.builder()
                    .id(tiGroup.id)
                    .name(tiGroup.getName())
                    .build())
        .collect(ImmutableList.toImmutableList());
  }
}
