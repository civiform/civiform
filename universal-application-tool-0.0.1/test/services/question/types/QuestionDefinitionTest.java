package services.question.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Locale;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.CiviFormError;
import services.LocalizedStrings;
import services.Path;
import services.TranslationNotFoundException;
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
            .setPath(Path.create("applicant.name"))
            .setDescription("description")
            .setQuestionType(QuestionType.TEXT)
            .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
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
            "",
            Path.empty(),
            Optional.empty(),
            "",
            LocalizedStrings.of(),
            LocalizedStrings.empty());

    assertThat(question.isEnumerator()).isFalse();
  }

  @Test
  public void isEnumerator_true() {
    QuestionDefinition question =
        new EnumeratorQuestionDefinition(
            "",
            Path.empty(),
            Optional.empty(),
            "",
            LocalizedStrings.of(),
            LocalizedStrings.empty());
    assertThat(question.isEnumerator()).isTrue();
  }

  @Test
  public void isRepeated_false() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "",
            Path.empty(),
            Optional.empty(),
            "",
            LocalizedStrings.of(),
            LocalizedStrings.empty());

    assertThat(question.isRepeated()).isFalse();
  }

  @Test
  public void isRepeated_true() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "",
            Path.empty(),
            Optional.of(123L),
            "",
            LocalizedStrings.of(),
            LocalizedStrings.empty());
    assertThat(question.isRepeated()).isTrue();
  }

  @Test
  public void newQuestionHasCorrectFields() throws Exception {
    QuestionDefinition question =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.TEXT)
            .setId(123L)
            .setName("name")
            .setPath(Path.create("applicant.name"))
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
            .setValidationPredicates(TextValidationPredicates.builder().setMinLength(0).build())
            .build();

    assertThat(question.getId()).isEqualTo(123L);
    assertThat(question.getName()).isEqualTo("name");
    assertThat(question.getPath().toString()).isEqualTo("applicant.name");
    assertThat(question.getDescription()).isEqualTo("description");
    assertThat(question.getQuestionText().get(Locale.US)).isEqualTo("question?");
    assertThat(question.getQuestionHelpText().get(Locale.US)).isEqualTo("help text");
    assertThat(question.getValidationPredicates())
        .isEqualTo(TextValidationPredicates.builder().setMinLength(0).build());
  }

  @Test
  public void getQuestionTextForUnknownLocale_throwsException() {
    String questionPath = "applicant.text";
    QuestionDefinition question =
        new TextQuestionDefinition(
            "text",
            Path.create(questionPath),
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
    String questionPath = "applicant.text";
    QuestionDefinition question =
        new TextQuestionDefinition(
            "text",
            Path.create(questionPath),
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
            "text",
            Path.create("applicant.text"),
            Optional.empty(),
            "",
            LocalizedStrings.of(),
            LocalizedStrings.empty());
    assertThat(question.getQuestionHelpText().get(Locale.FRANCE)).isEqualTo("");
  }

  @Test
  public void getQuestionTextOrDefault_returnsDefaultIfNotFound() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "text",
            Path.create("applicant.text"),
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
            Path.create("applicant.text"),
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
            Path.empty(),
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
            "",
            Path.empty(),
            Optional.empty(),
            "",
            LocalizedStrings.of(),
            LocalizedStrings.empty());

    assertThat(question.getQuestionText().maybeGet(Locale.forLanguageTag("und"))).isEmpty();
  }

  @Test
  public void maybeGetQuestionHelpText_returnsOptionalWithText() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "",
            Path.empty(),
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
            "",
            Path.empty(),
            Optional.empty(),
            "",
            LocalizedStrings.of(),
            LocalizedStrings.empty());

    assertThat(question.getQuestionHelpText().maybeGet(Locale.forLanguageTag("und"))).isEmpty();
  }

  @Test
  public void newQuestionHasTypeText() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "text",
            Path.create("applicant.text"),
            Optional.empty(),
            "",
            LocalizedStrings.of(),
            LocalizedStrings.empty());

    assertThat(question.getQuestionType()).isEqualTo(QuestionType.TEXT);
  }

  @Test
  public void validateWellFormedQuestion_returnsNoErrors() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "text",
            Path.create("applicant.text"),
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
            "",
            Path.create("applicant.text"),
            Optional.empty(),
            "",
            LocalizedStrings.of(),
            LocalizedStrings.empty());
    assertThat(question.validate())
        .containsOnly(
            CiviFormError.of("Name cannot be blank"),
            CiviFormError.of("Description cannot be blank"),
            CiviFormError.of("Question text cannot be blank"));
  }

  @Test
  public void validate_localeHasBlankText_returnsError() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            "test",
            Path.empty(),
            Optional.empty(),
            "test",
            LocalizedStrings.of(Locale.US, ""),
            LocalizedStrings.empty());
    assertThat(question.validate()).containsOnly(CiviFormError.of("Question text cannot be blank"));
  }

  @Test
  public void getSupportedLocales_onlyReturnsFullySupportedLocales() {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            "test",
            Path.create("applicant.test"),
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
}
