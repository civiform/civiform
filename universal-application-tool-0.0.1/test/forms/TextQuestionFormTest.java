package forms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.OptionalLong;
import models.LifecycleStage;
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
    form.setMinLength(4);
    form.setMaxLength(6);
    QuestionDefinitionBuilder builder = form.getBuilder();

    builder.setVersion(1L);
    builder.setLifecycleStage(LifecycleStage.ACTIVE);

    TextQuestionDefinition expected =
        new TextQuestionDefinition(
            1L,
            "name",
            Path.create("my.question.path.name"),
            OptionalLong.empty(),
            "description",
            LifecycleStage.ACTIVE,
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
            OptionalLong.empty(),
            "description",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "What is the question text?"),
            ImmutableMap.of(),
            TextQuestionDefinition.TextValidationPredicates.create(4, 6));

    TextQuestionForm form = new TextQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder();

    builder.setVersion(1L);
    builder.setLifecycleStage(LifecycleStage.ACTIVE);

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(originalQd);
  }
}
