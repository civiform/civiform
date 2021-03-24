package services.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import org.junit.Test;
import services.Path;

public class SingleSelectQuestionDefinitionTest {

  @Test
  public void buildSingleSelectQuestion() throws UnsupportedQuestionTypeException {
    ImmutableListMultimap<Locale, String> options =
        ImmutableListMultimap.of(Locale.US, "option 1", Locale.US, "option 2");

    QuestionDefinition definition =
        new QuestionDefinitionBuilder()
            .setQuestionType(QuestionType.SINGLE_SELECT)
            .setName("")
            .setDescription("")
            .setPath(Path.empty())
            .setQuestionText(ImmutableMap.of())
            .setQuestionHelpText(ImmutableMap.of())
            .setSingleSelectUiType(SingleSelectQuestionDefinition.SingleSelectUiType.DROPDOWN)
            .setSingleSelectOptions(options)
            .build();

    SingleSelectQuestionDefinition select = (SingleSelectQuestionDefinition) definition;

    assertThat(select.getSingleSelectUiType())
        .isEqualTo(SingleSelectQuestionDefinition.SingleSelectUiType.DROPDOWN);
    assertThat(select.getOptions()).isEqualTo(options);
  }

  @Test
  public void getOptionsForLocale_failsForMissingLocale() throws TranslationNotFoundException {
    SingleSelectQuestionDefinition definition =
        new SingleSelectQuestionDefinition(
            1L,
            "",
            Path.empty(),
            "",
            ImmutableMap.of(),
            ImmutableMap.of(),
            SingleSelectQuestionDefinition.SingleSelectUiType.DROPDOWN,
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
    SingleSelectQuestionDefinition definition =
        new SingleSelectQuestionDefinition(
            1L,
            "",
            Path.empty(),
            "",
            ImmutableMap.of(),
            ImmutableMap.of(),
            SingleSelectQuestionDefinition.SingleSelectUiType.DROPDOWN,
            options);

    assertThat(definition.getOptionsForLocale(Locale.US)).containsExactly("one", "two");
  }
}
