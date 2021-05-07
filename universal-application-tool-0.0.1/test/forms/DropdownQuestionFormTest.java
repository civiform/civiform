package forms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import services.Path;
import services.question.QuestionOption;
import services.question.exceptions.UnsupportedQuestionTypeException;
import services.question.types.DropdownQuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;

public class DropdownQuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws UnsupportedQuestionTypeException {
    Path path = Path.create("my.question.path.name");

    DropdownQuestionForm form = new DropdownQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("help text");
    // Unique field
    form.setOptions(ImmutableList.of("cat", "dog", "rabbit"));
    QuestionDefinitionBuilder builder = form.getBuilder(path);

    DropdownQuestionDefinition expected =
        new DropdownQuestionDefinition(
            "name",
            path,
            Optional.empty(),
            "description",
            ImmutableMap.of(Locale.US, "What is the question text?"),
            ImmutableMap.of(Locale.US, "help text"),
            ImmutableList.of(
                QuestionOption.builder()
                    .setId(1L)
                    .setOptionText(ImmutableMap.of(Locale.US, "cat"))
                    .build(),
                QuestionOption.builder()
                    .setId(2L)
                    .setOptionText(ImmutableMap.of(Locale.US, "dog"))
                    .build(),
                QuestionOption.builder()
                    .setId(3L)
                    .setOptionText(ImmutableMap.of(Locale.US, "rabbit"))
                    .build()));

    assertThat(builder.build()).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    Path path = Path.create("my.question.path.name");

    DropdownQuestionDefinition originalQd =
        new DropdownQuestionDefinition(
            "name",
            path,
            Optional.empty(),
            "description",
            ImmutableMap.of(Locale.US, "What is the question text?"),
            ImmutableMap.of(Locale.US, "help text"),
            ImmutableList.of(
                QuestionOption.builder()
                    .setId(1L)
                    .setOptionText(ImmutableMap.of(Locale.US, "hello"))
                    .build(),
                QuestionOption.builder()
                    .setId(1L)
                    .setOptionText(ImmutableMap.of(Locale.US, "world"))
                    .build()));

    DropdownQuestionForm form = new DropdownQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder(path);

    assertThat(builder.build()).isEqualTo(originalQd);
  }
}
