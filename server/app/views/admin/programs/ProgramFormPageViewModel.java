package views.admin.programs;

import controllers.admin.routes;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;

/** ViewModel for the new/edit program details page (Thymeleaf). */
@Data
@Builder
public final class ProgramFormPageViewModel implements BaseViewModel {

  // Whether the page edits an existing program (true) or creates a new one
  // (false). Edit mode changes the page title, posts to the update endpoint
  // and can show the "Manage questions" link.
  private final boolean editMode;

  // ID of the program being edited (for the form action). Null in new mode.
  private final Long programId;

  // Name of the ProgramEditStatus the page was loaded with (for the update
  // action URL). Null in new mode, where the create action takes no status.
  private final String programEditStatusName;

  // Program display name used in the edit-mode page title. Always the
  // program definition's localized name, even when re-rendering with form
  // data. Null in new mode.
  private final String titleProgramName;

  // Whether the program slug is editable (only at first creation). When
  // false, the slug section shows read-only text instead of an input.
  private final boolean slugEditable;

  // While in the creation flow admins continue to the program-image step, so
  // the submit button reads "Save and continue to next step" instead of
  // "Save".
  private final boolean saveAndContinue;

  // --- Form field values ---
  private final String adminName;
  private final String adminDescription;
  private final String displayName;
  private final String displayDescription;
  private final String shortDescription;
  private final String externalLink;
  private final String confirmationMessage;

  // DisplayMode enum name backing the visibility radio group.
  private final String displayMode;

  // ProgramType form value ("default", "external", "common_intake_form")
  // backing the program type radio group.
  private final String programTypeValue;

  private final boolean emailNotificationChecked;
  private final boolean loginOnlyChecked;

  // Eligibility radios; checked states already account for the disabled
  // program types, matching the legacy view.
  private final boolean eligibilityGatingChecked;
  private final boolean eligibilityNotGatingChecked;

  // --- Per-program-type field states ---
  private final boolean isExternalProgram;
  private final boolean disableProgramEligibility;
  private final boolean disableLongDescription;
  private final boolean disableExternalLink;
  private final boolean disableEmailNotifications;
  private final boolean disableApplicationSteps;
  private final boolean disableLoginOnly;
  private final boolean disableConfirmationMessage;
  private final boolean categoriesDisabled;

  // Program type radios can be disabled when editing (a program can become
  // external, but an external program cannot change type).
  private final boolean defaultProgramFieldDisabled;
  private final boolean preScreenerFieldDisabled;
  private final boolean externalProgramFieldDisabled;

  // Read-only slug section text: the applicant-facing program URL, or the
  // program ID for external programs. Null when the slug is editable.
  private final String slugFieldText;

  // Whether the edit page shows the "Manage questions" link. Based on the
  // stored program's type, not the submitted form's.
  private final boolean showManageQuestionsLink;

  // Category checkboxes. Empty when no categories exist, which hides the
  // whole fieldset.
  private final List<CategoryOption> categoryOptions;

  // Trusted intermediary group checkboxes for SELECT_TI visibility.
  private final List<TiGroupOption> tiGroups;
  private final boolean tiListVisible;

  // Exactly five application step rows, matching the legacy form.
  private final List<ApplicationStepRow> applicationSteps;

  // Default confirmation message rendered into the form's
  // data-default-confirmation-message attribute (read by admin JS).
  private final String defaultConfirmationMessage;

  // Display name of the current pre-screener form. Non-null only when the
  // change-pre-screener confirmation modal should display on load.
  private final String preScreenerFormDisplayName;

  // Error message shown as a toast (already carries the "Error: " prefix)
  private final Optional<String> errorMessage;
  // Random id for the toast container. Null when there is no error message.
  private final String errorToastId;

  /** Page title/heading. */
  public String getTitle() {
    return editMode
        ? String.format("Edit program: %s", titleProgramName)
        : "New program information";
  }

  public String getFormActionUrl() {
    return editMode
        ? routes.AdminProgramController.update(programId, programEditStatusName).url()
        : routes.AdminProgramController.create().url();
  }

  public String getManageQuestionsUrl() {
    return routes.AdminProgramBlocksController.index(programId).url();
  }

  /** Represents one category checkbox. */
  @Data
  @Builder
  public static final class CategoryOption {
    // Default-locale category name; also derives the checkbox id
    // ("checkbox-category-<name>").
    private final String name;
    private final String value;
    private final boolean checked;
  }

  /** Represents one trusted intermediary group checkbox. */
  @Data
  @Builder
  public static final class TiGroupOption {
    private final String value;
    private final String name;
    private final boolean checked;
  }

  /** Represents one application step (title + description fields). */
  @Data
  @Builder
  public static final class ApplicationStepRow {
    // Zero-based step index; the template derives field ids and names.
    private final int index;
    private final String title;
    private final String description;
    // Only the first step is required, and only when steps are enabled.
    private final boolean required;
  }
}
