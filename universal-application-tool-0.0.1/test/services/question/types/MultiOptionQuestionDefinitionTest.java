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
  public void getOptionsForLocale_failsForMissingLocale() throws UnsupportedQuestionTypeException {
    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.DROPDOWN)
            .setName("")
            .setDescription("")
            .setPath(Path.empty())
            .setQuestionText(ImmutableMap.of())
            .setQuestionHelpText(ImmutableMap.of())
            .setQuestionOptions(ImmutableList.of())
            .setLifecycleStage(LifecycleStage.ACTIVE)
            .build();

    MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) definition;
    Throwable thrown = catchThrowable(() -> multiOption.getOptionsForLocale(Locale.CANADA));

    assertThat(thrown).isInstanceOf(TranslationNotFoundException.class);
    assertThat(thrown).hasMessageContaining("ca");
  }

  @Test
  public void getOptionsForLocale_returnsAllTranslations()
      throws TranslationNotFoundException, UnsupportedQuestionTypeException {
    ImmutableList<QuestionOption> options =
        ImmutableList.of(
            QuestionOption.create(1L, ImmutableMap.of(Locale.US, "one")),
            QuestionOption.create(2L, ImmutableMap.of(Locale.US, "two")),
            QuestionOption.create(3L, ImmutableMap.of(Locale.GERMAN, "eins")),
            QuestionOption.create(4L, ImmutableMap.of(Locale.GERMAN, "zwei")));
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
