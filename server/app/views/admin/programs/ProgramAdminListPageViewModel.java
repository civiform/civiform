package views.admin.programs;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public class ProgramAdminListPageViewModel implements BaseViewModel {
  private final ImmutableList<ProgramCardData> programs;

  @Data
  @Builder
  public static class ProgramCardData {
    private final String programName;
    private final String adminName;
    private final String programType;
    private final long lastUpdatedMillis;
    private final long programId;
    private final boolean isExternalProgram;
    private final String slug;
    private final String baseUrl;

    public String getApplicationsUrl() {
      return routes.AdminApplicationController.index(
              programId,
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty(),
              Optional.empty())
          .url();
    }

    public Optional<String> getShareLink() {
      return isExternalProgram
          ? Optional.empty()
          : Optional.of(
              baseUrl + controllers.applicant.routes.ApplicantProgramsController.show(slug).url());
    }
  }
}
