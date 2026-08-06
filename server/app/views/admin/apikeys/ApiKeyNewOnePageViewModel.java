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

  private final FormField keyNameField;
  private final FormField expirationField;
  private final FormField subnetField;

  // Set when the submitted form granted access to no program at all.
  private final boolean showProgramsError;
  private final ImmutableList<ProgramCheckbox> programCheckboxes;

  /** A labeled input, along with the value and error the submitted form carried. */
  @Data
  @Builder
  public static final class FormField {
    private final String id;
    // Id of the sibling error container, always "<id>-errors".
    private final String errorsId;
    private final String name;
    private final String label;
    // Input type, either "text" or "date".
    private final String type;
    private final String value;

    private final boolean hasError;
    // Null when there is no error. Already prefixed with "Error: ".
    private final String errorMessage;
  }

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
