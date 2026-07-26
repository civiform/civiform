package views.admin.questions;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import forms.questions.MultiOptionQuestionForm;
import forms.questions.QuestionForm;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import services.RandomStringUtils;
import views.BaseViewModel;

/** ViewModel for the new/edit question page (Thymeleaf). */
@Data
@Builder
public final class QuestionFormPageViewModel implements BaseViewModel {

  private final QuestionForm questionForm;

  // Whether the page edits an existing question (true) or creates a new one
  // (false). Edit mode locks the create-time-only fields (administrative
  // identifier, enumerator, GeoJSON endpoint) and swaps the Cancel/Create
  // buttons for an Update button backed by the unset-universal confirmation
  // modal.
  private final boolean editMode;

  // ID of the question being edited (for the form action). Null in new mode.
  private final Long questionId;

  // Question type
  private final String questionTypeName;
  // Question type display label, e.g. "Text" or "Yes/No" (original casing;
  // used for the preview URL and, in edit mode, the modal text).
  private final String questionTypeLabel;

  // Hidden fields
  private final String concurrencyToken;
  private final String redirectUrl;

  // Applicant-visible fields
  private final String questionText;
  private final String questionHelpText;
  private final boolean showHelpText;

  // Admin-visible fields (identifier and enumerator are read-only in edit
  // mode)
  private final String questionName;
  private final String questionDescription;
  // Enumerator options for the select. Empty in edit mode, where the select
  // is locked to the single current value.
  private final ImmutableList<EnumeratorOption> enumeratorOptions;
  private final String selectedEnumeratorId;
  // Display name for the locked enumerator select in edit mode. Null in new
  // mode.
  private final String enumeratorDisplayName;

  // Whether the admin may choose an enumerator. False when the creation
  // context fixes it (e.g. adding a question to a repeated block), in which
  // case the select renders read-only. Always false in edit mode.
  private final boolean enumeratorSelectEnabled;

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

  // When the enumerator-improvements flag is on, the repeated-question info
  // alert omits the "$this" usage paragraphs.
  private final boolean enumeratorImprovementsEnabled;

  // --- Primary applicant info ---
  private final boolean showPrimaryApplicantInfo;
  private final List<PaiTagInfo> paiTags;

  // --- Question config (per-type structured data) ---

  // YES_NO: the option rows to render. Null for other question types.
  private final YesNoConfig yesNoConfig;

  // CHECKBOX / DROPDOWN / RADIO_BUTTON: whether the per-option score inputs render (the
  // answer-option-scoring flag is on and the question type supports scores).
  private final boolean showScores;

  // Error message shown as a toast (already carries the "Error: " prefix)
  private final Optional<String> errorMessage;
  // Random id for the toast container. Null when there is no error message.
  private final String errorToastId;

  /** Page title/heading, with the type label lowercased. */
  public String getTitle() {
    return String.format(
        editMode ? "Edit %s question" : "New %s question",
        questionTypeLabel.toLowerCase(Locale.ROOT));
  }

  public String getCancelUrl() {
    return isBlank(redirectUrl)
        ? routes.AdminQuestionController.index(Optional.empty()).url()
        : redirectUrl;
  }

  public String getFormActionUrl() {
    return editMode
        ? routes.AdminQuestionController.update(questionId, questionTypeName).url()
        : routes.AdminQuestionController.create(questionTypeName).url();
  }

  public String getPreviewUrl() {
    return controllers.admin.routes.QuestionPreviewController.sampleQuestion(questionTypeLabel)
        .url();
  }

  public String getGeoJsonPostUrl() {
    return controllers.geojson.routes.GeoJsonApiController.hxGetData().url();
  }

  /**
   * Generates a random field id for fields without an explicit id (labels need an id to stay
   * associated with their inputs).
   */
  public String randomFieldId() {
    return RandomStringUtils.randomAlphabetic(8);
  }

  /**
   * The display value for the score input at {@code index} of a bound score list, formatted
   * without trailing zeros. Null (attribute omitted) when the entry is missing, blank, or
   * unparseable; a null-padded or short list from a sparse crafted post is tolerated.
   */
  public String scoreDisplayValue(List<String> scores, int index) {
    if (index >= scores.size()) {
      return null;
    }
    return MultiOptionQuestionForm.formatScoreForDisplay(scores.get(index)).orElse(null);
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
