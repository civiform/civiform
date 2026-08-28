package mapping.admin.migration;

import static com.google.common.base.Preconditions.checkNotNull;

import controllers.admin.routes;
import views.admin.migration.AdminImportErrorPartialViewModel;

/** Maps data to the AdminImportErrorPartialViewModel. */
public final class AdminImportErrorPartialMapper {

  /** Builds the view model for an error that occurred while processing the program data. */
  public AdminImportErrorPartialViewModel map(String title, String errorMessage) {
    return AdminImportErrorPartialViewModel.builder()
        .title(checkNotNull(title))
        .errorMessage(checkNotNull(errorMessage))
        .tryAgainUrl(routes.AdminImportController.index().url())
        .build();
  }
}
