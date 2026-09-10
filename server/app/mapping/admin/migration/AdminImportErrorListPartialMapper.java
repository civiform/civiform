package mapping.admin.migration;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import views.admin.migration.AdminImportErrorListPartialViewModel;

/** Maps data to the AdminImportErrorListPartialViewModel. */
public final class AdminImportErrorListPartialMapper {

  /**
   * Builds the view model for validation errors rendered as one bulleted paragraph per error. The
   * error message is split on sentence boundaries, as in the legacy view.
   */
  public AdminImportErrorListPartialViewModel map(String title, String errorMessage) {
    ImmutableList<String> errorLines =
        Splitter.on(". ")
            .splitToStream(checkNotNull(errorMessage))
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .collect(ImmutableList.toImmutableList());

    return AdminImportErrorListPartialViewModel.builder()
        .title(title)
        .errorLines(errorLines)
        .tryAgainUrl(routes.AdminImportController.index().url())
        .build();
  }
}
