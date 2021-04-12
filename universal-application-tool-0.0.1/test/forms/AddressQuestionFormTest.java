package forms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import models.LifecycleStage;
import org.junit.Test;
import services.Path;
import services.question.types.AddressQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;

public class AddressQuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    AddressQuestionForm form = new AddressQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionParentPath("my.question.path");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setDisallowPoBox(true);
    QuestionDefinitionBuilder builder = form.getBuilder();

    builder.setVersion(1L);
    builder.setLifecycleStage(LifecycleStage.ACTIVE);

    AddressQuestionDefinition expected =
        new AddressQuestionDefinition(
            1L,
            "name",
            Path.create("my.question.path.name"),
            Optional.empty(),
            "description",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "What is the question text?"),
            ImmutableMap.of(),
            AddressQuestionDefinition.AddressValidationPredicates.create(true));

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    AddressQuestionDefinition originalQd =
        new AddressQuestionDefinition(
            1L,
            "name",
            Path.create("my.question.path.name"),
            Optional.empty(),
            "description",
            LifecycleStage.ACTIVE,
            ImmutableMap.of(Locale.US, "What is the question text?"),
            ImmutableMap.of(),
            AddressQuestionDefinition.AddressValidationPredicates.create());

    AddressQuestionForm form = new AddressQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder();

    builder.setVersion(1L);
    builder.setLifecycleStage(LifecycleStage.ACTIVE);

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(originalQd);
  }
}
