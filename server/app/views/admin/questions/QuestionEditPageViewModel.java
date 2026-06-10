package views.admin.questions;

import controllers.admin.routes;
import forms.questions.QuestionForm;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;

/** ViewModel for the edit question page (Thymeleaf). */
@Data
@Builder
public final class QuestionEditPageViewModel implements BaseViewModel {

  private final QuestionForm questionForm;

  // Question type
  private final String questionTypeName;
  private final String questionTypeLabel;

  // Question ID (for form action)
  private final long questionId;

  // Hidden fields
  private final String concurrencyToken;
  private final String redirectUrl;
  private final String enumeratorIdValue;

  // Applicant-visible fields
  private final String questionText;
  private final String questionHelpText;
  private final boolean showHelpText;

  // Admin-visible fields (read-only in edit mode)
  private final String questionName;
  private final String questionDescription;
  private final String enumeratorDisplayName;

  // MAP question (GeoJSON endpoint - read-only in edit mode)
  private final boolean isMapQuestion;
  private final String geoJsonEndpoint;

  // MAP question config: settings rendered via the shared MapQuestionSettingsPartial template.
  // Null for non-MAP questions.
  private final MapQuestionSettingsPartialViewModel mapSettings;

  // --- Universal question ---
  private final boolean isCurrentlyUniversal;

  // --- Demographic fields ---
  private final boolean showDemographicFields;
  private final String questionExportState;

  // --- Display mode fields ---
  private final boolean showDisplayModeFields;
  private final String displayMode;

  // --- Primary applicant info ---
  private final boolean showPrimaryApplicantInfo;
  private final List<PaiTagInfo> paiTags;

  // --- Question config (per-type structured data) ---

  // YES_NO: whether the optional "not sure" / "maybe" options are displayed to applicants.
  // The option set itself is fixed and embedded in the YesNoFragment template.
  private final boolean yesNoNotSureDisplayed;
  private final boolean yesNoMaybeDisplayed;

  // Error/flash messages
  private final Optional<String> errorMessage;

  public String getFormActionUrl() {
    return routes.AdminQuestionController.update(questionId, questionTypeName).url();
  }

  public String getPreviewUrl() {
    return controllers.admin.routes.QuestionPreviewController.sampleQuestion(questionTypeLabel)
        .url();
  }

  /** Whether the "Question settings" section header should be shown. */
  public boolean isHasQuestionSettings() {
    return hasQuestionConfig()
        || showDemographicFields
        || showDisplayModeFields
        || showPrimaryApplicantInfo;
  }

  /** Whether there is a type-specific question config section. */
  public boolean hasQuestionConfig() {
    return switch (questionTypeName) {
      case "ADDRESS",
          "CHECKBOX",
          "DATE",
          "DROPDOWN",
          "ENUMERATOR",
          "FILEUPLOAD",
          "ID",
          "NUMBER",
          "PHONE",
          "RADIO_BUTTON",
          "TEXT",
          "YES_NO" ->
          true;
      case "MAP" -> mapSettings != null;
      default -> false;
    };
  }

  /** Data for a single Primary Applicant Info tag. */
  @Data
  @Builder
  public static final class PaiTagInfo {
    private final String fieldName;
    private final String displayName;
    private final String description;
    private final boolean enabled;
    private final boolean toggleHidden;
    private final boolean differentQuestionHasTag;
    private final String otherQuestionName;
    private final boolean isUniversal;
  }
}
