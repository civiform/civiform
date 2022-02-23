package forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.types.QuestionDefinition;
import services.question.types.StaticContentQuestionDefinition;

public class StaticContentQuestionFormTest {
  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    StaticContentQuestionForm form = new StaticContentQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("Some text. Not an actual question.");
    form.setQuestionHelpText("");
    QuestionDefinition actual = form.getBuilder().build();

    StaticContentQuestionDefinition expected =
        new StaticContentQuestionDefinition(
            "name",
            Optional.empty(),
            "description",
            LocalizedStrings.of(Locale.US, "Some text. Not an actual question."),
            LocalizedStrings.empty());

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    StaticContentQuestionDefinition originalQd =
        new StaticContentQuestionDefinition(
            "name",
            Optional.empty(),
            "description",
            LocalizedStrings.of(Locale.US, "Some text. Not an actual question."),
            LocalizedStrings.empty());

    StaticContentQuestionForm form = new StaticContentQuestionForm(originalQd);
    QuestionDefinition actual = form.getBuilder().build();

    assertThat(actual).isEqualTo(originalQd);
  }
}
