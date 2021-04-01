package forms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import org.junit.Test;
import services.Path;
import services.question.AddressQuestionDefinition;
import services.question.QuestionDefinition;
import services.question.QuestionDefinitionBuilder;

public class AddressQuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    AddressQuestionForm form = new AddressQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionParentPath("my.question.path");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    QuestionDefinitionBuilder builder = form.getBuilder();

    // The QuestionForm does not set version, which is needed in order to build the
    // QuestionDefinition. How we get this value hasn't been determined.
    builder.setVersion(1L);

    AddressQuestionDefinition expected =
        new AddressQuestionDefinition(
            1L,
            "name",
            Path.create("my.question.path.name"),
            "description",
            ImmutableMap.of(Locale.US, "What is the question text?"),
            ImmutableMap.of(),
            AddressQuestionDefinition.AddressValidationPredicates.create());

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
            "description",
            ImmutableMap.of(Locale.US, "What is the question text?"),
            ImmutableMap.of(),
            AddressQuestionDefinition.AddressValidationPredicates.create());

    AddressQuestionForm form = new AddressQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder();

    // The QuestionForm does not set version, which is needed in order to build the
    // QuestionDefinition. How we get this value hasn't been determined.
    builder.setVersion(1L);

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(originalQd);
  }
}
