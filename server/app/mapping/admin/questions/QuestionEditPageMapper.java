package mapping.admin.questions;

import com.google.common.collect.ImmutableList;
import forms.questions.MapQuestionForm;
import forms.questions.MultiOptionQuestionForm;
import forms.questions.QuestionForm;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import services.export.CsvExporterService;
import services.question.PrimaryApplicantInfoTag;
import services.question.ReadOnlyQuestionService;
import services.question.YesNoQuestionOption;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import views.admin.questions.MapQuestionSettingsPartialViewModel;
import views.admin.questions.QuestionEditPageViewModel;
import views.admin.questions.QuestionEditPageViewModel.PaiTagInfo;

/** Maps data to the QuestionEditPageViewModel. */
public final class QuestionEditPageMapper {

  public QuestionEditPageViewModel map(
      long questionId,
      QuestionForm questionForm,
      Optional<QuestionDefinition> maybeEnumerationQuestion,
      MapQuestionSettingsPartialViewModel mapSettings,
      boolean apiBridgeEnabled,
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

    // Question config data
    QuestionEditPageViewModel.QuestionEditPageViewModelBuilder builder =
        QuestionEditPageViewModel.builder()
            .questionForm(questionForm)
            .questionTypeName(questionType.name())
            .questionTypeLabel(questionType.getLabel().toLowerCase(Locale.ROOT))
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
            .questionExportState(questionForm.getQuestionExportState())
            .showDisplayModeFields(showDisplayModeFields)
            .displayMode(questionForm.getDisplayMode().getValue())
            .showPrimaryApplicantInfo(showPrimaryApplicantInfo)
            .paiTags(paiTags)
            .errorMessage(errorMessage);

    // YES_NO questions have a fixed option set embedded in the template; only the displayed state
    // of the optional "not sure"/"maybe" options is dynamic. A new question (no options yet)
    // defaults to showing all options.
    if (questionType.equals(QuestionType.YES_NO)) {
      MultiOptionQuestionForm multiOptionForm = (MultiOptionQuestionForm) questionForm;
      boolean isNewQuestion = multiOptionForm.getOptions().isEmpty();
      builder
          .yesNoNotSureDisplayed(
              isNewQuestion
                  || multiOptionForm
                      .getDisplayedOptionIds()
                      .contains(YesNoQuestionOption.NOT_SURE.getId()))
          .yesNoMaybeDisplayed(
              isNewQuestion
                  || multiOptionForm
                      .getDisplayedOptionIds()
                      .contains(YesNoQuestionOption.MAYBE.getId()));
    }

    return builder.build();
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
