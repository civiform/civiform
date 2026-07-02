package views.admin.questions;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import forms.questions.QuestionForm;
import java.util.List;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import views.BaseViewModel;
import views.admin.questions.QuestionEditPageViewModel.PaiTagInfo;

/** ViewModel for the new question creation page (Thymeleaf). */
@Data
@Builder
public final class QuestionNewPageViewModel implements BaseViewModel {

  private final QuestionForm questionForm;

  // Question type
  private final String questionTypeName;
  private final String questionTypeLabel;

  // Hidden fields
  private final String concurrencyToken;
  private final String redirectUrl;

  // Applicant-visible fields
  private final String questionText;
  private final String questionHelpText;
  private final boolean showHelpText;

  // Admin-visible fields
  private final String questionName;
  private final String questionDescription;
  private final ImmutableList<EnumeratorOption> enumeratorOptions;
  private final String selectedEnumeratorId;

  // MAP question (GeoJSON endpoint)
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

  // Error message
  private final Optional<String> errorMessage;

  public String getCancelUrl() {
    return isBlank(redirectUrl)
        ? routes.AdminQuestionController.index(Optional.empty()).url()
        : redirectUrl;
  }

  public String getFormActionUrl() {
    return routes.AdminQuestionController.create(questionTypeName).url();
  }

  public String getPreviewUrl() {
    return controllers.admin.routes.QuestionPreviewController.sampleQuestion(questionTypeLabel)
        .url();
  }

  public String getGeoJsonPostUrl() {
    return controllers.geojson.routes.GeoJsonApiController.hxGetData().url();
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

  /** Represents an enumerator question option in the select dropdown. */
  @Data
  @Builder
  public static final class EnumeratorOption {
    private final String label;
    private final String value;
  }
}
