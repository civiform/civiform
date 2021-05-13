package forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;

public class EnumeratorQuestionFormTest {
  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    EnumeratorQuestionForm form = new EnumeratorQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    QuestionDefinitionBuilder builder = form.getBuilder();

    EnumeratorQuestionDefinition expected =
        new EnumeratorQuestionDefinition(
            "name",
            Optional.empty(),
            "description",
            LocalizedStrings.of(Locale.US, "What is the question text?"),
            LocalizedStrings.empty(),
            LocalizedStrings.empty());

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    EnumeratorQuestionDefinition originalQd =
        new EnumeratorQuestionDefinition(
            "name",
            Optional.empty(),
            "description",
            LocalizedStrings.of(Locale.US, "What is the question text?"),
            LocalizedStrings.empty(),
            LocalizedStrings.empty());

    EnumeratorQuestionForm form = new EnumeratorQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder();

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(originalQd);
  }
}
