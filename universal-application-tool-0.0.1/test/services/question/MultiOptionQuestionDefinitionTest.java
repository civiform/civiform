package services.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import org.junit.Test;
import services.Path;

public class MultiOptionQuestionDefinitionTest {

  @Test
  public void buildMultiSelectQuestion() throws UnsupportedQuestionTypeException {
    ImmutableListMultimap<Locale, String> options =
        ImmutableListMultimap.of(Locale.US, "option 1", Locale.US, "option 2");

    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.MULTI_OPTION)
            .setName("")
            .setDescription("")
            .setPath(Path.empty())
            .setQuestionText(ImmutableMap.of())
            .setQuestionHelpText(ImmutableMap.of())
            .getMultiOptionUiType(MultiOptionQuestionDefinition.MultiOptionUiType.DROPDOWN)
            .setQuestionOptions(options)
            .build();

    MultiOptionQuestionDefinition multiOption = (MultiOptionQuestionDefinition) definition;

    assertThat(multiOption.getMultiOptionUiType())
        .isEqualTo(MultiOptionQuestionDefinition.MultiOptionUiType.DROPDOWN);
    assertThat(multiOption.getOptions()).isEqualTo(options);
  }

  @Test
  public void getOptionsForLocale_failsForMissingLocale() {
    MultiOptionQuestionDefinition definition =
        new MultiOptionQuestionDefinition(
            1L,
            "",
            Path.empty(),
            "",
            ImmutableMap.of(),
            ImmutableMap.of(),
            MultiOptionQuestionDefinition.MultiOptionUiType.DROPDOWN,
            ImmutableListMultimap.of());

    Throwable thrown = catchThrowable(() -> definition.getOptionsForLocale(Locale.CANADA));

    assertThat(thrown).isInstanceOf(TranslationNotFoundException.class);
    assertThat(thrown).hasMessageContaining("ca");
  }

  @Test
  public void getOptionsForLocale_returnsAllTranslations() throws TranslationNotFoundException {
    ImmutableListMultimap<Locale, String> options =
        ImmutableListMultimap.of(
            Locale.US, "one", Locale.US, "two", Locale.GERMAN, "eins", Locale.GERMAN, "zwei");
    MultiOptionQuestionDefinition definition =
        new MultiOptionQuestionDefinition(
            1L,
            "",
            Path.empty(),
            "",
            ImmutableMap.of(),
            ImmutableMap.of(),
            MultiOptionQuestionDefinition.MultiOptionUiType.DROPDOWN,
            options);

    assertThat(definition.getOptionsForLocale(Locale.US)).containsExactly("one", "two");
  }
}
