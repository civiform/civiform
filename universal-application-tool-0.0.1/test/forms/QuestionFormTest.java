package forms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
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
    QuestionForm form = new QuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionParentPath("my.question.path");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setQuestionType(QuestionType.TEXT);
    QuestionDefinitionBuilder builder = form.getBuilder();

    // The QuestionForm does not set version or lifecycle stage.
    // A first question is a draft in version 1 - set those here.
    builder.setVersion(1L);
    builder.setLifecycleStage(LifecycleStage.DRAFT);

    TextQuestionDefinition expected =
        new TextQuestionDefinition(
            1L,
            "name",
            Path.create("my.question.path.name"),
            Optional.empty(),
            "description",
            LifecycleStage.DRAFT,
            ImmutableMap.of(Locale.US, "What is the question text?"),
            ImmutableMap.of());
    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getQuestionPath_repeaterQuestion_hasSquareBrackets() {
    QuestionForm form = new QuestionForm();
    form.setQuestionParentPath("the.first.part");
    form.setQuestionName("the other part");
    form.setQuestionType(QuestionType.REPEATER);

    assertThat(form.getQuestionPath()).isEqualTo(Path.create("the.first.part.the_other_part[]"));
  }

  @Parameters({
    "the other part|the_other_part",
    "LOWER cAsE|lower_case",
    "ig--.][;n-or.e sy$mb#o*ls|ignore_symbols"
  })
  @Test
  public void getQuestionPath_formatsQuestionName(String name, String path) {
    Path base = Path.create("the.first.part");
    QuestionForm form = new QuestionForm();
    form.setQuestionParentPath(base.toString());
    form.setQuestionName(name);

    assertThat(form.getQuestionPath()).isEqualTo(base.join(path));
  }
}
