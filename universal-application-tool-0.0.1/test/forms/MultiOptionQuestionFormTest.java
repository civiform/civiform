package forms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import models.LifecycleStage;
import org.junit.Test;
import services.Path;
import services.question.QuestionOption;
import services.question.types.CheckboxQuestionDefinition;
import services.question.types.MultiOptionQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;

public class MultiOptionQuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    MultiOptionQuestionForm form = new CheckboxQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionParentPath("my.question.path");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setMinChoicesRequired("1");
    form.setMaxChoicesAllowed("10");
    QuestionDefinitionBuilder builder = form.getBuilder();

    builder.setVersion(1L);
    builder.setLifecycleStage(LifecycleStage.ACTIVE);

    CheckboxQuestionDefinition expected =
        new CheckboxQuestionDefinition(
            1L,
            "name",
            Path.create("my.question.path.name"),
            Optional.empty(),
            "description",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "What is the question text?"),
            ImmutableMap.of(),
            ImmutableList.of(QuestionOption.create(1L, ImmutableMap.of(Locale.US, "option one"))),
            MultiOptionQuestionDefinition.MultiOptionValidationPredicates.create(1, 10));

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    CheckboxQuestionDefinition originalQd =
        new CheckboxQuestionDefinition(
            1L,
            "name",
            Path.create("my.question.path.name"),
            Optional.empty(),
            "description",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "What is the question text?"),
            ImmutableMap.of(),
            ImmutableList.of(QuestionOption.create(1L, ImmutableMap.of(Locale.US, "option 1"))),
            MultiOptionQuestionDefinition.MultiOptionValidationPredicates.create(1, 10));

    MultiOptionQuestionForm form = new CheckboxQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder();

    builder.setVersion(1L);
    builder.setLifecycleStage(LifecycleStage.ACTIVE);

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(originalQd);
  }

  @Test
  public void getBuilder_emptyStringMinMax_noPredicateSet() throws Exception {
    MultiOptionQuestionForm form = new CheckboxQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionParentPath("my.question.path");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setMinChoicesRequired("");
    form.setMaxChoicesAllowed("");
    QuestionDefinitionBuilder builder = form.getBuilder();

    builder.setVersion(1L);
    builder.setLifecycleStage(LifecycleStage.ACTIVE);

    CheckboxQuestionDefinition expected =
        new CheckboxQuestionDefinition(
            1L,
            "name",
            Path.create("my.question.path.name"),
            Optional.empty(),
            "description",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "What is the question text?"),
            ImmutableMap.of(),
            ImmutableList.of(QuestionOption.create(1L, ImmutableMap.of(Locale.US, "option one"))),
            MultiOptionQuestionDefinition.MultiOptionValidationPredicates.create());

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }
}
