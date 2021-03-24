package forms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import org.junit.Test;
import services.Path;
import services.question.QuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.TextQuestionDefinition;

public class TextQuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    TextQuestionForm form = new TextQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionParentPath("my.question.path");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setTextMinLength(4);
    form.setTextMaxLength(6);
    QuestionDefinitionBuilder builder = form.getBuilder();

    // The QuestionForm does not set version, which is needed in order to build the
    // QuestionDefinition. How we get this value hasn't been determined.
    builder.setVersion(1L);

    TextQuestionDefinition expected =
        new TextQuestionDefinition(
            1L,
            "name",
            Path.create("my.question.path.name"),
            "description",
            ImmutableMap.of(Locale.US, "What is the question text?"),
            ImmutableMap.of(),
            TextQuestionDefinition.TextValidationPredicates.create(4, 6));

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    TextQuestionDefinition originalQd =
        new TextQuestionDefinition(
            1L,
            "name",
            Path.create("my.question.path.name"),
            "description",
            ImmutableMap.of(Locale.US, "What is the question text?"),
            ImmutableMap.of(),
            TextQuestionDefinition.TextValidationPredicates.create(4, 6));

    TextQuestionForm form = new TextQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder();

    // The QuestionForm does not set version, which is needed in order to build the
    // QuestionDefinition. How we get this value hasn't been determined.
    builder.setVersion(1L);

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(originalQd);
  }
}
