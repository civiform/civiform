package views.admin.questions;

import controllers.admin.routes;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.admin.BaseViewModel;

/** ViewModel for the edit question page (Thymeleaf). */
@Data
@Builder
public final class QuestionEditPageViewModel implements BaseViewModel {

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

  // MAP question config (still pre-rendered via Thymeleaf partial)
  private final String questionConfigHtml;

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

  // ADDRESS
  private final boolean disallowPoBox;

  // ID, TEXT
  private final String configMinLength;
  private final String configMaxLength;

  // NUMBER
  private final String configMin;
  private final String configMax;

  // ENUMERATOR
  private final String entityType;
  private final String minEntities;
  private final String maxEntities;

  // FILEUPLOAD
  private final String maxFiles;

  // DATE
  private final String minDateType;
  private final String maxDateType;
  private final String minCustomDay;
  private final String minCustomMonth;
  private final String minCustomYear;
  private final String maxCustomDay;
  private final String maxCustomMonth;
  private final String maxCustomYear;

  // CHECKBOX (in addition to multi-option)
  private final String minChoicesRequired;
  private final String maxChoicesAllowed;

  // Multi-option (CHECKBOX, DROPDOWN, RADIO_BUTTON)
  private final boolean isMultiOptionQuestion;
  private final List<MultiOptionItem> existingOptions;
  private final List<MultiOptionItem> newOptionItems;
  private final String nextAvailableId;

  // YES_NO
  private final List<YesNoOption> yesNoOptions;

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
    switch (questionTypeName) {
      case "ADDRESS":
      case "CHECKBOX":
      case "DATE":
      case "DROPDOWN":
      case "ENUMERATOR":
      case "FILEUPLOAD":
      case "ID":
      case "NUMBER":
      case "PHONE":
      case "RADIO_BUTTON":
      case "TEXT":
      case "YES_NO":
        return true;
      case "MAP":
        return !questionConfigHtml.isEmpty();
      default:
        return false;
    }
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

  /** Data for a single multi-option item (existing or new). */
  @Data
  @Builder
  public static final class MultiOptionItem {
    private final String id;
    private final String adminName;
    private final String optionText;
    private final boolean isNew;
  }

  /** Data for a single Yes/No option. */
  @Data
  @Builder
  public static final class YesNoOption {
    private final String id;
    private final String adminName;
    private final String optionText;
    private final boolean displayed;
    private final boolean required;
  }
}
