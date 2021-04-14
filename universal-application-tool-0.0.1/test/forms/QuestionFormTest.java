package forms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import models.LifecycleStage;
import org.junit.Test;
import org.junit.runner.RunWith;
import services.Path;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;
import services.question.types.TextQuestionDefinition;

@RunWith(JUnitParamsRunner.class)
public class QuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    Path path = Path.create("my.question.path.name");

    QuestionForm form = new QuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setQuestionType(QuestionType.TEXT);
    QuestionDefinitionBuilder builder = form.getBuilder(path);

    // The QuestionForm does not set version or lifecycle stage.
    // A first question is a draft in version 1 - set those here.
    builder.setVersion(1L);
    builder.setLifecycleStage(LifecycleStage.DRAFT);

    TextQuestionDefinition expected =
        new TextQuestionDefinition(
            1L,
            "name",
            path,
            Optional.empty(),
            "description",
            LifecycleStage.DRAFT,
            ImmutableMap.of(Locale.US, "What is the question text?"),
            ImmutableMap.of());
    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }
}
