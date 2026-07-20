package mapping.admin.questions;

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
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import views.admin.questions.MapQuestionSettingsPartialViewModel;
import views.admin.questions.PaiTagInfo;
import views.admin.questions.QuestionNewPageViewModel;

/** Maps data to the QuestionNewPageViewModel for the new question page. */
public final class QuestionNewPageMapper {

  public QuestionNewPageViewModel map(
      QuestionForm questionForm,
      ImmutableList<EnumeratorQuestionDefinition> enumeratorQuestionDefinitions,
      MapQuestionSettingsPartialViewModel mapSettings,
      boolean apiBridgeEnabled,
      boolean enumeratorImprovementsEnabled,
      ReadOnlyQuestionService readOnlyQuestionService,
      Optional<String> errorMessage) {
    QuestionType questionType = questionForm.getQuestionType();

    // Build enumerator options
    ImmutableList.Builder<QuestionNewPageViewModel.EnumeratorOption> enumOptionsBuilder =
        ImmutableList.builder();
    enumOptionsBuilder.add(
        QuestionNewPageViewModel.EnumeratorOption.builder()
            .label("does not repeat")
            .value("")
            .build());
    for (EnumeratorQuestionDefinition eq : enumeratorQuestionDefinitions) {
      enumOptionsBuilder.add(
          QuestionNewPageViewModel.EnumeratorOption.builder()
              .label(eq.getName())
              .value(String.valueOf(eq.getId()))
              .build());
    }

    String selectedEnumeratorId = questionForm.getEnumeratorId().map(String::valueOf).orElse("");

    // MAP question fields
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

    return QuestionNewPageViewModel.builder()
        .questionForm(questionForm)
        .questionTypeName(questionType.name())
        .questionTypeLabel(questionType.getLabel())
        .concurrencyToken(questionForm.getConcurrencyToken())
        .redirectUrl(questionForm.getRedirectUrl())
        .questionText(questionForm.getQuestionText())
        .questionHelpText(questionForm.getQuestionHelpText())
        .showHelpText(!questionType.equals(QuestionType.STATIC))
        .questionName(questionForm.getQuestionName())
        .questionDescription(questionForm.getQuestionDescription())
        .enumeratorOptions(enumOptionsBuilder.build())
        .selectedEnumeratorId(selectedEnumeratorId)
        .enumeratorSelectEnabled(questionForm.getEnumeratorSelectEnabled())
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
                ? YesNoConfigMapper.buildYesNoConfig((MultiOptionQuestionForm) questionForm)
                : null)
        .errorMessage(errorToastMessage)
        .errorToastId(errorToastMessage.isPresent() ? UUID.randomUUID().toString() : null)
        .build();
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
