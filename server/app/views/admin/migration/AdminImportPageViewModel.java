package views.admin.migration;

import controllers.admin.routes;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

@Data
@Builder
public final class AdminImportPageViewModel implements BaseViewModel {

  private final int maxTextLength;

  public String getBackToProgramsUrl() {
    return routes.AdminProgramController.index().url();
  }

  public String getHxImportProgramUrl() {
    return routes.AdminImportController.hxImportProgram().url();
  }
}
