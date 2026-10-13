package views.admin.migration;

import controllers.admin.routes;
import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;

/** ViewModel for the program-saved htmx partial (Thymeleaf). */
@Data
@Builder
public final class AdminImportProgramSavedPartialViewModel implements BaseViewModel {

  // Display name of the saved program, rendered in the success alert.
  private final String programName;

  // Id of the saved program.
  private final long programId;

  /** "View program" button redirect target (the saved program's block editor). */
  public String getViewProgramUrl() {
    return routes.AdminProgramBlocksController.index(programId).url();
  }

  /** "Import another program" button redirect target (the import page). */
  public String getImportAnotherProgramUrl() {
    return routes.AdminImportController.index().url();
  }
}
