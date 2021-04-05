package forms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import models.LifecycleStage;
import org.junit.Test;
import services.Path;
import services.question.QuestionDefinitionBuilder;
import services.question.RadioButtonQuestionDefinition;
import services.question.UnsupportedQuestionTypeException;

public class RadioButtonQuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws UnsupportedQuestionTypeException {
    RadioButtonQuestionForm form = new RadioButtonQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionParentPath("my.question.path");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    // Unique field
    form.setOptions(ImmutableList.of("cat", "dog", "rabbit"));
    QuestionDefinitionBuilder builder = form.getBuilder();

    // The QuestionForm does not set version, which is needed in order to build the
    // QuestionDefinition. How we get this value hasn't been determined.
    builder.setVersion(1L);
    builder.setLifecycleStage(LifecycleStage.ACTIVE);

    RadioButtonQuestionDefinition expected =
        new RadioButtonQuestionDefinition(
            1L,
            "name",
            Path.create("my.question.path.name"),
            "description",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "What is the question text?"),
            ImmutableMap.of(),
            ImmutableListMultimap.of(Locale.US, "cat", Locale.US, "dog", Locale.US, "rabbit"));

    assertThat(builder.build()).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    RadioButtonQuestionDefinition originalQd =
        new RadioButtonQuestionDefinition(
            1L,
            "name",
            Path.create("my.question.path.name"),
            "description",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "What is the question text?"),
            ImmutableMap.of(),
            ImmutableListMultimap.of(Locale.US, "hello", Locale.US, "world"));

    RadioButtonQuestionForm form = new RadioButtonQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder();

    // The QuestionForm does not set version, which is needed in order to build the
    // QuestionDefinition. How we get this value hasn't been determined.
    builder.setVersion(1L);
    builder.setLifecycleStage(LifecycleStage.ACTIVE);

    assertThat(builder.build()).isEqualTo(originalQd);
  }
}
