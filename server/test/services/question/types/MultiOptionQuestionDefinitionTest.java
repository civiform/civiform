package services.question.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import org.junit.Test;
import services.LocalizedStrings;
import services.TranslationNotFoundException;
import services.question.LocalizedQuestionOption;
import services.question.QuestionOption;
import services.question.exceptions.UnsupportedQuestionTypeException;

public class MultiOptionQuestionDefinitionTest {

  @Test
  public void buildMultiSelectQuestion() throws UnsupportedQuestionTypeException {
    ImmutableList<QuestionOption> options =
        ImmutableList.of(
            QuestionOption.create(1L, "opt1", LocalizedStrings.of(Locale.US, "option 1")),
            QuestionOption.create(2L, "opt1", LocalizedStrings.of(Locale.US, "option 2")));

    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.DROPDOWN)
            .setName("")
            .setDescription("")
            .setQuestionText(LocalizedStrings.of())
            .setQuestionHelpText(LocalizedStrings.empty())
            .setQuestionOptions(options)
            .build();

    MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) definition;

    assertThat(multiOption.getOptions()).isEqualTo(options);
  }

  @Test
  public void getSupportedLocales_onlyIncludesLocalesSupportedByQuestionTextAndOptions()
      throws UnsupportedQuestionTypeException {
    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.DROPDOWN)
            .setName("")
            .setDescription("")
            .setQuestionText(LocalizedStrings.of(Locale.US, "test", Locale.FRANCE, "test"))
            .setQuestionHelpText(LocalizedStrings.of(Locale.US, "test", Locale.FRANCE, "test"))
            .setQuestionOptions(
                ImmutableList.of(
                    QuestionOption.create(1L, "opt1", LocalizedStrings.of(Locale.US, "option 1"))))
            .build();

    assertThat(definition.getSupportedLocales()).containsExactly(Locale.US);
  }

  @Test
  public void getSupportedLocales_selectsSmallestSetOfLocalesFromOptions()
      throws UnsupportedQuestionTypeException {
    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.DROPDOWN)
            .setName("")
            .setDescription("")
            .setQuestionText(
                LocalizedStrings.of(Locale.US, "test", Locale.FRANCE, "test", Locale.UK, "test"))
            .setQuestionHelpText(
                LocalizedStrings.of(Locale.US, "test", Locale.FRANCE, "test", Locale.UK, "test"))
            .setQuestionOptions(
                ImmutableList.of(
                    QuestionOption.create(
                        1L,
                        "opt1",
                        LocalizedStrings.of(Locale.US, "1", Locale.FRANCE, "1", Locale.UK, "1")),
                    QuestionOption.create(
                        1L, "opt2", LocalizedStrings.of(Locale.US, "2", Locale.FRANCE, "2"))))
            .build();

    assertThat(definition.getSupportedLocales()).containsExactly(Locale.US, Locale.FRANCE);
  }

  @Test
  public void getOptionsForLocale_failsForMissingLocale() throws UnsupportedQuestionTypeException {
    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.DROPDOWN)
            .setName("")
            .setDescription("")
            .setQuestionText(LocalizedStrings.of())
            .setQuestionHelpText(LocalizedStrings.empty())
            .setQuestionOptions(
                ImmutableList.of(
                    QuestionOption.create(1L, "ay", LocalizedStrings.of(Locale.US, "option 1"))))
            .build();

    MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) definition;
    Throwable thrown = catchThrowable(() -> multiOption.getOptionsForLocale(Locale.CANADA_FRENCH));

    assertThat(thrown).isInstanceOf(TranslationNotFoundException.class);
    assertThat(thrown).hasMessageContaining("ca");
  }

  @Test
  public void getOptionsForLocale_returnsAllTranslations() throws Exception {
    ImmutableList<QuestionOption> options =
        ImmutableList.of(
            QuestionOption.create(
                1L, "ay", LocalizedStrings.of(Locale.US, "one", Locale.GERMAN, "eins")),
            QuestionOption.create(
                2L, "bee", LocalizedStrings.of(Locale.US, "two", Locale.GERMAN, "zwei")));
    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.DROPDOWN)
            .setName("")
            .setDescription("")
            .setQuestionText(LocalizedStrings.of())
            .setQuestionHelpText(LocalizedStrings.empty())
            .setQuestionOptions(options)
            .build();

    MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) definition;

    assertThat(multiOption.getOptionsForLocale(Locale.US))
        .containsExactly(
            LocalizedQuestionOption.create(1L, 1L, "ay", "one", Locale.US),
            LocalizedQuestionOption.create(2L, 2L, "bee", "two", Locale.US));
  }

  @Test
  public void getOptionsForLocaleOrDefault_returnsBothLocalizedAndDefault() throws Exception {
    ImmutableList<QuestionOption> options =
        ImmutableList.of(
            QuestionOption.create(
                1L, "ay", LocalizedStrings.of(Locale.US, "one", Locale.GERMAN, "eins")),
            QuestionOption.create(
                2L, "bee", LocalizedStrings.of(Locale.US, "two", Locale.GERMAN, "zwei")),
            QuestionOption.create(3L, "see", LocalizedStrings.of(Locale.US, "three")));
    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.DROPDOWN)
            .setName("")
            .setDescription("")
            .setQuestionText(LocalizedStrings.of())
            .setQuestionHelpText(LocalizedStrings.empty())
            .setQuestionOptions(options)
            .build();

    MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) definition;

    assertThat(multiOption.getOptionsForLocaleOrDefault(Locale.GERMAN))
        .containsExactly(
            LocalizedQuestionOption.create(1L, 1L, "ay", "eins", Locale.GERMAN),
            LocalizedQuestionOption.create(2L, 2L, "bee", "zwei", Locale.GERMAN),
            LocalizedQuestionOption.create(3L, 3L, "see", "three", Locale.US));
  }

  @Test
  public void getOptionAdminNames_returnsAdminNames() throws UnsupportedQuestionTypeException {
    ImmutableList<QuestionOption> options =
        ImmutableList.of(
            QuestionOption.create(1L, "opt1", LocalizedStrings.of(Locale.US, "option 1")),
            QuestionOption.create(2L, "opt2", LocalizedStrings.of(Locale.US, "option 2")));

    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.DROPDOWN)
            .setName("")
            .setDescription("")
            .setQuestionText(LocalizedStrings.of())
            .setQuestionHelpText(LocalizedStrings.empty())
            .setQuestionOptions(options)
            .build();

    MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) definition;

    assertThat(multiOption.getOptionAdminNames()).containsExactly("opt1", "opt2");
  }
}
