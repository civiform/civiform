package services.question.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
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
import services.question.types.MultiOptionQuestionDefinition.MultiOptionQuestionType;
import services.question.types.TextQuestionDefinition.TextValidationPredicates;

@RunWith(JUnitParamsRunner.class)
public class QuestionDefinitionTest {
  private QuestionDefinitionBuilder builder;
  private QuestionDefinitionConfig.Builder configBuilder;

  @Before
  public void setup() {
    builder =
        new QuestionDefinitionBuilder()
            .setName("name")
            .setDescription("description")
            .setQuestionType(QuestionType.TEXT)
            .setQuestionText(LocalizedStrings.of(Locale.US, "question?"));
    configBuilder =
        QuestionDefinitionConfig.builder()
            .setName("name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "question?"));
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
  public void testEquality_differentConcurrencyTokenReturnsFalse()
      throws UnsupportedQuestionTypeException {
    QuestionDefinition question = builder.setConcurrencyToken(UUID.randomUUID()).build();
    QuestionDefinition questionWithDifferentToken =
        builder.setConcurrencyToken(UUID.randomUUID()).build();
    assertThat(question.equals(questionWithDifferentToken)).isFalse();
  }

  @Test
  public void isEnumerator_false() {
    QuestionDefinition question = new TextQuestionDefinition(configBuilder.build());

    assertThat(question.isEnumerator()).isFalse();
  }

  @Test
  public void isEnumerator_true() {
    QuestionDefinition question =
        new EnumeratorQuestionDefinition(configBuilder.build(), LocalizedStrings.empty());
    assertThat(question.isEnumerator()).isTrue();
  }

  @Test
  public void isRepeated_false() {
    QuestionDefinition question = new TextQuestionDefinition(configBuilder.build());

    assertThat(question.isRepeated()).isFalse();
  }

  @Test
  public void isRepeated_true() {
    QuestionDefinition question =
        new TextQuestionDefinition(configBuilder.setEnumeratorId(Optional.of(123L)).build());
    assertThat(question.isRepeated()).isTrue();
  }

  @Test
  public void newQuestionHasCorrectFields() throws Exception {
    QuestionDefinitionConfig config =
        QuestionDefinitionConfig.builder()
            .setName("name")
            .setDescription("description")
            .setQuestionText(LocalizedStrings.of(Locale.US, "question?"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text"))
            .setId(123L)
            .setValidationPredicates(TextValidationPredicates.builder().setMaxLength(128).build())
            .setUniversal(false)
            .build();
    QuestionDefinition question = new TextQuestionDefinition(config);

    assertThat(question.getQuestionType()).isEqualTo(QuestionType.TEXT);
    assertThat(question.getName()).isEqualTo("name");
    assertThat(question.getDescription()).isEqualTo("description");
    assertThat(question.getQuestionText().get(Locale.US)).isEqualTo("question?");
    assertThat(question.getQuestionHelpText().get(Locale.US)).isEqualTo("help text");
    assertThat(question.getId()).isEqualTo(123L);
    assertThat(question.getValidationPredicates())
        .isEqualTo(TextValidationPredicates.builder().setMaxLength(128).build());
    assertThat(question.isUniversal()).isFalse();
  }

  @Test
  public void getQuestionTextForUnknownLocale_throwsException() {
    QuestionDefinition question = new TextQuestionDefinition(configBuilder.build());

    Throwable thrown = catchThrowable(() -> question.getQuestionText().get(Locale.FRANCE));

    assertThat(thrown).isInstanceOf(TranslationNotFoundException.class);
    assertThat(thrown).hasMessage("No translation was found for locale fr_FR");
  }

  @Test
  public void getQuestionHelpTextForUnknownLocale_throwsException() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            configBuilder.setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text")).build());

    Throwable thrown = catchThrowable(() -> question.getQuestionHelpText().get(Locale.FRANCE));

    assertThat(thrown).isInstanceOf(TranslationNotFoundException.class);
    assertThat(thrown).hasMessage("No translation was found for locale fr_FR");
  }

  @Test
  public void getEmptyHelpTextForUnknownLocale_succeeds() throws TranslationNotFoundException {
    QuestionDefinition question =
        new TextQuestionDefinition(
            configBuilder
                .setQuestionText(LocalizedStrings.of())
                .setQuestionHelpText(LocalizedStrings.empty())
                .build());
    assertThat(question.getQuestionHelpText().get(Locale.FRANCE)).isEqualTo("");
  }

  @Test
  public void getQuestionTextOrDefault_returnsDefaultIfNotFound() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            configBuilder.setQuestionText(LocalizedStrings.withDefaultValue("default")).build());

    assertThat(question.getQuestionText().getOrDefault(Locale.forLanguageTag("und")))
        .isEqualTo("default");
  }

  @Test
  public void getQuestionHelpTextOrDefault_returnsDefaultIfNotFound() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            configBuilder
                .setQuestionHelpText(LocalizedStrings.withDefaultValue("default"))
                .build());

    assertThat(question.getQuestionHelpText().getOrDefault(Locale.forLanguageTag("und")))
        .isEqualTo("default");
  }

  @Test
  public void maybeGetQuestionText_returnsOptionalWithText() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            configBuilder.setQuestionText(LocalizedStrings.of(Locale.US, "question?")).build());

    assertThat(question.getQuestionText().maybeGet(Locale.US)).hasValue("question?");
  }

  @Test
  public void maybeGetQuestionText_returnsEmptyIfLocaleNotFound() {
    QuestionDefinition question = new TextQuestionDefinition(configBuilder.build());

    assertThat(question.getQuestionText().maybeGet(Locale.forLanguageTag("und"))).isEmpty();
  }

  @Test
  public void maybeGetQuestionHelpText_returnsOptionalWithText() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            configBuilder.setQuestionHelpText(LocalizedStrings.of(Locale.US, "help text")).build());

    assertThat(question.getQuestionHelpText().maybeGet(Locale.US)).hasValue("help text");
  }

  @Test
  public void maybeGetQuestionHelpText_returnsEmptyIfLocaleNotFound() {
    QuestionDefinition question = new TextQuestionDefinition(configBuilder.build());

    assertThat(question.getQuestionHelpText().maybeGet(Locale.forLanguageTag("und"))).isEmpty();
  }

  @Test
  public void newQuestionHasTypeText() {
    QuestionDefinition question = new TextQuestionDefinition(configBuilder.build());

    assertThat(question.getQuestionType()).isEqualTo(QuestionType.TEXT);
  }

  @Test
  public void validateWellFormedQuestion_returnsNoErrors() {
    QuestionDefinition question = new TextQuestionDefinition(configBuilder.build());
    assertThat(question.validate()).isEmpty();
  }

  @Test
  public void validateBadQuestion_returnsErrors() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            configBuilder
                .setName("")
                .setDescription("")
                .setQuestionText(LocalizedStrings.of())
                .setQuestionHelpText(LocalizedStrings.empty())
                .build());
    assertThat(question.validate())
        .containsOnly(
            CiviFormError.of("Administrative identifier cannot be blank"),
            CiviFormError.of("Question text cannot be blank"));
  }

  @Test
  public void validate_withRepeatedQuestion_missingEntityNameFormatString_returnsErrors()
      throws Exception {
    QuestionDefinition question = builder.setEnumeratorId(Optional.of(1L)).build();

    assertThat(question.validate())
        .containsOnly(CiviFormError.of("Repeated questions must reference '$this' in the text"));
  }

  @Test
  public void
      validate_withRepeatedQuestion_oneTranslationMissingEntityNameFormatString_returnsErrors()
          throws Exception {
    QuestionDefinition question =
        builder
            .setQuestionText(
                LocalizedStrings.of(
                    Locale.US, "$this is present", Locale.FRANCE, "this is not present"))
            .setQuestionHelpText(
                LocalizedStrings.of(
                    Locale.US, "$this is present", Locale.FRANCE, "$this is also present"))
            .setEnumeratorId(Optional.of(1L))
            .build();

    assertThat(question.validate())
        .containsOnly(CiviFormError.of("Repeated questions must reference '$this' in the text"));
  }

  @Test
  public void validate_withRepeatedQuestion_withHelpTextThatDoesHaveFormatString_returnsNoErrors()
      throws Exception {
    QuestionDefinition question =
        builder
            .setQuestionText(
                LocalizedStrings.of(
                    Locale.US, "$this is present", Locale.FRANCE, "$this is present"))
            .setQuestionHelpText(
                LocalizedStrings.of(
                    Locale.US, "$this is present", Locale.FRANCE, "$this is present"))
            .setEnumeratorId(Optional.of(1L))
            .build();

    assertThat(question.validate()).isEmpty();
  }

  @Test
  public void
      validate_withRepeatedQuestion_withHelpTextThatDoesNotHaveFormatString_returnsNoErrors()
          throws Exception {
    QuestionDefinition question =
        builder
            .setQuestionText(
                LocalizedStrings.of(
                    Locale.US, "$this is present", Locale.FRANCE, "$this is present"))
            .setQuestionHelpText(
                LocalizedStrings.of(
                    Locale.US, "this is not present", Locale.FRANCE, "this is not present"))
            .setEnumeratorId(Optional.of(1L))
            .build();

    assertThat(question.validate()).isEmpty();
  }

  @Test
  public void validate_withRepeatedQuestion_withNoHelpText_returnsNoErrors() throws Exception {
    QuestionDefinition question =
        builder
            .setQuestionText(LocalizedStrings.withDefaultValue("something with $this"))
            .setQuestionHelpText(LocalizedStrings.empty())
            .setEnumeratorId(Optional.of(1L))
            .build();

    assertThat(question.validate()).isEmpty();
  }

  @Test
  public void validate_localeHasBlankText_returnsError() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            configBuilder
                .setQuestionText(LocalizedStrings.of(Locale.US, ""))
                .setQuestionHelpText(LocalizedStrings.empty())
                .build());
    assertThat(question.validate()).containsOnly(CiviFormError.of("Question text cannot be blank"));
  }

  @Test
  public void validate_nameIsNotEntityName() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            configBuilder
                .setName("entity name")
                .setDescription("test")
                .setQuestionText(LocalizedStrings.of(Locale.US, "Entity Name Test"))
                .build());
    assertThat(question.validate())
        .containsOnly(CiviFormError.of("Administrative identifier 'entity name' is not allowed"));
  }

  @Test
  public void validate_nameIsNotEntityMetadata() {
    QuestionDefinition question =
        new TextQuestionDefinition(
            configBuilder
                .setName("entity metadata")
                .setDescription("test")
                .setQuestionText(LocalizedStrings.of(Locale.US, "Entity Metadata Test"))
                .build());
    assertThat(question.validate())
        .containsOnly(
            CiviFormError.of("Administrative identifier 'entity metadata' is not allowed"));
  }

  @Test
  public void validate_throwsExceptionWhenQuestionTypesMismatched() {
    QuestionDefinitionConfig config = configBuilder.build();
    ImmutableList<QuestionOption> questionOptions =
        ImmutableList.of(
            QuestionOption.create(1L, "a_one-1", LocalizedStrings.withDefaultValue("a")),
            QuestionOption.create(2L, "b_two-2", LocalizedStrings.withDefaultValue("b")));
    QuestionDefinition previousQuestion =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.CHECKBOX);
    QuestionDefinition question =
        new MultiOptionQuestionDefinition(
            config, questionOptions, MultiOptionQuestionType.DROPDOWN);

    var throwableAssert =
        assertThatThrownBy(() -> question.validate(Optional.of(previousQuestion)))
            .isInstanceOf(IllegalArgumentException.class);
    throwableAssert.hasMessage(
        "The previous version of the question definition must be of the same question type as the"
            + " updated version.");
  }

  @Test
  public void getSupportedLocales_onlyReturnsFullySupportedLocales() {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            configBuilder
                .setQuestionText(
                    LocalizedStrings.of(
                        Locale.US,
                        "question?",
                        Locale.forLanguageTag("es-US"),
                        "pregunta",
                        Locale.FRANCE,
                        "question"))
                .setQuestionHelpText(
                    LocalizedStrings.of(
                        Locale.US,
                        "help",
                        Locale.forLanguageTag("es-US"),
                        "ayuda",
                        Locale.GERMAN,
                        "Hilfe"))
                .build());

    assertThat(definition.getSupportedLocales())
        .containsExactly(Locale.US, Locale.forLanguageTag("es-US"));
  }

  @Test
  public void getSupportedLocales_emptyHelpText_returnsLocalesForQuestionText() {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            configBuilder
                .setQuestionText(
                    LocalizedStrings.of(
                        Locale.US,
                        "question?",
                        Locale.forLanguageTag("es-US"),
                        "pregunta",
                        Locale.FRANCE,
                        "question"))
                .setQuestionHelpText(LocalizedStrings.empty())
                .build());

    assertThat(definition.getSupportedLocales())
        .containsExactly(Locale.US, Locale.forLanguageTag("es-US"), Locale.FRANCE);
  }
}
