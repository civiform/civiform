package mapping.admin.questions;

import com.google.common.collect.ImmutableList;
import forms.AddressQuestionForm;
import forms.DateQuestionForm;
import forms.EnumeratorQuestionForm;
import forms.FileUploadQuestionForm;
import forms.IdQuestionForm;
import forms.MapQuestionForm;
import forms.MultiOptionQuestionForm;
import forms.NumberQuestionForm;
import forms.QuestionForm;
import forms.TextQuestionForm;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import services.export.CsvExporterService;
import services.question.PrimaryApplicantInfoTag;
import services.question.ReadOnlyQuestionService;
import services.question.YesNoQuestionOption;
import services.question.types.DateQuestionDefinition.DateValidationOption.DateType;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import views.admin.questions.QuestionEditPageViewModel;
import views.admin.questions.QuestionEditPageViewModel.MultiOptionItem;
import views.admin.questions.QuestionEditPageViewModel.PaiTagInfo;
import views.admin.questions.QuestionEditPageViewModel.YesNoOption;

/** Maps data to the QuestionEditPageViewModel. */
public final class QuestionEditPageMapper {

  public QuestionEditPageViewModel map(
      long questionId,
      QuestionForm questionForm,
      Optional<QuestionDefinition> maybeEnumerationQuestion,
      String questionConfigHtml,
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
            .questionConfigHtml(questionConfigHtml)
            .isCurrentlyUniversal(questionForm.isUniversal())
            .showDemographicFields(showDemographicFields)
            .questionExportState(questionForm.getQuestionExportState())
            .showDisplayModeFields(showDisplayModeFields)
            .displayMode(questionForm.getDisplayMode().getValue())
            .showPrimaryApplicantInfo(showPrimaryApplicantInfo)
            .paiTags(paiTags)
            .errorMessage(errorMessage)
            // Defaults for config fields
            .disallowPoBox(false)
            .configMinLength("")
            .configMaxLength("")
            .configMin("")
            .configMax("")
            .entityType("")
            .minEntities("")
            .maxEntities("")
            .maxFiles("")
            .minDateType("")
            .maxDateType("")
            .minCustomDay("")
            .minCustomMonth("")
            .minCustomYear("")
            .maxCustomDay("")
            .maxCustomMonth("")
            .maxCustomYear("")
            .minChoicesRequired("")
            .maxChoicesAllowed("")
            .isMultiOptionQuestion(false)
            .existingOptions(ImmutableList.of())
            .newOptionItems(ImmutableList.of())
            .nextAvailableId("")
            .yesNoOptions(ImmutableList.of());

    // Populate type-specific config fields
    populateQuestionConfig(builder, questionForm);

    return builder.build();
  }

  private void populateQuestionConfig(
      QuestionEditPageViewModel.QuestionEditPageViewModelBuilder builder,
      QuestionForm questionForm) {
    switch (questionForm.getQuestionType()) {
      case ADDRESS:
        AddressQuestionForm addressForm = (AddressQuestionForm) questionForm;
        builder.disallowPoBox(addressForm.getDisallowPoBox());
        break;
      case ID:
        IdQuestionForm idForm = (IdQuestionForm) questionForm;
        builder
            .configMinLength(optionalIntToString(idForm.getMinLength()))
            .configMaxLength(optionalIntToString(idForm.getMaxLength()));
        break;
      case TEXT:
        TextQuestionForm textForm = (TextQuestionForm) questionForm;
        builder
            .configMinLength(optionalIntToString(textForm.getMinLength()))
            .configMaxLength(optionalIntToString(textForm.getMaxLength()));
        break;
      case NUMBER:
        NumberQuestionForm numberForm = (NumberQuestionForm) questionForm;
        builder
            .configMin(optionalLongToString(numberForm.getMin()))
            .configMax(optionalLongToString(numberForm.getMax()));
        break;
      case ENUMERATOR:
        EnumeratorQuestionForm enumForm = (EnumeratorQuestionForm) questionForm;
        builder
            .entityType(enumForm.getEntityType())
            .minEntities(optionalIntToString(enumForm.getMinEntities()))
            .maxEntities(optionalIntToString(enumForm.getMaxEntities()));
        break;
      case FILEUPLOAD:
        FileUploadQuestionForm fileForm = (FileUploadQuestionForm) questionForm;
        builder.maxFiles(optionalIntToString(fileForm.getMaxFiles()));
        break;
      case DATE:
        DateQuestionForm dateForm = (DateQuestionForm) questionForm;
        builder
            .minDateType(dateForm.getMinDateType().map(DateType::toString).orElse("ANY"))
            .maxDateType(dateForm.getMaxDateType().map(DateType::toString).orElse("ANY"))
            .minCustomDay(dateForm.getMinCustomDay().orElse(""))
            .minCustomMonth(dateForm.getMinCustomMonth().orElse(""))
            .minCustomYear(dateForm.getMinCustomYear().orElse(""))
            .maxCustomDay(dateForm.getMaxCustomDay().orElse(""))
            .maxCustomMonth(dateForm.getMaxCustomMonth().orElse(""))
            .maxCustomYear(dateForm.getMaxCustomYear().orElse(""));
        break;
      case CHECKBOX:
        MultiOptionQuestionForm checkboxForm = (MultiOptionQuestionForm) questionForm;
        builder
            .isMultiOptionQuestion(true)
            .minChoicesRequired(optionalIntToString(checkboxForm.getMinChoicesRequired()))
            .maxChoicesAllowed(optionalIntToString(checkboxForm.getMaxChoicesAllowed()));
        populateMultiOptionFields(builder, checkboxForm);
        break;
      case DROPDOWN:
      case RADIO_BUTTON:
        MultiOptionQuestionForm multiForm = (MultiOptionQuestionForm) questionForm;
        builder.isMultiOptionQuestion(true);
        populateMultiOptionFields(builder, multiForm);
        break;
      case YES_NO:
        MultiOptionQuestionForm yesNoForm = (MultiOptionQuestionForm) questionForm;
        builder.yesNoOptions(buildYesNoOptions(yesNoForm));
        break;
      default:
        break;
    }
  }

  private void populateMultiOptionFields(
      QuestionEditPageViewModel.QuestionEditPageViewModelBuilder builder,
      MultiOptionQuestionForm form) {
    List<MultiOptionItem> existing = new ArrayList<>();
    for (int i = 0; i < form.getOptions().size(); i++) {
      existing.add(
          MultiOptionItem.builder()
              .id(String.valueOf(form.getOptionIds().get(i)))
              .adminName(form.getOptionAdminNames().get(i))
              .optionText(form.getOptions().get(i))
              .isNew(false)
              .build());
    }

    List<MultiOptionItem> newItems = new ArrayList<>();
    for (int i = 0; i < form.getNewOptions().size(); i++) {
      newItems.add(
          MultiOptionItem.builder()
              .id("")
              .adminName(form.getNewOptionAdminNames().get(i))
              .optionText(form.getNewOptions().get(i))
              .isNew(true)
              .build());
    }

    builder
        .existingOptions(existing)
        .newOptionItems(newItems)
        .nextAvailableId(
            form.getNextAvailableId().isPresent()
                ? String.valueOf(form.getNextAvailableId().getAsLong())
                : "0");
  }

  private List<YesNoOption> buildYesNoOptions(MultiOptionQuestionForm form) {
    List<YesNoOption> options = new ArrayList<>();

    if (form.getOptions().isEmpty()) {
      // Default YES_NO options when creating a new question
      options.add(buildDefaultYesNoOption(YesNoQuestionOption.YES, "Yes", 0));
      options.add(buildDefaultYesNoOption(YesNoQuestionOption.NO, "No", 1));
      options.add(buildDefaultYesNoOption(YesNoQuestionOption.NOT_SURE, "Not sure", 2));
      options.add(buildDefaultYesNoOption(YesNoQuestionOption.MAYBE, "Maybe", 3));
    } else {
      for (int i = 0; i < form.getOptions().size(); i++) {
        String adminName = form.getOptionAdminNames().get(i);
        long optionId = form.getOptionIds().get(i);
        boolean isDisplayed = form.getDisplayedOptionIds().contains(optionId);
        boolean isRequired = YesNoQuestionOption.getRequiredAdminNames().contains(adminName);
        options.add(
            YesNoOption.builder()
                .id(String.valueOf(optionId))
                .adminName(adminName)
                .optionText(form.getOptions().get(i))
                .displayed(isDisplayed)
                .required(isRequired)
                .build());
      }
    }

    return options;
  }

  private YesNoOption buildDefaultYesNoOption(YesNoQuestionOption option, String text, int order) {
    boolean isRequired =
        YesNoQuestionOption.getRequiredAdminNames().contains(option.getAdminName());
    return YesNoOption.builder()
        .id(String.valueOf(option.getId()))
        .adminName(option.getAdminName())
        .optionText(text)
        .displayed(true)
        .required(isRequired)
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

  private static String optionalIntToString(OptionalInt value) {
    return value.isPresent() ? String.valueOf(value.getAsInt()) : "";
  }

  private static String optionalLongToString(OptionalLong value) {
    return value.isPresent() ? String.valueOf(value.getAsLong()) : "";
  }
}
