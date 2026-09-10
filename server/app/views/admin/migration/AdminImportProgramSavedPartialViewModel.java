package views.admin.migration;

import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;

/** ViewModel for the program-saved htmx partial (Thymeleaf). */
@Data
@Builder
public final class AdminImportProgramSavedPartialViewModel implements BaseViewModel {

  // Display name of the saved program, rendered in the success alert.
  private final String programName;

  // "View program" button redirect target (the saved program's block editor).
  private final String viewProgramUrl;

  // "Import another program" button redirect target (the import page).
  private final String importAnotherProgramUrl;
}
