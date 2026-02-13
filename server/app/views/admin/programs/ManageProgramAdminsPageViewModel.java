package views.admin.programs;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public class ManageProgramAdminsPageViewModel implements BaseViewModel {
  private final String programName;
  private final long programId;
  private final ImmutableList<AdminRow> existingAdmins;
  private final Optional<String> errorMessage;

  public String getBackUrl() {
    return routes.AdminProgramController.index().url();
  }

  public String getAddAdminUrl() {
    return routes.ProgramAdminManagementController.add(programId).url();
  }

  @Data
  @Builder
  public static class AdminRow {
    private final String email;
    private final long programId;

    public String getDeleteUrl() {
      return routes.ProgramAdminManagementController.delete(programId).url();
    }
  }
}
