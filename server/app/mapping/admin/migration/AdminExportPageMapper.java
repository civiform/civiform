package mapping.admin.migration;

import views.admin.migration.AdminExportPageViewModel;

/** Maps data to the AdminExportPageViewModel for the program export page. */
public final class AdminExportPageMapper {

  /**
   * Maps the exported program to the view model.
   *
   * @param adminName the admin name of the program being exported
   * @param programJson the serialized program JSON
   */
  public AdminExportPageViewModel map(String adminName, String programJson) {
    return AdminExportPageViewModel.builder()
        .backUrl(controllers.admin.routes.AdminProgramController.index().url())
        .adminName(adminName)
        .programJson(programJson)
        .downloadUrl(controllers.admin.routes.AdminExportController.downloadJson(adminName).url())
        .build();
  }
}
