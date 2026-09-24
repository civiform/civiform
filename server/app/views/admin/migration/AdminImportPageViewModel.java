package views.admin.migration;

import controllers.admin.routes;
import lombok.Builder;
import lombok.Data;
import services.RandomStringUtils;
import views.BaseViewModel;

/** ViewModel for the program import page. */
@Data
@Builder
public final class AdminImportPageViewModel implements BaseViewModel {

  /** "Back to all programs" link target. */
  public String getBackUrl() {
    return routes.AdminProgramController.index().url();
  }

  /** htmx endpoint that parses the pasted program JSON and renders the preview partial. */
  public String getHxImportProgramUrl() {
    return routes.AdminImportController.hxImportProgram().url();
  }

  /**
   * Generates a random field id for fields without an explicit id (labels need an id to stay
   * associated with their inputs).
   */
  public String randomFieldId() {
    return RandomStringUtils.randomAlphabetic(8);
  }
}
