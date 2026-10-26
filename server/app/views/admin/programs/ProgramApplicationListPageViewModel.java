package views.admin.programs;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import services.RandomStringUtils;
import views.BaseViewModel;
import views.admin.shared.LegacyPagination;

/** ViewModel for the admin's list of applications submitted to one program. */
@Data
@Builder
public final class ProgramApplicationListPageViewModel implements BaseViewModel {

  // Program admin name; the page heading and the " - Applications" title are built from it.
  private final String programName;

  // Applications list URL without any filter params; the search form action and "Clear" link.
  private final String indexUrl;

  // Current filter values, empty strings when unset. Rendered into the search form and carried
  // through the download modal as hidden inputs.
  private final String fromDate;
  private final String untilDate;
  private final String search;
  private final String selectedApplicationStatus;

  // Value of the "Only applications without a status" filter option.
  private final String noStatusFilterOptionValue;

  // Every status ever applied to this program's applications, for the status filter. When empty
  // the filter, the bulk status form, and the status column are all hidden.
  private final ImmutableList<String> applicationStatusOptions;

  // Download button and modal are hidden for program admins when downloads are disabled.
  private final boolean showDownloadButton;

  // Opens the download modal on page load.
  private final boolean showDownloadModal;
  private final String downloadAllCsvUrl;
  private final String downloadAllJsonUrl;

  // Bulk status update form.
  private final String updateStatusesUrl;

  // Placeholder option text for the bulk status selector, from the en-US messages.
  private final String bulkStatusPlaceholder;

  // Currently active statuses, in definition order.
  private final ImmutableList<String> bulkStatusOptions;

  // Info alert text after a bulk status update, null when there is nothing to show.
  private final String statusUpdateMessage;

  // Shows the "Eligibility" column.
  private final boolean eligibilityEnabled;

  private final ImmutableList<ApplicationRow> applicationRows;

  // Null when everything fits on one page.
  private final LegacyPagination pagination;

  public boolean hasApplicationStatuses() {
    return !applicationStatusOptions.isEmpty();
  }

  /**
   * Random id for a form field, so its label stays associated with it. The legacy FieldWithLabel
   * generated one per field when no id was set.
   */
  public String randomFieldId() {
    return RandomStringUtils.randomAlphabetic(8);
  }

  /** One application in the table. */
  @Data
  @Builder
  public static final class ApplicationRow {
    // Application id; the template derives the checkbox and link ids from it.
    private final long id;

    // "<applicant name> (<application id>)".
    private final String applicantNameWithApplicationId;
    private final String viewUrl;
    private final boolean meetsEligibility;

    // Latest status text, suffixed with " (default)" for the default status, or "None".
    private final String statusText;

    // Human readable submission time, empty when the application has none.
    private final String submitTime;

    // Submitter email, empty when unknown.
    private final String submitterEmail;
  }
}
