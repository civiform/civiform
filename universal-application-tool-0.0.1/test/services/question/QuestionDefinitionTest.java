package services.question;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;

public class QuestionDefinitionTest {

  @Test
  public void newQuestionHasCorrectFields() {
    QuestionDefinition question =
        new QuestionDefinition(
            "id",
            "version",
            "name",
            "my.path",
            "description",
            ImmutableMap.of(Locale.ENGLISH, "question?"),
            ImmutableMap.of(Locale.ENGLISH, "help text"));

    assertThat(question.getId()).isEqualTo("id");
    assertThat(question.getVersion()).isEqualTo("version");
    assertThat(question.getName()).isEqualTo("name");
    assertThat(question.getPath()).isEqualTo("my.path");
    assertThat(question.getDescription()).isEqualTo("description");
    assertThat(question.getQuestionText(Locale.ENGLISH)).isEqualTo("question?");
    assertThat(question.getQuestionHelpText(Locale.ENGLISH)).isEqualTo("help text");
  }

  @Test
  public void getQuestionTextForUnknownLocale_throwsException() {
    QuestionDefinition question =
        new QuestionDefinition("", "", "", "", "", ImmutableMap.of(), ImmutableMap.of());

    Throwable thrown = catchThrowable(() -> question.getQuestionText(Locale.FRANCE));

    assertThat(thrown).isInstanceOf(RuntimeException.class);
    assertThat(thrown).hasMessage("Locale not found: fr_FR");
  }

  @Test
  public void getQuestionHelpTextForUnknownLocale_throwsException() {
    QuestionDefinition question =
        new QuestionDefinition("", "", "", "", "", ImmutableMap.of(), ImmutableMap.of());

    Throwable thrown = catchThrowable(() -> question.getQuestionHelpText(Locale.FRANCE));

    assertThat(thrown).isInstanceOf(RuntimeException.class);
    assertThat(thrown).hasMessage("Locale not found: fr_FR");
  }

  @Test
  public void newQuestionHasTypeText() {
    QuestionDefinition question =
        new QuestionDefinition("", "", "", "", "", ImmutableMap.of(), ImmutableMap.of());

    assertThat(question.getQuestionType()).isEqualTo(QuestionType.TEXT);
  }

  @Test
  public void newQuestionHasTextScalar() {
    QuestionDefinition question =
        new QuestionDefinition("", "", "", "", "", ImmutableMap.of(), ImmutableMap.of());

    assertThat(question.getScalars()).containsOnly(entry("text", String.class));
  }
}
