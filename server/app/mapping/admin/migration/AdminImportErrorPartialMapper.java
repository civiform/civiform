package mapping.admin.migration;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import views.admin.migration.AdminImportErrorPartialViewModel;

/** Maps data to the AdminImportErrorPartialViewModel. */
public final class AdminImportErrorPartialMapper {

  /** Builds the view model for a single error message. */
  public AdminImportErrorPartialViewModel map(String title, String errorMessage) {
    return AdminImportErrorPartialViewModel.builder()
        .title(title)
        .errorLines(ImmutableList.of(checkNotNull(errorMessage)))
        .build();
  }

  /**
   * Builds the view model for validation errors rendered as one bulleted paragraph per error. The
   * error message is split on sentence boundaries, as in the legacy view.
   */
  public AdminImportErrorPartialViewModel mapWithLineBreaks(String title, String errorMessage) {
    ImmutableList<String> errorLines =
        Splitter.on(". ")
            .splitToStream(checkNotNull(errorMessage))
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .collect(ImmutableList.toImmutableList());

    return AdminImportErrorPartialViewModel.builder().title(title).errorLines(errorLines).build();
  }
}
