package mapping.admin.questions;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.MapSettingType;
import services.question.QuestionOption;
import services.question.QuestionSetting;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.MapQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.TextQuestionDefinition;
import views.admin.questions.QuestionTranslationPageViewModel;
import views.admin.questions.QuestionTranslationPageViewModel.TranslationField;

public final class QuestionTranslationPageMapperTest {

  private static final Locale SPANISH = Locale.forLanguageTag("es-US");
  private static final Locale KOREAN = Locale.forLanguageTag("ko");
  private static final ImmutableList<Locale> TRANSLATABLE_LOCALES =
      ImmutableList.of(SPANISH, KOREAN);

  private QuestionTranslationPageMapper mapper;

  @Before
  public void setup() {
    mapper = new QuestionTranslationPageMapper();
  }

  private static QuestionDefinitionConfig.Builder configBuilder() {
    return QuestionDefinitionConfig.builder()
        .setName("my-question")
        .setDescription("description")
        .setQuestionText(LocalizedStrings.of(Locale.US, "What is your name?", SPANISH, "Nombre?"))
        .setId(5L);
  }

  private static QuestionDefinition textQuestion() {
    return new TextQuestionDefinition(configBuilder().build());
  }

  private QuestionTranslationPageViewModel map(QuestionDefinition question) {
    return mapper.map(
        question, SPANISH, TRANSLATABLE_LOCALES, /* errorMessage= */ Optional.empty());
  }

  @Test
  public void map_setsTitle() {
    QuestionTranslationPageViewModel result = map(textQuestion());

    assertThat(result.getTitle()).isEqualTo("Manage question translations: my-question");
  }

  @Test
  public void map_setsFormActionUrl() {
    QuestionTranslationPageViewModel result = map(textQuestion());

    assertThat(result.getFormActionUrl())
        .isEqualTo("/admin/questions/my-question/translations/es-US");
  }

  @Test
  public void map_setsLanguageLinkHrefs() {
    QuestionTranslationPageViewModel result = map(textQuestion());

    assertThat(result.getLanguageLinks().get(0).getHref())
        .isEqualTo("/admin/questions/my-question/translations/es-US/edit");
  }

  @Test
  public void map_setsEditDefaultUrl() {
    QuestionTranslationPageViewModel result = map(textQuestion());

    assertThat(result.getEditDefaultUrl()).isEqualTo("/admin/questions/5/edit");
  }

  @Test
  public void map_setsSaveButtonText() {
    QuestionTranslationPageViewModel result = map(textQuestion());

    assertThat(result.getSaveButtonText()).isEqualTo("Save Spanish updates");
  }

  @Test
  public void map_setsConcurrencyToken() {
    UUID token = UUID.randomUUID();

    QuestionTranslationPageViewModel result =
        map(new TextQuestionDefinition(configBuilder().setConcurrencyToken(token).build()));

    assertThat(result.getConcurrencyToken()).isEqualTo(token.toString());
  }

  @Test
  public void map_withoutConcurrencyToken_setsEmptyString() {
    QuestionTranslationPageViewModel result = map(textQuestion());

    assertThat(result.getConcurrencyToken()).isEmpty();
  }

  @Test
  public void map_setsOneLanguageLinkPerLocaleAndMarksTheCurrentOne() {
    QuestionTranslationPageViewModel result = map(textQuestion());

    assertThat(result.getLanguageLinks()).hasSize(2);
    assertThat(result.getLanguageLinks().get(0).getDisplayLanguage()).isEqualTo("Spanish");
    assertThat(result.getLanguageLinks().get(0).isSelected()).isTrue();
    assertThat(result.getLanguageLinks().get(1).getDisplayLanguage()).isEqualTo("Korean");
    assertThat(result.getLanguageLinks().get(1).isSelected()).isFalse();
  }

  @Test
  public void map_setsQuestionTextFieldFromTheLocaleBeingEdited() {
    QuestionTranslationPageViewModel result = map(textQuestion());

    TranslationField field = result.getQuestionTextField();
    assertThat(field.getName()).isEqualTo("questionText");
    assertThat(field.getLabel()).isEqualTo("Question text");
    assertThat(field.getValue()).isEqualTo("Nombre?");
    assertThat(field.isTextarea()).isTrue();
    assertThat(field.isMarkdown()).isTrue();
    assertThat(field.getId()).isNotEmpty();
  }

  @Test
  public void map_untranslatedField_hasEmptyValue() {
    QuestionTranslationPageViewModel result =
        mapper.map(textQuestion(), KOREAN, TRANSLATABLE_LOCALES, Optional.empty());

    assertThat(result.getQuestionTextField().getValue()).isEmpty();
  }

  @Test
  public void map_setsDefaultTextAsRenderedMarkdown() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            configBuilder()
                .setQuestionText(LocalizedStrings.of(Locale.US, "**bold** text"))
                .build());

    QuestionTranslationPageViewModel result = map(question);

    assertThat(result.getQuestionTextField().getDefaultTextHtml())
        .contains("<strong>bold</strong>");
  }

  @Test
  public void map_withoutHelpText_hidesHelpTextField() {
    QuestionTranslationPageViewModel result = map(textQuestion());

    assertThat(result.isShowHelpText()).isFalse();
    assertThat(result.getQuestionHelpTextField()).isNull();
  }

  @Test
  public void map_withHelpText_setsHelpTextField() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            configBuilder()
                .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help", SPANISH, "ayuda"))
                .build());

    QuestionTranslationPageViewModel result = map(question);

    assertThat(result.isShowHelpText()).isTrue();
    assertThat(result.getQuestionHelpTextField().getName()).isEqualTo("questionHelpText");
    assertThat(result.getQuestionHelpTextField().getValue()).isEqualTo("ayuda");
  }

  @Test
  public void map_textQuestion_hasNoTypeSpecificFields() {
    QuestionTranslationPageViewModel result = map(textQuestion());

    assertThat(result.isShowYesNoNote()).isFalse();
    assertThat(result.isShowAnswerOptions()).isFalse();
    assertThat(result.isShowEntityType()).isFalse();
    assertThat(result.isShowMapSettings()).isFalse();
  }

  @Test
  public void map_multiOptionQuestion_setsNumberedAnswerOptionFields() {
    QuestionDefinition question =
        new MultiOptionQuestionDefinition(
            configBuilder().build(),
            ImmutableList.of(
                QuestionOption.create(
                    1L, "cat admin", LocalizedStrings.of(Locale.US, "cat", SPANISH, "gato")),
                QuestionOption.create(2L, "dog admin", LocalizedStrings.of(Locale.US, "dog"))),
            MultiOptionQuestionType.RADIO_BUTTON);

    QuestionTranslationPageViewModel result = map(question);

    assertThat(result.isShowAnswerOptions()).isTrue();
    assertThat(result.getAnswerOptionFields()).hasSize(2);
    assertThat(result.getAnswerOptionFields().get(0).getLabel()).isEqualTo("Answer option #1");
    assertThat(result.getAnswerOptionFields().get(0).getName()).isEqualTo("options[]");
    assertThat(result.getAnswerOptionFields().get(0).getValue()).isEqualTo("gato");
    assertThat(result.getAnswerOptionFields().get(0).isTextarea()).isFalse();
    assertThat(result.getAnswerOptionFields().get(1).getLabel()).isEqualTo("Answer option #2");
    assertThat(result.getAnswerOptionFields().get(1).getValue()).isEmpty();
  }

  @Test
  public void map_multiOptionQuestionWithoutOptions_hidesAnswerOptions() {
    QuestionDefinition question =
        new MultiOptionQuestionDefinition(
            configBuilder().build(), ImmutableList.of(), MultiOptionQuestionType.CHECKBOX);

    QuestionTranslationPageViewModel result = map(question);

    assertThat(result.isShowAnswerOptions()).isFalse();
    assertThat(result.getAnswerOptionFields()).isEmpty();
  }

  @Test
  public void map_yesNoQuestion_showsPreTranslatedNoteOnly() {
    QuestionDefinition question =
        new MultiOptionQuestionDefinition(
            configBuilder().build(),
            ImmutableList.of(
                QuestionOption.create(1L, "yes", LocalizedStrings.of(Locale.US, "Yes"))),
            MultiOptionQuestionType.YES_NO);

    QuestionTranslationPageViewModel result = map(question);

    assertThat(result.isShowYesNoNote()).isTrue();
    assertThat(result.isShowAnswerOptions()).isFalse();
  }

  @Test
  public void map_enumeratorQuestion_setsEntityTypeField() {
    QuestionDefinition question =
        new EnumeratorQuestionDefinition(
            configBuilder().build(),
            LocalizedStrings.of(Locale.US, "household member", SPANISH, "miembro"));

    QuestionTranslationPageViewModel result = map(question);

    assertThat(result.isShowEntityType()).isTrue();
    assertThat(result.getEntityTypeField().getName()).isEqualTo("entityType");
    assertThat(result.getEntityTypeField().getLabel()).isEqualTo("What is being enumerated");
    assertThat(result.getEntityTypeField().getValue()).isEqualTo("miembro");
    assertThat(result.getEntityTypeField().isTextarea()).isTrue();
    assertThat(result.getEntityTypeField().isMarkdown()).isTrue();
  }

  @Test
  public void map_mapQuestion_setsFilterAndTagFields() {
    QuestionDefinition question =
        new MapQuestionDefinition(
            configBuilder()
                .setQuestionSettings(
                    ImmutableSet.of(
                        QuestionSetting.create(
                            "filterKey",
                            MapSettingType.LOCATION_FILTER_GEO_JSON_KEY,
                            Optional.of(
                                LocalizedStrings.of(Locale.US, "Services", SPANISH, "Servicios"))),
                        // A tag setting always carries both a display name and
                        // its alert text, the way MapQuestionForm builds it.
                        QuestionSetting.create(
                            "tagKey",
                            MapSettingType.LOCATION_TAG_GEO_JSON_KEY,
                            Optional.of(LocalizedStrings.of(Locale.US, "Tag")),
                            /* settingValue= */ Optional.of("tagValue"),
                            Optional.of(
                                LocalizedStrings.of(Locale.US, "Tag alert", SPANISH, "Alerta")))))
                .build());

    QuestionTranslationPageViewModel result = map(question);

    assertThat(result.isShowMapSettings()).isTrue();
    assertThat(result.getMapSettingFields().stream().map(TranslationField::getName))
        .containsExactlyInAnyOrder("filters[]", "tagDisplayName", "tagText");
    TranslationField filterField =
        result.getMapSettingFields().stream()
            .filter(field -> field.getName().equals("filters[]"))
            .findFirst()
            .orElseThrow();
    assertThat(filterField.getLabel()).isEqualTo("Filter display name");
    assertThat(filterField.getValue()).isEqualTo("Servicios");
    assertThat(filterField.isTextarea()).isFalse();
    TranslationField tagTextField =
        result.getMapSettingFields().stream()
            .filter(field -> field.getName().equals("tagText"))
            .findFirst()
            .orElseThrow();
    assertThat(tagTextField.getLabel()).isEqualTo("Tag text");
    assertThat(tagTextField.getValue()).isEqualTo("Alerta");
    assertThat(tagTextField.isTextarea()).isTrue();
    assertThat(tagTextField.isMarkdown()).isFalse();
  }

  @Test
  public void map_mapQuestionWithoutSettings_hidesMapSettings() {
    QuestionTranslationPageViewModel result =
        map(new MapQuestionDefinition(configBuilder().build()));

    assertThat(result.isShowMapSettings()).isFalse();
    assertThat(result.getMapSettingFields()).isEmpty();
  }

  @Test
  public void map_withErrorMessage_prefixesErrorAndSetsToastId() {
    QuestionTranslationPageViewModel result =
        mapper.map(textQuestion(), SPANISH, TRANSLATABLE_LOCALES, Optional.of("something broke"));

    assertThat(result.getErrorMessage()).hasValue("Error: something broke");
    assertThat(result.getErrorToastId()).isNotEmpty();
  }

  @Test
  public void map_withoutErrorMessage_hasNoToastId() {
    QuestionTranslationPageViewModel result = map(textQuestion());

    assertThat(result.getErrorMessage()).isEmpty();
    assertThat(result.getErrorToastId()).isNull();
  }
}
