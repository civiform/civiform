package views.admin.migration;

import controllers.admin.routes;
import lombok.Builder;
import lombok.Data;
import services.RandomStringUtils;
import views.BaseViewModel;

/** ViewModel for the program export page (Thymeleaf). */
@Data
@Builder
public final class AdminExportPageViewModel implements BaseViewModel {

  // Admin name of the program being exported, e.g. "utility-discount"
  private final String adminName;

  // Serialized program JSON, shown in the preview and posted for download
  private final String programJson;

  /** "Back to all programs" link target. */
  public String getBackUrl() {
    return routes.AdminProgramController.index().url();
  }

  /** Target of the download form POST. */
  public String getDownloadUrl() {
    return routes.AdminExportController.downloadJson(adminName).url();
  }

  /**
   * Generates a random field id for fields without an explicit id (labels need an id to stay
   * associated with their inputs).
   */
  public String randomFieldId() {
    return RandomStringUtils.randomAlphabetic(8);
  }
}
