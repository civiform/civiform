package mapping.admin.migration;

import controllers.admin.routes;
import views.admin.migration.AdminImportPageViewModel;

/** Maps data to the AdminImportPageViewModel. */
public final class AdminImportPageMapper {

  /** Builds the view model for the program import page. */
  public AdminImportPageViewModel map() {
    return AdminImportPageViewModel.builder()
        .backUrl(routes.AdminProgramController.index().url())
        .hxImportProgramUrl(routes.AdminImportController.hxImportProgram().url())
        .build();
  }
}
