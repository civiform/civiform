package services.question.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.CiviFormError;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.question.QuestionOption;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.AddressQuestionDefinition.AddressValidationPredicates;
import services.question.types.TextQuestionDefinition.TextValidationPredicates;

@RunWith(JUnitParamsRunner.class)
public class QuestionDefinitionTest {
  private QuestionDefinitionBuilder builder;

  @Before
  public void setup() {
    builder =
        new QuestionDefinitionBuilder()
            .setName("name")
            .setDescription("description")
            .setQuestionType(QuestionType.TEXT)
            .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
            .setEntityType(LocalizedStrings.empty())
            .setValidationPredicates(TextValidationPredicates.builder().setMaxLength(128).build());
  }

  @Test
  public void testEquality_true() throws UnsupportedQuestionTypeException {
    QuestionDefinition question = builder.setId(123L).build();
    assertThat(question.equals(question)).isTrue();
    QuestionDefinition sameQuestion = new QuestionDefinitionBuilder(question).build();
    assertThat(question.equals(sameQuestion)).isTrue();
  }

  @Test
  public void testEquality_nullReturnsFalse() throws UnsupportedQuestionTypeException {
    QuestionDefinition question = builder.setId(123L).build();
    assertThat(question.equals(null)).isFalse();
  }

  @Test
  public void testEquality_differentPredicatesReturnsFalse()
      throws UnsupportedQuestionTypeException {
    QuestionDefinition question = builder.setId(123L).build();

    QuestionDefinition differentQuestionPredicates =
        new QuestionDefinitionBuilder(question)
            .setValidationPredicates(TextValidationPredicates.builder().setMaxLength(1).build())
            .build();
    assertThat(question.equals(differentQuestionPredicates)).isFalse();
  }

  @Test
  public void testEquality_differentHelpTextReturnsFalse() throws UnsupportedQuestionTypeException {
    QuestionDefinition question = builder.setId(123L).build();

    QuestionDefinition differentQuestionHelpText =
        new QuestionDefinitionBuilder(question)
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "different help text"))
            .build();
    assertThat(question.equals(differentQuestionHelpText)).isFalse();
  }

  @Test
  public void testEquality_differentTextReturnsFalse() throws UnsupportedQuestionTypeException {
    QuestionDefinition question = builder.setId(123L).build();

    QuestionDefinition differentQuestionText =
        new QuestionDefinitionBuilder(question)
            .setQuestionText(LocalizedStrings.of(Locale.US, "question text?"))
            .build();
    assertThat(question.equals(differentQuestionText)).isFalse();
  }

  @Test
  public void testEquality_differentQuestionTypeReturnsFalse()
      throws UnsupportedQuestionTypeException {
    QuestionDefinition question = builder.setId(123L).build();

    QuestionDefinition differentQuestionType =
        new QuestionDefinitionBuilder(question)
            .setQuestionType(QuestionType.ADDRESS)
            .setValidationPredicates(AddressValidationPredicates.create())
            .build();
    assertThat(question.equals(differentQuestionType)).isFalse();
  }

  @Test
  public void testEquality_differentIdReturnsFalse() throws UnsupportedQuestionTypeException {
    QuestionDefinition question = builder.setId(123L).build();

    QuestionDefinition unpersistedQuestion = builder.clearId().build();
    assertThat(question.equals(unpersistedQuestion)).isFalse();

    QuestionDefinition differentId = builder.setId(456L).build();
    assertThat(question.equals(differentId)).isFalse();
  }

  @Test
  public void testEquality_differentNameReturnsFalse() throws UnsupportedQuestionTypeException {
    QuestionDefinition question = builder.setId(123L).build();
    QuestionDefinition differentName = builder.setName("Different name").build();
    assertThat(question.equals(differentName)).isFalse();
  }

  @Test
  public void testEquality_differentDescriptionReturnsFalse()
      throws UnsupportedQuestionTypeException {
    QuestionDefinition question = builder.setId(123L).build();
    QuestionDefinition differentDescription =
        builder.setDescription("Different description").build();
    assertThat(question.equals(differentDescription)).isFalse();
  }

  @Test
  public void isEnumerator_false() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "", Optional.empty(), "", LocalizedStrings.of(), LocalizedStrings.empty());

    assertThat(question.isEnumerator()).isFalse();
  }

  @Test
  public void isEnumerator_true() {
    QuestionDefinition question =
        new EnumeratorQuestionDefinition(
            "",
            Optional.empty(),
            "",
            LocalizedStrings.of(),
            LocalizedStrings.empty(),
            LocalizedStrings.empty());
    assertThat(question.isEnumerator()).isTrue();
  }

  @Test
  public void isRepeated_false() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "", Optional.empty(), "", LocalizedStrings.of(), LocalizedStrings.empty());

    assertThat(question.isRepeated()).isFalse();
  }

  @Test
  public void isRepeated_true() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "", Optional.of(123L), "", LocalizedStrings.of(), LocalizedStrings.empty());
    assertThat(question.isRepeated()).isTrue();
  }

  @Test
  public void newQuestionHasCorrectFields() throws Exception {
    QuestionDefinition question =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.TEXT)
            .setId(123L)
            .setName("name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
            .setValidationPredicates(TextValidationPredicates.builder().setMinLength(0).build())
            .build();

    assertThat(question.getId()).isEqualTo(123L);
    assertThat(question.getName()).isEqualTo("name");
    assertThat(question.getDescription()).isEqualTo("description");
    assertThat(question.getQuestionText().get(Locale.US)).isEqualTo("question?");
    assertThat(question.getQuestionHelpText().get(Locale.US)).isEqualTo("help text");
    assertThat(question.getValidationPredicates())
        .isEqualTo(TextValidationPredicates.builder().setMinLength(0).build());
  }

  @Test
  public void getQuestionTextForUnknownLocale_throwsException() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "text",
            Optional.empty(),
            "",
            LocalizedStrings.of(Locale.US, "not french"),
            LocalizedStrings.empty());

    Throwable thrown = catchThrowable(() -> question.getQuestionText().get(Locale.FRANCE));

    assertThat(thrown).isInstanceOf(TranslationNotFoundException.class);
    assertThat(thrown).hasMessage("No translation was found for locale fr_FR");
  }

  @Test
  public void getQuestionHelpTextForUnknownLocale_throwsException() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "text",
            Optional.empty(),
            "",
            LocalizedStrings.of(),
            LocalizedStrings.of(Locale.US, "help text"));

    Throwable thrown = catchThrowable(() -> question.getQuestionHelpText().get(Locale.FRANCE));

    assertThat(thrown).isInstanceOf(TranslationNotFoundException.class);
    assertThat(thrown).hasMessage("No translation was found for locale fr_FR");
  }

  @Test
  public void getEmptyHelpTextForUnknownLocale_succeeds() throws TranslationNotFoundException {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "text", Optional.empty(), "", LocalizedStrings.of(), LocalizedStrings.empty());
    assertThat(question.getQuestionHelpText().get(Locale.FRANCE)).isEqualTo("");
  }

  @Test
  public void getQuestionTextOrDefault_returnsDefaultIfNotFound() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "text",
            Optional.empty(),
            "",
            LocalizedStrings.withDefaultValue("default"),
            LocalizedStrings.empty());

    assertThat(question.getQuestionText().getOrDefault(Locale.forLanguageTag("und")))
        .isEqualTo("default");
  }

  @Test
  public void getQuestionHelpTextOrDefault_returnsDefaultIfNotFound() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "text",
            Optional.empty(),
            "",
            LocalizedStrings.of(),
            LocalizedStrings.withDefaultValue("default"));

    assertThat(question.getQuestionHelpText().getOrDefault(Locale.forLanguageTag("und")))
        .isEqualTo("default");
  }

  @Test
  public void maybeGetQuestionText_returnsOptionalWithText() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "",
            Optional.empty(),
            "",
            LocalizedStrings.of(Locale.US, "hello"),
            LocalizedStrings.empty());

    assertThat(question.getQuestionText().maybeGet(Locale.US)).hasValue("hello");
  }

  @Test
  public void maybeGetQuestionText_returnsEmptyIfLocaleNotFound() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "", Optional.empty(), "", LocalizedStrings.of(), LocalizedStrings.empty());

    assertThat(question.getQuestionText().maybeGet(Locale.forLanguageTag("und"))).isEmpty();
  }

  @Test
  public void maybeGetQuestionHelpText_returnsOptionalWithText() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "",
            Optional.empty(),
            "",
            LocalizedStrings.of(),
            LocalizedStrings.of(Locale.US, "world"));

    assertThat(question.getQuestionHelpText().maybeGet(Locale.US)).hasValue("world");
  }

  @Test
  public void maybeGetQuestionHelpText_returnsEmptyIfLocaleNotFound() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "", Optional.empty(), "", LocalizedStrings.of(), LocalizedStrings.empty());

    assertThat(question.getQuestionHelpText().maybeGet(Locale.forLanguageTag("und"))).isEmpty();
  }

  @Test
  public void newQuestionHasTypeText() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "text", Optional.empty(), "", LocalizedStrings.of(), LocalizedStrings.empty());

    assertThat(question.getQuestionType()).isEqualTo(QuestionType.TEXT);
  }

  @Test
  public void validateWellFormedQuestion_returnsNoErrors() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "text",
            Optional.empty(),
            "description",
            LocalizedStrings.of(Locale.US, "question?"),
            LocalizedStrings.empty());
    assertThat(question.validate()).isEmpty();
  }

  @Test
  public void validateBadQuestion_returnsErrors() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "", Optional.empty(), "", LocalizedStrings.of(), LocalizedStrings.empty());
    assertThat(question.validate())
        .containsOnly(
            CiviFormError.of("Name cannot be blank"),
            CiviFormError.of("Description cannot be blank"),
            CiviFormError.of("Question text cannot be blank"));
  }

  @Test
  public void validate_withEnumerator_withEmptyEntityString_returnsErrors() throws Exception {
    QuestionDefinition question =
        new QuestionDefinitionBuilder()
            .setName("name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.withDefaultValue("text"))
            .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
            .setEntityType(LocalizedStrings.withDefaultValue(""))
            .setQuestionType(QuestionType.ENUMERATOR)
            .build();

    assertThat(question.validate())
        .containsOnly(CiviFormError.of("Enumerator question must have specified entity type"));
  }

  @Test
  public void validate_withRepeatedQuestion_missingEntityNameFormatString_returnsErrors()
      throws Exception {
    QuestionDefinition question =
        new QuestionDefinitionBuilder()
            .setName("name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.withDefaultValue("text"))
            .setQuestionHelpText(LocalizedStrings.withDefaultValue("help text"))
            .setEnumeratorId(Optional.of(1L))
            .setQuestionType(QuestionType.TEXT)
            .build();

    assertThat(question.validate())
        .containsOnly(
            CiviFormError.of(
                "Repeated questions must reference '$this' in the text and help text (if"
                    + " present)"));
  }

  @Test
  public void
      validate_withRepeatedQuestion_oneTranslationMissingEntityNameFormatString_returnsErrors()
          throws Exception {
    QuestionDefinition question =
        new QuestionDefinitionBuilder()
            .setName("name")
            .setDescription("description")
            .setQuestionText(
                LocalizedStrings.of(
                    Locale.US, "$this is present", Locale.FRANCE, "$this is also present"))
            .setQuestionHelpText(
                LocalizedStrings.of(
                    Locale.US, "$this is present", Locale.FRANCE, "this is not present"))
            .setEnumeratorId(Optional.of(1L))
            .setQuestionType(QuestionType.TEXT)
            .build();

    assertThat(question.validate())
        .containsOnly(
            CiviFormError.of(
                "Repeated questions must reference '$this' in the text and help text (if"
                    + " present)"));
  }

  @Test
  public void validate_withRepeatedQuestion_withNoHelpText_returnsNoErrors() throws Exception {
    QuestionDefinition question =
        new QuestionDefinitionBuilder()
            .setName("name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.withDefaultValue("something with $this"))
            .setEnumeratorId(Optional.of(1L))
            .setQuestionType(QuestionType.TEXT)
            .build();

    assertThat(question.validate()).isEmpty();
  }

  @Test
  public void validate_localeHasBlankText_returnsError() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "test",
            Optional.empty(),
            "test",
            LocalizedStrings.of(Locale.US, ""),
            LocalizedStrings.empty());
    assertThat(question.validate()).containsOnly(CiviFormError.of("Question text cannot be blank"));
  }

  @Test
  public void validate_multiOptionQuestion_withoutOptions_returnsError() {
    QuestionDefinition question =
        new CheckboxQuestionDefinition(
            "test",
            Optional.empty(),
            "test",
            LocalizedStrings.withDefaultValue("test"),
            LocalizedStrings.empty(),
            ImmutableList.of(),
            MultiOptionQuestionDefinition.MultiOptionValidationPredicates.create());
    assertThat(question.validate())
        .containsOnly(CiviFormError.of("Multi-option questions must have at least one option"));
  }

  @Test
  public void validate_multiOptionQuestion_withBlankOption_returnsError() {
    QuestionDefinition question =
        new CheckboxQuestionDefinition(
            "test",
            Optional.empty(),
            "test",
            LocalizedStrings.withDefaultValue("test"),
            LocalizedStrings.empty(),
            ImmutableList.of(QuestionOption.create(1L, LocalizedStrings.withDefaultValue(""))),
            MultiOptionQuestionDefinition.MultiOptionValidationPredicates.create());
    assertThat(question.validate())
        .containsOnly(CiviFormError.of("Multi-option questions cannot have blank options"));
  }

  @Test
  public void validate_multiOptionQuestion_withDuplicateOptions_returnsError() {
    QuestionDefinition question =
        new CheckboxQuestionDefinition(
            "test",
            Optional.empty(),
            "test",
            LocalizedStrings.withDefaultValue("test"),
            LocalizedStrings.empty(),
            ImmutableList.of(
                QuestionOption.create(1L, LocalizedStrings.withDefaultValue("a")),
                QuestionOption.create(2L, LocalizedStrings.withDefaultValue("a"))),
            MultiOptionQuestionDefinition.MultiOptionValidationPredicates.create());
    assertThat(question.validate())
        .containsOnly(CiviFormError.of("Multi-option question options must be unique"));
  }

  @Test
  public void getSupportedLocales_onlyReturnsFullySupportedLocales() {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            "test",
            Optional.empty(),
            "test",
            LocalizedStrings.of(
                Locale.US,
                "question?",
                Locale.forLanguageTag("es-US"),
                "pregunta",
                Locale.FRANCE,
                "question"),
            LocalizedStrings.of(
                Locale.US,
                "help",
                Locale.forLanguageTag("es-US"),
                "ayuda",
                Locale.GERMAN,
                "Hilfe"));

    assertThat(definition.getSupportedLocales())
        .containsExactly(Locale.US, Locale.forLanguageTag("es-US"));
  }

  @Test
  public void getSupportedLocales_emptyHelpText_returnsLocalesForQuestionText() {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            "test",
            Optional.empty(),
            "test",
            LocalizedStrings.of(
                Locale.US,
                "question?",
                Locale.forLanguageTag("es-US"),
                "pregunta",
                Locale.FRANCE,
                "question"),
            LocalizedStrings.empty());

    assertThat(definition.getSupportedLocales())
        .containsExactly(Locale.US, Locale.forLanguageTag("es-US"), Locale.FRANCE);
  }
}
