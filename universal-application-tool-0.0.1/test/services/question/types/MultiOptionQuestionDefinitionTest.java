package services.question.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import models.LifecycleStage;
import org.junit.Test;
import services.Path;
import services.question.LocalizedQuestionOption;
import services.question.QuestionOption;
import services.question.exceptions.TranslationNotFoundException;
import services.question.exceptions.UnsupportedQuestionTypeException;

public class MultiOptionQuestionDefinitionTest {

  @Test
  public void buildMultiSelectQuestion() throws UnsupportedQuestionTypeException {
    ImmutableList<QuestionOption> options =
        ImmutableList.of(
            QuestionOption.create(1L, ImmutableMap.of(Locale.US, "option 1")),
            QuestionOption.create(2L, ImmutableMap.of(Locale.US, "option 2")));

    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.DROPDOWN)
            .setName("")
            .setDescription("")
            .setPath(Path.empty())
            .setQuestionText(ImmutableMap.of())
            .setQuestionHelpText(ImmutableMap.of())
            .setQuestionOptions(options)
            .setLifecycleStage(LifecycleStage.ACTIVE)
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
            .setPath(Path.empty())
            .setQuestionText(ImmutableMap.of(Locale.US, "test", Locale.FRANCE, "test"))
            .setQuestionHelpText(ImmutableMap.of(Locale.US, "test", Locale.FRANCE, "test"))
            .setQuestionOptions(
                ImmutableList.of(QuestionOption.create(1L, ImmutableMap.of(Locale.US, "option 1"))))
            .setLifecycleStage(LifecycleStage.ACTIVE)
            .build();

    assertThat(definition.getSupportedLocales()).containsExactly(Locale.US);
  }

  @Test
  public void getOptionsForLocale_failsForMissingLocale() throws UnsupportedQuestionTypeException {
    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.DROPDOWN)
            .setName("")
            .setDescription("")
            .setPath(Path.empty())
            .setQuestionText(ImmutableMap.of())
            .setQuestionHelpText(ImmutableMap.of())
            .setQuestionOptions(
                ImmutableList.of(QuestionOption.create(1L, ImmutableMap.of(Locale.US, "option 1"))))
            .setLifecycleStage(LifecycleStage.ACTIVE)
            .build();

    MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) definition;
    Throwable thrown = catchThrowable(() -> multiOption.getOptionsForLocale(Locale.CANADA_FRENCH));

    assertThat(thrown).isInstanceOf(TranslationNotFoundException.class);
    assertThat(thrown).hasMessageContaining("ca");
  }

  @Test
  public void getOptionsForLocale_returnsAllTranslations()
      throws TranslationNotFoundException, UnsupportedQuestionTypeException {
    ImmutableList<QuestionOption> options =
        ImmutableList.of(
            QuestionOption.create(1L, ImmutableMap.of(Locale.US, "one", Locale.GERMAN, "eins")),
            QuestionOption.create(2L, ImmutableMap.of(Locale.US, "two", Locale.GERMAN, "zwei")));
    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.DROPDOWN)
            .setName("")
            .setDescription("")
            .setPath(Path.empty())
            .setQuestionText(ImmutableMap.of())
            .setQuestionHelpText(ImmutableMap.of())
            .setQuestionOptions(options)
            .setLifecycleStage(LifecycleStage.ACTIVE)
            .build();

    MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) definition;

    assertThat(multiOption.getOptionsForLocale(Locale.US))
        .containsExactly(
            LocalizedQuestionOption.create(1L, "one", Locale.US),
            LocalizedQuestionOption.create(2L, "two", Locale.US));
  }
}
