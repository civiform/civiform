package views.admin.migration;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;

/** ViewModel for the bulleted import-error htmx partial (Thymeleaf). */
@Data
@Builder
public final class AdminImportErrorListPartialViewModel implements BaseViewModel {

  // Alert heading. Null hides the heading, as in the legacy view.
  private final String title;

  // One trimmed error sentence per bulleted paragraph.
  private final ImmutableList<String> errorLines;

  // "Try again" button redirect target (the import page).
  private final String tryAgainUrl;
}
