package mapping.admin.questions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import services.LocalizedStrings;
import services.question.MapSettingType;
import services.question.QuestionOption;
import services.question.QuestionSetting;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.MapQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import views.admin.questions.QuestionTranslationPageViewModel;

/** Maps data to the QuestionTranslationPageViewModel for the question translation page. */
public final class QuestionTranslationPageMapper {

  public QuestionTranslationPageViewModel map(
      QuestionDefinition question,
      Locale locale,
      ImmutableList<Locale> translatableLocales,
      Optional<String> errorMessage) {
    ImmutableList<QuestionTranslationPageViewModel.LocaleLink> localeLinks =
        translatableLocales.stream()
            .map(
                l ->
                    QuestionTranslationPageViewModel.LocaleLink.builder()
                        .questionName(question.getName())
                        .localeTag(l.toLanguageTag())
                        .displayName(getDisplayLanguage(l))
                        .selected(l.equals(locale))
                        .build())
            .collect(ImmutableList.toImmutableList());

    QuestionTranslationPageViewModel.TranslationField questionTextField =
        QuestionTranslationPageViewModel.TranslationField.builder()
            .fieldName("questionText")
            .label("Question text")
            .value(question.getQuestionText().maybeGet(locale).orElse(""))
            .defaultText(question.getQuestionText().getDefault())
            .isTextArea(true)
            .build();

    Optional<QuestionTranslationPageViewModel.TranslationField> helpTextField =
        question.getQuestionHelpText().isEmpty()
            ? Optional.empty()
            : Optional.of(
                QuestionTranslationPageViewModel.TranslationField.builder()
                    .fieldName("questionHelpText")
                    .label("Question help text")
                    .value(question.getQuestionHelpText().maybeGet(locale).orElse(""))
                    .defaultText(question.getQuestionHelpText().getDefault())
                    .isTextArea(true)
                    .build());

    ImmutableList<QuestionTranslationPageViewModel.TranslationField> typeSpecificFields =
        buildTypeSpecificFields(question, locale);

    return QuestionTranslationPageViewModel.builder()
        .questionName(question.getName())
        .questionId(question.getId())
        .localeTag(locale.toLanguageTag())
        .currentLocaleDisplayName(getDisplayLanguage(locale))
        .concurrencyToken(question.getConcurrencyToken().map(String::valueOf).orElse(""))
        .localeLinks(localeLinks)
        .questionTextField(questionTextField)
        .helpTextField(helpTextField)
        .questionType(getTypeSpecificLegend(question.getQuestionType()))
        .typeSpecificFields(typeSpecificFields)
        .errorMessage(errorMessage)
        .build();
  }

  private ImmutableList<QuestionTranslationPageViewModel.TranslationField> buildTypeSpecificFields(
      QuestionDefinition question, Locale locale) {
    switch (question.getQuestionType()) {
      case CHECKBOX:
      case DROPDOWN:
      case RADIO_BUTTON:
        MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) question;
        ImmutableList<QuestionOption> options = multiOption.getOptions();
        ImmutableList.Builder<QuestionTranslationPageViewModel.TranslationField> optionFields =
            ImmutableList.builder();
        for (int i = 0; i < options.size(); i++) {
          QuestionOption option = options.get(i);
          optionFields.add(
              QuestionTranslationPageViewModel.TranslationField.builder()
                  .fieldName("options[]")
                  .label(String.format("Answer option #%d", i + 1))
                  .value(option.optionText().maybeGet(locale).orElse(""))
                  .defaultText(option.optionText().getDefault())
                  .isTextArea(false)
                  .build());
        }
        return optionFields.build();
      case ENUMERATOR:
        EnumeratorQuestionDefinition enumerator = (EnumeratorQuestionDefinition) question;
        LocalizedStrings entityType = enumerator.getEntityType();
        return ImmutableList.of(
            QuestionTranslationPageViewModel.TranslationField.builder()
                .fieldName("entityType")
                .label("What is being enumerated")
                .value(entityType.maybeGet(locale).orElse(""))
                .defaultText(entityType.getDefault())
                .isTextArea(true)
                .build());
      case MAP:
        MapQuestionDefinition mapQuestion = (MapQuestionDefinition) question;
        ImmutableSet<QuestionSetting> settings =
            mapQuestion.getQuestionSettings().orElse(ImmutableSet.of());
        ImmutableList.Builder<QuestionTranslationPageViewModel.TranslationField> settingFields =
            ImmutableList.builder();
        settings.forEach(
            setting -> {
              if (setting.settingType().equals(MapSettingType.LOCATION_FILTER_GEO_JSON_KEY)) {
                LocalizedStrings displayName =
                    setting.localizedSettingDisplayName().orElse(LocalizedStrings.of());
                settingFields.add(
                    QuestionTranslationPageViewModel.TranslationField.builder()
                        .fieldName("filters[]")
                        .label("Filter display name")
                        .value(displayName.maybeGet(locale).orElse(""))
                        .defaultText(
                            displayName.hasTranslationFor(LocalizedStrings.DEFAULT_LOCALE)
                                ? displayName.getDefault()
                                : "")
                        .isTextArea(false)
                        .build());
              }
              if (setting.settingType().equals(MapSettingType.LOCATION_TAG_GEO_JSON_KEY)) {
                LocalizedStrings tagDisplayName =
                    setting.localizedSettingDisplayName().orElse(LocalizedStrings.of());
                LocalizedStrings tagText =
                    setting.localizedSettingText().orElse(LocalizedStrings.of());
                settingFields.add(
                    QuestionTranslationPageViewModel.TranslationField.builder()
                        .fieldName("tagDisplayName")
                        .label("Tag display name")
                        .value(tagDisplayName.maybeGet(locale).orElse(""))
                        .defaultText(
                            tagDisplayName.hasTranslationFor(LocalizedStrings.DEFAULT_LOCALE)
                                ? tagDisplayName.getDefault()
                                : "")
                        .isTextArea(false)
                        .build());
                settingFields.add(
                    QuestionTranslationPageViewModel.TranslationField.builder()
                        .fieldName("tagText")
                        .label("Tag text")
                        .value(tagText.maybeGet(locale).orElse(""))
                        .defaultText(
                            tagText.hasTranslationFor(LocalizedStrings.DEFAULT_LOCALE)
                                ? tagText.getDefault()
                                : "")
                        .isTextArea(true)
                        .build());
              }
            });
        return settingFields.build();
      default:
        return ImmutableList.of();
    }
  }

  private static String getDisplayLanguage(Locale locale) {
    return locale.equals(Locale.TRADITIONAL_CHINESE)
        ? "Traditional Chinese"
        : locale.getDisplayLanguage(LocalizedStrings.DEFAULT_LOCALE);
  }

  private static String getTypeSpecificLegend(QuestionType type) {
    switch (type) {
      case CHECKBOX:
      case DROPDOWN:
      case RADIO_BUTTON:
        return "Answer options";
      case ENUMERATOR:
        return "Enumerator settings";
      case MAP:
        return "Settings";
      default:
        return "";
    }
  }
}
