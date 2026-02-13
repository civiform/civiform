package views.admin.programs;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public class ProgramStatusesPageViewModel implements BaseViewModel {
  private final String programName;
  private final long programId;
  private final String programAdminName;
  private final boolean hasTranslatableLocales;
  private final ImmutableList<StatusData> statuses;
  private final Optional<String> successMessage;
  private final Optional<String> errorMessage;

  public String getCreateOrUpdateUrl() {
    return routes.AdminProgramStatusesController.createOrUpdate(programId).url();
  }

  public String getDeleteUrl() {
    return routes.AdminProgramStatusesController.delete(programId).url();
  }

  public Optional<String> getManageTranslationsUrl() {
    return hasTranslatableLocales
        ? Optional.of(
            routes.AdminProgramTranslationsController.redirectToFirstLocale(programAdminName).url())
        : Optional.empty();
  }

  public String numResultsText() {
    return statuses.size() == 1 ? "1 result" : String.format("%d results", statuses.size());
  }

  @Data
  @Builder
  public static class StatusData {
    private final String statusText;
    private final boolean hasEmail;
    private final String emailBody;
    private final boolean isDefault;
    private final String configuredStatusText;
    private final String modalId;
    private final String deleteModalId;
  }
}
