package views.admin.migration;

import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;

/** ViewModel for the import-error htmx partial (Thymeleaf). */
@Data
@Builder
public final class AdminImportErrorPartialViewModel implements BaseViewModel {

  // Alert heading.
  private final String title;

  // Alert body text.
  private final String errorMessage;

  // "Try again" button redirect target (the import page).
  private final String tryAgainUrl;
}
