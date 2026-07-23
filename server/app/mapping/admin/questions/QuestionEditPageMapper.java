package mapping.admin.questions;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import forms.questions.MapQuestionForm;
import forms.questions.MultiOptionQuestionForm;
import forms.questions.QuestionForm;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import services.export.CsvExporterService;
import services.question.PrimaryApplicantInfoTag;
import services.question.ReadOnlyQuestionService;
import services.question.YesNoQuestionOption;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import views.admin.questions.MapQuestionSettingsPartialViewModel;
import views.admin.questions.QuestionEditPageViewModel;
import views.admin.questions.QuestionEditPageViewModel.PaiTagInfo;
import views.admin.questions.QuestionEditPageViewModel.YesNoConfig;
import views.admin.questions.QuestionEditPageViewModel.YesNoOptionRow;

/** Maps data to the QuestionEditPageViewModel. */
public final class QuestionEditPageMapper {

  public QuestionEditPageViewModel map(
      long questionId,
      QuestionForm questionForm,
      Optional<QuestionDefinition> maybeEnumerationQuestion,
      MapQuestionSettingsPartialViewModel mapSettings,
      boolean apiBridgeEnabled,
      boolean enumeratorImprovementsEnabled,
      ReadOnlyQuestionService readOnlyQuestionService,
      Optional<String> errorMessage) {
    QuestionType questionType = questionForm.getQuestionType();

    String enumeratorDisplayName =
        maybeEnumerationQuestion.map(QuestionDefinition::getName).orElse("does not repeat");
    String enumeratorIdValue = questionForm.getEnumeratorId().map(String::valueOf).orElse("");

    boolean isMapQuestion = questionType.equals(QuestionType.MAP);
    String geoJsonEndpoint = "";
    if (isMapQuestion) {
      geoJsonEndpoint = ((MapQuestionForm) questionForm).getGeoJsonEndpoint();
    }

    // Demographic fields visibility
    boolean showDemographicFields =
        !CsvExporterService.NON_EXPORTED_QUESTION_TYPES.contains(questionType);

    // Display mode fields visibility
    boolean showDisplayModeFields = apiBridgeEnabled;

    // Primary applicant info
    boolean showPrimaryApplicantInfo =
        questionForm.getEnumeratorId().isEmpty()
            && PrimaryApplicantInfoTag.getAllPaiEnabledQuestionTypes().contains(questionType);
    List<PaiTagInfo> paiTags =
        showPrimaryApplicantInfo
            ? buildPaiTags(questionForm, readOnlyQuestionService)
            : ImmutableList.of();

    Optional<String> errorToastMessage = errorMessage.map(message -> "Error: " + message);

    return QuestionEditPageViewModel.builder()
        .questionForm(questionForm)
        .questionTypeName(questionType.name())
        .questionTypeLabel(questionType.getLabel())
        .questionId(questionId)
        .concurrencyToken(questionForm.getConcurrencyToken())
        .redirectUrl(questionForm.getRedirectUrl())
        .enumeratorIdValue(enumeratorIdValue)
        .questionText(questionForm.getQuestionText())
        .questionHelpText(questionForm.getQuestionHelpText())
        .showHelpText(!questionType.equals(QuestionType.STATIC))
        .questionName(questionForm.getQuestionName())
        .questionDescription(questionForm.getQuestionDescription())
        .enumeratorDisplayName(enumeratorDisplayName)
        .isMapQuestion(isMapQuestion)
        .geoJsonEndpoint(geoJsonEndpoint)
        .mapSettings(mapSettings)
        .isCurrentlyUniversal(questionForm.isUniversal())
        .showDemographicFields(showDemographicFields)
        .questionExportState(questionForm.getQuestionExportStateTag().getValue())
        .showDisplayModeFields(showDisplayModeFields)
        .displayMode(questionForm.getDisplayMode().getValue())
        .enumeratorImprovementsEnabled(enumeratorImprovementsEnabled)
        .showPrimaryApplicantInfo(showPrimaryApplicantInfo)
        .paiTags(paiTags)
        .yesNoConfig(
            questionType.equals(QuestionType.YES_NO)
                ? buildYesNoConfig((MultiOptionQuestionForm) questionForm)
                : null)
        .errorMessage(errorToastMessage)
        .errorToastId(errorToastMessage.isPresent() ? UUID.randomUUID().toString() : null)
        .build();
  }

  /**
   * Builds the YES_NO option rows: a new question (no saved options) shows the "Select answer
   * options" label and the default option set, all displayed; an existing question renders its
   * saved options with their displayed state. Required options ("yes"/"no") always render checked.
   */
  static YesNoConfig buildYesNoConfig(MultiOptionQuestionForm form) {
    Preconditions.checkState(
        form.getOptionIds().size() == form.getOptions().size(),
        "Options and Option indexes need to be the same size.");

    boolean useDefaults = form.getOptions().isEmpty();
    ImmutableList.Builder<YesNoOptionRow> rows = ImmutableList.builder();
    if (useDefaults) {
      rows.add(
          yesNoOptionRow(
              YesNoQuestionOption.YES.getId(),
              YesNoQuestionOption.YES.getAdminName(),
              "Yes",
              /* checked= */ true));
      rows.add(
          yesNoOptionRow(
              YesNoQuestionOption.NO.getId(),
              YesNoQuestionOption.NO.getAdminName(),
              "No",
              /* checked= */ true));
      rows.add(
          yesNoOptionRow(
              YesNoQuestionOption.NOT_SURE.getId(),
              YesNoQuestionOption.NOT_SURE.getAdminName(),
              "Not sure",
              /* checked= */ true));
      rows.add(
          yesNoOptionRow(
              YesNoQuestionOption.MAYBE.getId(),
              YesNoQuestionOption.MAYBE.getAdminName(),
              "Maybe",
              /* checked= */ true));
    } else {
      for (int i = 0; i < form.getOptions().size(); i++) {
        long optionId = form.getOptionIds().get(i);
        rows.add(
            yesNoOptionRow(
                optionId,
                form.getOptionAdminNames().get(i),
                form.getOptions().get(i),
                form.getDisplayedOptionIds().contains(optionId)));
      }
    }
    return new YesNoConfig(/* showLabel= */ useDefaults, rows.build());
  }

  private static YesNoOptionRow yesNoOptionRow(
      long optionId, String adminName, String optionText, boolean checked) {
    boolean required = YesNoQuestionOption.getRequiredAdminNames().contains(adminName);
    return new YesNoOptionRow(
        String.valueOf(optionId),
        adminName,
        optionText,
        required,
        // Required options always render checked (and disabled).
        required || checked,
        String.format("Admin ID: %s. Option text: %s.", adminName, optionText));
  }

  private List<PaiTagInfo> buildPaiTags(
      QuestionForm questionForm, ReadOnlyQuestionService readOnlyQuestionService) {
    List<PaiTagInfo> tags = new ArrayList<>();
    PrimaryApplicantInfoTag.getAllPaiTagsForQuestionType(questionForm.getQuestionType())
        .forEach(
            paiTag -> {
              Optional<QuestionDefinition> currentQuestionForTag =
                  readOnlyQuestionService.getUpToDateQuestions().stream()
                      .filter(qd -> qd.getPrimaryApplicantInfoTags().contains(paiTag))
                      .findFirst();
              boolean differentQuestionHasTag =
                  currentQuestionForTag
                      .map(q -> !q.getName().equals(questionForm.getQuestionName()))
                      .orElse(false);
              String otherQuestionName =
                  currentQuestionForTag.map(QuestionDefinition::getName).orElse("");

              tags.add(
                  PaiTagInfo.builder()
                      .fieldName(paiTag.getFieldName())
                      .displayName(paiTag.getDisplayName())
                      .description(paiTag.getDescription())
                      .enabled(questionForm.primaryApplicantInfoTags().contains(paiTag))
                      .toggleHidden(!questionForm.isUniversal() || differentQuestionHasTag)
                      .differentQuestionHasTag(differentQuestionHasTag)
                      .otherQuestionName(otherQuestionName)
                      .isUniversal(questionForm.isUniversal())
                      .build());
            });
    return tags;
  }
}
