package views.admin.migration;

import lombok.Builder;
import lombok.Data;
import services.RandomStringUtils;
import views.BaseViewModel;

/** ViewModel for the program import page (Thymeleaf). */
@Data
@Builder
public final class AdminImportPageViewModel implements BaseViewModel {

  // "Back to all programs" link target.
  private final String backUrl;

  // htmx endpoint that parses the pasted program JSON and renders the preview partial.
  private final String hxImportProgramUrl;

  /**
   * Generates a random field id for fields without an explicit id (labels need an id to stay
   * associated with their inputs).
   */
  public String randomFieldId() {
    return RandomStringUtils.randomAlphabetic(8);
  }
}
