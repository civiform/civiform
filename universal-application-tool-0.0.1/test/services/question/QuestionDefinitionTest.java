package services.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.entry;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import org.junit.Test;

public class QuestionDefinitionTest {

  @Test
  public void newQuestionHasCorrectFields() {
    QuestionDefinition question =
        new QuestionDefinition(
            "id",
            "version",
            "my name",
            "my.path.name",
            "description",
            ImmutableMap.of(Locale.ENGLISH, "question?"),
            Optional.of(ImmutableMap.of(Locale.ENGLISH, "help text")));

    assertThat(question.getId()).isEqualTo("id");
    assertThat(question.getVersion()).isEqualTo("version");
    assertThat(question.getName()).isEqualTo("my name");
    assertThat(question.getPath()).isEqualTo("my.path.name");
    assertThat(question.getDescription()).isEqualTo("description");
    assertThat(question.getQuestionText(Locale.ENGLISH)).isEqualTo("question?");
    assertThat(question.getQuestionHelpText(Locale.ENGLISH)).isEqualTo("help text");
  }

  @Test
  public void getQuestionTextForUnknownLocale_throwsException() {
    QuestionDefinition question =
        new QuestionDefinition("", "", "", "", "", ImmutableMap.of(), Optional.empty());

    Throwable thrown = catchThrowable(() -> question.getQuestionText(Locale.FRANCE));

    assertThat(thrown).isInstanceOf(RuntimeException.class);
    assertThat(thrown).hasMessage("Locale not found: fr_FR");
  }

  @Test
  public void getQuestionHelpTextForUnknownLocale_throwsException() {
    QuestionDefinition question =
        new QuestionDefinition(
            "",
            "",
            "",
            "",
            "",
            ImmutableMap.of(),
            Optional.of(ImmutableMap.of(Locale.ENGLISH, "help text")));

    Throwable thrown = catchThrowable(() -> question.getQuestionHelpText(Locale.FRANCE));

    assertThat(thrown).isInstanceOf(RuntimeException.class);
    assertThat(thrown).hasMessage("Locale not found: fr_FR");
  }

  @Test
  public void getEmptyHelpTextForUnknownLocale_succeeds() {
    QuestionDefinition question =
        new QuestionDefinition("", "", "", "", "", ImmutableMap.of(), Optional.empty());
    assertThat(question.getQuestionHelpText(Locale.FRANCE)).isEqualTo("");
  }

  @Test
  public void newQuestionHasTypeText() {
    QuestionDefinition question =
        new QuestionDefinition("", "", "", "", "", ImmutableMap.of(), Optional.empty());

    assertThat(question.getQuestionType()).isEqualTo(QuestionType.TEXT);
  }

  @Test
  public void newQuestionHasStringScalar() {
    QuestionDefinition question =
        new QuestionDefinition("", "", "", "", "", ImmutableMap.of(), Optional.empty());
    assertThat(question.getScalars()).containsOnly(entry("text", ScalarType.STRING));
    assertThat(question.getScalarType("text").get()).isEqualTo(ScalarType.STRING);
    assertThat(question.getScalarType("text").get().getClassFor().get()).isEqualTo(String.class);
  }

  @Test
  public void newQuestionMissingScalar_returnsOptionalEmpty() {
    QuestionDefinition question =
        new QuestionDefinition("", "", "", "", "", ImmutableMap.of(), Optional.empty());
    assertThat(question.getScalarType("notPresent")).isEqualTo(Optional.empty());
  }
}
