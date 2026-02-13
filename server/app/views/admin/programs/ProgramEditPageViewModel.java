package views.admin.programs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import controllers.admin.routes;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import modules.MainModule;
import views.admin.BaseViewModel;

@Data
@Builder
public class ProgramEditPageViewModel implements BaseViewModel {
  private final String programName;
  private final long programId;
  private final String editStatus;
  private final boolean externalProgramCardsEnabled;
  private final String baseUrl;

  // Pre-populated field values
  private final String adminName;
  private final String adminDescription;
  private final String displayName;
  private final String shortDescription;
  private final String displayDescription;
  private final String externalLink;
  private final String confirmationMessage;
  private final String displayMode;
  private final String programTypeValue;
  private final boolean eligibilityIsGating;
  private final boolean loginOnly;
  private final ImmutableList<String> notificationPreferences;
  private final ImmutableSet<Long> selectedTiGroups;
  private final ImmutableList<Long> selectedCategories;
  private final ImmutableList<ApplicationStepData> applicationSteps;

  // Options for selectable fields
  private final ImmutableList<ProgramNewOnePageViewModel.CategoryOption> categoryOptions;
  private final ImmutableList<ProgramNewOnePageViewModel.TiGroupOption> tiGroupOptions;
  private final String defaultNotificationPreferenceValue;

  // Program type state for disabling fields
  private final boolean isDefaultProgram;
  private final boolean isPreScreenerForm;
  private final boolean isExternalProgram;

  private final Optional<String> errorMessage;

  public String getFormActionUrl() {
    return routes.AdminProgramController.update(programId, editStatus).url();
  }

  public Optional<String> getProgramUrl() {
    if (isExternalProgram && externalProgramCardsEnabled) {
      return Optional.empty();
    }
    return Optional.of(
        baseUrl
            + controllers.applicant.routes.ApplicantProgramsController.show(
                    MainModule.SLUGIFIER.slugify(adminName))
                .url());
  }

  public Optional<String> getManageQuestionsUrl() {
    return (isDefaultProgram || isPreScreenerForm)
        ? Optional.of(controllers.admin.routes.AdminProgramBlocksController.index(programId).url())
        : Optional.empty();
  }

  @Data
  @Builder
  public static class ApplicationStepData {
    private final String title;
    private final String description;
  }
}
