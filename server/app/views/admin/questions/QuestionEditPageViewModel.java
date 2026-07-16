package views.admin.questions;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import forms.questions.QuestionForm;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import services.RandomStringUtils;
import views.BaseViewModel;

/** ViewModel for the edit question page (Thymeleaf). */
@Data
@Builder
public final class QuestionEditPageViewModel implements BaseViewModel {

  private final QuestionForm questionForm;

  // Question type
  private final String questionTypeName;
  // Question type display label, e.g. "Text" or "Yes/No" (original casing;
  // used for the preview URL and modal text).
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

  // When the enumerator-improvements flag is on, the repeated-question info
  // alert omits the "$this" usage paragraphs.
  private final boolean enumeratorImprovementsEnabled;

  // --- Primary applicant info ---
  private final boolean showPrimaryApplicantInfo;
  private final List<PaiTagInfo> paiTags;

  // --- Question config (per-type structured data) ---

  // YES_NO: the option rows to render. Null for other question types.
  private final YesNoConfig yesNoConfig;

  // Error message shown as a toast (already carries the "Error: " prefix)
  private final Optional<String> errorMessage;
  // Random id for the toast container. Null when there is no error message.
  private final String errorToastId;

  /** Page title/heading, with the type label lowercased. */
  public String getTitle() {
    return String.format("Edit %s question", questionTypeLabel.toLowerCase(Locale.ROOT));
  }

  public String getFormActionUrl() {
    return routes.AdminQuestionController.update(questionId, questionTypeName).url();
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

    public String getAlreadySetAlertText() {
      return String.format(
          "You cannot make this question a Primary applicant information question because the"
              + " question named \"%s\" is already set as a Primary applicant information"
              + " question.",
          otherQuestionName);
    }

    public String getNotUniversalAlreadySetAlertText() {
      return String.format(
          "You cannot make this question a Primary applicant information question because the"
              + " question is not a Universal question and because the question named \"%s\" is"
              + " already set as a Primary applicant information question.",
          otherQuestionName);
    }
  }

  /**
   * YES_NO question settings. {@code showLabel} is true when rendering the default option set for a
   * question with no saved options.
   */
  public record YesNoConfig(boolean showLabel, ImmutableList<YesNoOptionRow> options) {}

  /**
   * A single YES_NO option row: hidden form-binding inputs plus a display checkbox. Required
   * options ("yes"/"no") render checked and disabled, with an extra hidden input so the value still
   * posts.
   */
  public record YesNoOptionRow(
      String optionId,
      String adminName,
      String optionText,
      boolean required,
      boolean checked,
      String ariaLabel) {}
}
