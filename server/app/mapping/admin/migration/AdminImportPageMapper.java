package mapping.admin.migration;

import views.admin.migration.AdminImportPageViewModel;

/** Maps data to the AdminImportPageViewModel. */
public final class AdminImportPageMapper {

  public AdminImportPageViewModel map(int maxTextLength) {
    return AdminImportPageViewModel.builder().maxTextLength(maxTextLength).build();
  }
}
