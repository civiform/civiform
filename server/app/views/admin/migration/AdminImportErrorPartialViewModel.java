package views.admin.migration;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;

/** ViewModel for the import-error htmx partial. */
@Data
@Builder
public final class AdminImportErrorPartialViewModel implements BaseViewModel {

  // Alert heading. Null hides the heading.
  private final String title;

  // Error text, one paragraph per line. Rendered as a bulleted list when there is more than one
  // line.
  private final ImmutableList<String> errorLines;

  /** "Try again" button redirect target (the import page). */
  public String getTryAgainUrl() {
    return routes.AdminImportController.index().url();
  }
}
