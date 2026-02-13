package views.admin.migration;

import controllers.admin.routes;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public final class AdminExportPageViewModel implements BaseViewModel {

  private final String programAdminName;
  private final String programJson;

  public String getDownloadJsonUrl() {
    return routes.AdminExportController.downloadJson(programAdminName).url();
  }

  public String getBackToProgramsUrl() {
    return routes.AdminProgramController.index().url();
  }
}
