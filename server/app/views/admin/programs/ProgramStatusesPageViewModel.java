package views.admin.programs;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import services.RandomStringUtils;
import views.BaseViewModel;

/**
 * ViewModel for the admin "Manage application statuses" page (Thymeleaf), a 1:1 port of {@link
 * ProgramStatusesView}.
 */
@Data
@Builder
public final class ProgramStatusesPageViewModel implements BaseViewModel {

  // Flash toasts (success and/or error), rendered as hidden cf-toast-data divs.
  private final List<Toast> toasts;

  // Default-locale program name, rendered in the page heading.
  private final String programDisplayName;

  // Null when there are no translatable locales, which hides the "Manage translations" button.
  private final String manageTranslationsUrl;

  // Form actions shared by every create/edit modal and every delete modal respectively.
  private final String createOrUpdateUrl;
  private final String deleteUrl;

  // Text of the current default status, null when none is set. Drives the data-messagepart
  // attribute the admin JS reads when confirming a default-status change.
  private final String currentDefaultStatusText;

  // Modal opened by the "Create a new status" button.
  private final StatusFormModal createStatusModal;

  // One row per configured status, in their natural ordering.
  private final List<StatusItem> statusItems;

  /**
   * Random id for a form field, so its label stays associated with it. The legacy FieldWithLabel
   * generated one per field when no id was set.
   */
  public String randomFieldId() {
    return RandomStringUtils.randomAlphabetic(8);
  }

  /** A flash toast message. */
  @Data
  @Builder
  public static final class Toast {
    // Random UUID, mirroring ToastMessage's generated id.
    private final String id;
    private final String message;
    // "SUCCESS" or "ERROR" (ToastMessage.ToastType name).
    private final String type;
    private final boolean canDismiss;
  }

  /** One configured status row together with its edit and delete modals. */
  @Data
  @Builder
  public static final class StatusItem {
    private final String statusText;
    // Whether an applicant notification email is configured.
    private final boolean hasEmail;
    // Whether this status is the program's default status.
    private final boolean defaultStatus;
    private final StatusFormModal editModal;
    // Random "uuid-..." modal id; the template derives the trigger button id from it.
    private final String deleteModalId;
  }

  /** The create-or-edit status modal and the form values / errors it renders. */
  @Data
  @Builder
  public static final class StatusFormModal {
    // Random "uuid-..." modal id; the template derives the title and trigger button ids from it.
    private final String id;
    // True for the "Create a new status" modal, false for an "Edit this status" modal.
    private final boolean creation;
    // Whether the modal opens on page load, i.e. it re-renders a rejected submission.
    private final boolean displayOnLoad;
    // Whether to warn that clearing the email body also clears its translations.
    private final boolean showEmailDeletionWarning;

    // Form values. configuredStatusText is empty when creating a status.
    private final String configuredStatusText;
    private final String statusText;
    private final String emailBody;
    private final boolean defaultStatus;

    // Formatted validation errors; empty when the form was not submitted or is valid. Field
    // errors are already wrapped in the "Error: " outline.
    private final List<String> globalErrors;
    private final List<String> statusTextErrors;
    private final List<String> emailBodyErrors;
  }
}
