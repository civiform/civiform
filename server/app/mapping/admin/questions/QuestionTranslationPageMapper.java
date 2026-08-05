package mapping.admin.questions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import services.LocalizedStrings;
import services.RandomStringUtils;
import services.question.MapSettingType;
import services.question.QuestionOption;
import services.question.QuestionSetting;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.MapQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import views.admin.questions.QuestionTranslationPageViewModel;
import views.admin.questions.QuestionTranslationPageViewModel.LanguageLink;
import views.admin.questions.QuestionTranslationPageViewModel.TranslationField;
import views.components.TextFormatter;

/** Maps data to the QuestionTranslationPageViewModel for the question translations page. */
public final class QuestionTranslationPageMapper {

  /**
   * Maps a question and the locale being translated to the view model.
   *
   * @param question the question being translated
   * @param localeToEdit the locale whose translations are being edited
   * @param translatableLocales every locale the deployment supports translations for, used to build
   *     the language picker
   * @param errorMessage a save failure or concurrent-update notice, rendered as an error toast
   */
  public QuestionTranslationPageViewModel map(
      QuestionDefinition question,
      Locale localeToEdit,
      ImmutableList<Locale> translatableLocales,
      Optional<String> errorMessage) {
    Optional<String> errorToastMessage = errorMessage.map(message -> "Error: " + message);

    QuestionTranslationPageViewModel.QuestionTranslationPageViewModelBuilder builder =
        QuestionTranslationPageViewModel.builder()
            .title(String.format("Manage question translations: %s", question.getName()))
            .formActionUrl(
                controllers.admin.routes.AdminQuestionTranslationsController.update(
                        question.getName(), localeToEdit.toLanguageTag())
                    .url())
            .editDefaultUrl(
                controllers.admin.routes.AdminQuestionController.edit(
                        question.getId(), /* redirectUrl= */ "")
                    .url())
            .saveButtonText(String.format("Save %s updates", getDisplayLanguage(localeToEdit)))
            .concurrencyToken(question.getConcurrencyToken().map(String::valueOf).orElse(""))
            .languageLinks(
                buildLanguageLinks(question.getName(), localeToEdit, translatableLocales))
            .questionTextField(
                buildTextareaField(
                    "questionText",
                    "Question text",
                    question.getQuestionText(),
                    localeToEdit,
                    /* markdown= */ true))
            .errorMessage(errorToastMessage)
            .errorToastId(errorToastMessage.isPresent() ? UUID.randomUUID().toString() : null);

    // Help text is optional - only show it when the question has one.
    LocalizedStrings helpText = question.getQuestionHelpText();
    boolean showHelpText = !helpText.isEmpty();
    builder.showHelpText(showHelpText);
    if (showHelpText) {
      builder.questionHelpTextField(
          buildTextareaField(
              "questionHelpText",
              "Question help text",
              helpText,
              localeToEdit,
              /* markdown= */ true));
    }

    addQuestionTypeSpecificFields(builder, question, localeToEdit);

    return builder.build();
  }

  private void addQuestionTypeSpecificFields(
      QuestionTranslationPageViewModel.QuestionTranslationPageViewModelBuilder builder,
      QuestionDefinition question,
      Locale localeToEdit) {
    // Defaults for the sections that do not apply to this question type.
    builder
        .showYesNoNote(false)
        .showAnswerOptions(false)
        .answerOptionFields(ImmutableList.of())
        .showEntityType(false)
        .showMapSettings(false)
        .mapSettingFields(ImmutableList.of());

    switch (question.getQuestionType()) {
      case YES_NO:
        builder.showYesNoNote(true);
        return;
      case CHECKBOX: // fallthrough intended
      case DROPDOWN: // fallthrough intended
      case RADIO_BUTTON:
        ImmutableList<QuestionOption> options =
            ((MultiOptionQuestionDefinition) question).getOptions();
        if (options.isEmpty()) {
          return;
        }
        builder
            .showAnswerOptions(true)
            .answerOptionFields(buildAnswerOptionFields(options, localeToEdit));
        return;
      case ENUMERATOR:
        builder
            .showEntityType(true)
            .entityTypeField(
                buildTextareaField(
                    "entityType",
                    "What is being enumerated",
                    ((EnumeratorQuestionDefinition) question).getEntityType(),
                    localeToEdit,
                    /* markdown= */ true));
        return;
      case MAP:
        ImmutableSet<QuestionSetting> settings =
            ((MapQuestionDefinition) question).getQuestionSettings().orElse(ImmutableSet.of());
        if (settings.isEmpty()) {
          return;
        }
        builder
            .showMapSettings(true)
            .mapSettingFields(buildMapSettingFields(settings, localeToEdit));
        return;
      case ADDRESS: // fallthrough intended
      case CURRENCY: // fallthrough intended
      case FILEUPLOAD: // fallthrough intended
      case NAME: // fallthrough intended
      case NUMBER: // fallthrough intended
      case TEXT: // fallthrough intended
      case PHONE: // fallthrough intended
      default:
        return;
    }
  }

  private ImmutableList<LanguageLink> buildLanguageLinks(
      String questionName, Locale currentlySelected, ImmutableList<Locale> translatableLocales) {
    return translatableLocales.stream()
        .map(
            locale ->
                LanguageLink.builder()
                    .href(
                        controllers.admin.routes.AdminQuestionTranslationsController.edit(
                                questionName, locale.toLanguageTag())
                            .url())
                    .displayLanguage(getDisplayLanguage(locale))
                    .selected(locale.equals(currentlySelected))
                    .build())
        .collect(ImmutableList.toImmutableList());
  }

  private ImmutableList<TranslationField> buildAnswerOptionFields(
      ImmutableList<QuestionOption> options, Locale localeToEdit) {
    ImmutableList.Builder<TranslationField> fields = ImmutableList.builder();
    for (int optionIdx = 0; optionIdx < options.size(); optionIdx++) {
      LocalizedStrings optionText = options.get(optionIdx).optionText();
      fields.add(
          buildTextField(
              "options[]",
              String.format("Answer option #%d", optionIdx + 1),
              optionText,
              localeToEdit));
    }
    return fields.build();
  }

  private ImmutableList<TranslationField> buildMapSettingFields(
      ImmutableSet<QuestionSetting> settings, Locale localeToEdit) {
    ImmutableList.Builder<TranslationField> fields = ImmutableList.builder();
    settings.forEach(
        setting -> {
          if (setting.settingType().equals(MapSettingType.LOCATION_FILTER_GEO_JSON_KEY)) {
            fields.add(
                buildTextField(
                    "filters[]",
                    "Filter display name",
                    setting.localizedSettingDisplayName().orElse(LocalizedStrings.of()),
                    localeToEdit));
          }

          if (setting.settingType().equals(MapSettingType.LOCATION_TAG_GEO_JSON_KEY)) {
            fields.add(
                buildTextField(
                    "tagDisplayName",
                    "Tag display name",
                    setting.localizedSettingDisplayName().orElse(LocalizedStrings.of()),
                    localeToEdit));
            fields.add(
                buildTextareaField(
                    "tagText",
                    "Tag text",
                    setting.localizedSettingText().orElse(LocalizedStrings.of()),
                    localeToEdit,
                    /* markdown= */ false));
          }
        });
    return fields.build();
  }

  private TranslationField buildTextField(
      String name, String label, LocalizedStrings localizedStrings, Locale localeToEdit) {
    return baseField(name, label, localizedStrings, localeToEdit)
        .textarea(false)
        .markdown(false)
        .build();
  }

  private TranslationField buildTextareaField(
      String name,
      String label,
      LocalizedStrings localizedStrings,
      Locale localeToEdit,
      boolean markdown) {
    return baseField(name, label, localizedStrings, localeToEdit)
        .textarea(true)
        .markdown(markdown)
        .build();
  }

  private TranslationField.TranslationFieldBuilder baseField(
      String name, String label, LocalizedStrings localizedStrings, Locale localeToEdit) {
    return TranslationField.builder()
        .id(RandomStringUtils.randomAlphabetic(8))
        .name(name)
        .label(label)
        .value(localizedStrings.maybeGet(localeToEdit).orElse(""))
        .defaultTextHtml(
            TextFormatter.formatTextToSanitizedHTML(
                localizedStrings.getDefault(),
                /* preserveEmptyLines= */ false,
                /* addRequiredIndicator= */ false,
                /* ariaLabelForNewTabs= */ "opens in a new tab"));
  }

  /**
   * Returns the English display text for a locale. Mirrors {@code
   * TranslationFormView#getDisplayLanguage}, which the legacy j2html view uses.
   */
  private static String getDisplayLanguage(Locale locale) {
    return locale.equals(Locale.TRADITIONAL_CHINESE)
        ? "Traditional Chinese"
        : locale.getDisplayLanguage(LocalizedStrings.DEFAULT_LOCALE);
  }
}
