package mapping.admin.questions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
import views.admin.questions.QuestionTranslationPageViewModel;

public final class QuestionTranslationPageMapperTest {

  private QuestionTranslationPageMapper mapper;

  @Before
  public void setup() {
    mapper = new QuestionTranslationPageMapper();
  }

  private QuestionDefinition buildMockQuestion() {
    QuestionDefinition question = mock(QuestionDefinition.class);
    when(question.getName()).thenReturn("test-question");
    when(question.getId()).thenReturn(1L);
    when(question.getQuestionText())
        .thenReturn(LocalizedStrings.of(Locale.US, "What is your name?"));
    when(question.getQuestionHelpText()).thenReturn(LocalizedStrings.empty());
    when(question.getQuestionType()).thenReturn(QuestionType.TEXT);
    when(question.getConcurrencyToken()).thenReturn(Optional.empty());
    return question;
  }

  @Test
  public void map_setsQuestionName() {
    QuestionDefinition question = buildMockQuestion();
    Locale locale = Locale.FRENCH;

    QuestionTranslationPageViewModel result =
        mapper.map(question, locale, ImmutableList.of(locale), Optional.empty());

    assertThat(result.getQuestionName()).isEqualTo("test-question");
  }

  @Test
  public void map_setsCurrentLocaleDisplayName() {
    QuestionDefinition question = buildMockQuestion();
    Locale locale = Locale.FRENCH;

    QuestionTranslationPageViewModel result =
        mapper.map(question, locale, ImmutableList.of(locale), Optional.empty());

    assertThat(result.getCurrentLocaleDisplayName()).isEqualTo("French");
  }

  @Test
  public void map_setsFormActionUrl() {
    QuestionDefinition question = buildMockQuestion();
    Locale locale = Locale.FRENCH;

    QuestionTranslationPageViewModel result =
        mapper.map(question, locale, ImmutableList.of(locale), Optional.empty());

    assertThat(result.getFormActionUrl()).isNotEmpty();
  }

  @Test
  public void map_setsQuestionTextField() {
    QuestionDefinition question = buildMockQuestion();
    Locale locale = Locale.FRENCH;

    QuestionTranslationPageViewModel result =
        mapper.map(question, locale, ImmutableList.of(locale), Optional.empty());

    assertThat(result.getQuestionTextField().getFieldName()).isEqualTo("questionText");
    assertThat(result.getQuestionTextField().getDefaultText()).isEqualTo("What is your name?");
  }

  @Test
  public void map_withEmptyHelpText_setsEmptyHelpTextField() {
    QuestionDefinition question = buildMockQuestion();
    Locale locale = Locale.FRENCH;

    QuestionTranslationPageViewModel result =
        mapper.map(question, locale, ImmutableList.of(locale), Optional.empty());

    assertThat(result.getHelpTextField()).isEmpty();
  }

  @Test
  public void map_withHelpText_setsHelpTextField() {
    QuestionDefinition question = buildMockQuestion();
    when(question.getQuestionHelpText())
        .thenReturn(LocalizedStrings.of(Locale.US, "Enter your full name"));
    Locale locale = Locale.FRENCH;

    QuestionTranslationPageViewModel result =
        mapper.map(question, locale, ImmutableList.of(locale), Optional.empty());

    assertThat(result.getHelpTextField()).isPresent();
    assertThat(result.getHelpTextField().get().getDefaultText()).isEqualTo("Enter your full name");
  }

  @Test
  public void map_setsLocaleLinks() {
    QuestionDefinition question = buildMockQuestion();
    Locale french = Locale.FRENCH;
    Locale spanish = new Locale("es");

    QuestionTranslationPageViewModel result =
        mapper.map(question, french, ImmutableList.of(french, spanish), Optional.empty());

    assertThat(result.getLocaleLinks()).hasSize(2);
  }

  @Test
  public void map_forTextQuestion_setsEmptyTypeSpecificFields() {
    QuestionDefinition question = buildMockQuestion();
    Locale locale = Locale.FRENCH;

    QuestionTranslationPageViewModel result =
        mapper.map(question, locale, ImmutableList.of(locale), Optional.empty());

    assertThat(result.getTypeSpecificFields()).isEmpty();
    assertThat(result.getQuestionType()).isEmpty();
  }

  @Test
  public void map_setsErrorMessage() {
    QuestionDefinition question = buildMockQuestion();
    Locale locale = Locale.FRENCH;

    QuestionTranslationPageViewModel result =
        mapper.map(question, locale, ImmutableList.of(locale), Optional.of("An error occurred"));

    assertThat(result.getErrorMessage()).contains("An error occurred");
  }
}
