package views.admin.apikeys;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;

/** ViewModel for the new API key page (Thymeleaf). */
@Data
@Builder
public final class ApiKeyNewOnePageViewModel implements BaseViewModel {

  // Page heading, always "Create a new API key"
  private final String title;

  // False when there is no program to grant access to yet. The legacy view had a
  // separate render method for that case, but it is the empty state of this same
  // page: same route, same title, only the form is swapped for a notice.
  private final boolean showForm;

  private final String formActionUrl;

  // Submitted field values, empty when creation has not failed validation.
  private final String keyNameValue;
  private final String expirationValue;
  private final String subnetValue;

  // Field errors, null when the field has none. Already prefixed with "Error: ".
  private final String keyNameError;
  private final String expirationError;
  private final String subnetError;

  // Set when the submitted form granted access to no program at all.
  private final boolean showProgramsError;
  private final ImmutableList<ProgramCheckbox> programCheckboxes;

  /** A checkbox granting the key read access to one program. */
  @Data
  @Builder
  public static final class ProgramCheckbox {
    // The slugified program name, which the browser tests select on.
    private final String id;
    private final String name;
    private final String label;
    private final boolean checked;
  }
}
