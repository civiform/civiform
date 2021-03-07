package forms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import org.junit.Test;
import services.question.QuestionDefinition;
import services.question.QuestionDefinitionBuilder;
import services.question.TextQuestionDefinition;

public class QuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    QuestionForm form = new QuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionPath("my.question.path");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setQuestionType("TEXT");
    QuestionDefinitionBuilder builder = form.getBuilder();

    // The QuestionForm does not set version, which is needed in order to build the
    // QuestionDefinition. How we get this value hasn't been determined.
    builder.setVersion(1L);

    TextQuestionDefinition expected =
        new TextQuestionDefinition(
            1L,
            "name",
            "my.question.path",
            "description",
            ImmutableMap.of(Locale.ENGLISH, "What is the question text?"),
            ImmutableMap.of());
    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }
}
