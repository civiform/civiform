package mapping.admin.migration;

import static com.google.common.base.Preconditions.checkNotNull;

import controllers.admin.routes;
import views.admin.migration.AdminImportProgramSavedPartialViewModel;

/** Maps data to the AdminImportProgramSavedPartialViewModel. */
public final class AdminImportProgramSavedPartialMapper {

  /** Builds the view model for the message saying the program was successfully saved. */
  public AdminImportProgramSavedPartialViewModel map(String programName, Long programId) {
    return AdminImportProgramSavedPartialViewModel.builder()
        .programName(checkNotNull(programName))
        .viewProgramUrl(routes.AdminProgramBlocksController.index(programId).url())
        .importAnotherProgramUrl(routes.AdminImportController.index().url())
        .build();
  }
}
