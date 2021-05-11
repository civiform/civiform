package forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import services.LocalizedStrings;
import services.Path;
import services.question.types.NameQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;

public class NameQuestionFormTest {
  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    Path path = Path.create("my.question.path.name");

    NameQuestionForm form = new NameQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    QuestionDefinitionBuilder builder = form.getBuilder(path);

    NameQuestionDefinition expected =
        new NameQuestionDefinition(
            "name",
            path,
            Optional.empty(),
            "description",
            LocalizedStrings.of(Locale.US, "What is the question text?"),
            LocalizedStrings.empty());

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    Path path = Path.create("my.question.path.name");

    NameQuestionDefinition originalQd =
        new NameQuestionDefinition(
            "name",
            path,
            Optional.empty(),
            "description",
            LocalizedStrings.of(Locale.US, "What is the question text?"),
            LocalizedStrings.empty());

    NameQuestionForm form = new NameQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder(path);

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(originalQd);
  }
}
