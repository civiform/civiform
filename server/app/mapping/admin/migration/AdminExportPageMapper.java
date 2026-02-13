package mapping.admin.migration;

import views.admin.migration.AdminExportPageViewModel;

/** Maps data to the AdminExportPageViewModel. */
public final class AdminExportPageMapper {

  public AdminExportPageViewModel map(String programAdminName, String programJson) {
    return AdminExportPageViewModel.builder()
        .programAdminName(programAdminName)
        .programJson(programJson)
        .build();
  }
}
