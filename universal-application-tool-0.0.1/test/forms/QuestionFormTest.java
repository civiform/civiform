package forms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import services.question.QuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.TextQuestionDefinition;
import services.question.UnsupportedQuestionTypeException;

public class QuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws UnsupportedQuestionTypeException {
    QuestionForm form = new QuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionPath("my.question.path");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setQuestionType("TEXT");
    QuestionDefinitionBuilder builder = form.getBuilder();

    // The QuestionForm does not set version or id, which are needed in order to build the
    // QuestionDefinition.
    builder.setId(1L);
    builder.setVersion(1L);

    TextQuestionDefinition expected =
        new TextQuestionDefinition(
            1L,
            1L,
            "name",
            "my.question.path",
            "description",
            ImmutableMap.of(Locale.ENGLISH, "What is the question text?"),
            Optional.empty());
    QuestionDefinition actual = builder.build();

    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getVersion()).isEqualTo(expected.getVersion());
    assertThat(actual.getName()).isEqualTo(expected.getName());
    assertThat(actual.getQuestionType()).isEqualTo(expected.getQuestionType());
    assertThat(actual.getPath()).isEqualTo(expected.getPath());
    assertThat(actual.getDescription()).isEqualTo(expected.getDescription());
    assertThat(actual.getQuestionText()).isEqualTo(expected.getQuestionText());
    assertThat(actual.getQuestionHelpText()).isEqualTo(expected.getQuestionHelpText());
  }
}
