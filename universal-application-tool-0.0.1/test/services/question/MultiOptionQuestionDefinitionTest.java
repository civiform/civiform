package services.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import models.LifecycleStage;
import org.junit.Test;
import services.Path;

public class MultiOptionQuestionDefinitionTest {

  @Test
  public void buildMultiSelectQuestion() throws UnsupportedQuestionTypeException {
    ImmutableListMultimap<Locale, String> options =
        ImmutableListMultimap.of(Locale.US, "option 1", Locale.US, "option 2");

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
            .setQuestionOptions(ImmutableListMultimap.of())
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
    ImmutableListMultimap<Locale, String> options =
        ImmutableListMultimap.of(
            Locale.US, "one", Locale.US, "two", Locale.GERMAN, "eins", Locale.GERMAN, "zwei");
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

    assertThat(multiOption.getOptionsForLocale(Locale.US)).containsExactly("one", "two");
  }
}
