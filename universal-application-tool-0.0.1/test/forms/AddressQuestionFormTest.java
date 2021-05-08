package forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import services.LocalizedStrings;
import services.Path;
import services.question.types.AddressQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;

public class AddressQuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    Path path = Path.create("my.question.path.name");

    AddressQuestionForm form = new AddressQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setDisallowPoBox(true);
    QuestionDefinitionBuilder builder = form.getBuilder(path);

    AddressQuestionDefinition expected =
        new AddressQuestionDefinition(
            "name",
            path,
            Optional.empty(),
            "description",
            LocalizedStrings.of(Locale.US, "What is the question text?"),
            LocalizedStrings.of(),
            AddressQuestionDefinition.AddressValidationPredicates.create(true));

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    Path path = Path.create("my.question.path.name");

    AddressQuestionDefinition originalQd =
        new AddressQuestionDefinition(
            "name",
            path,
            Optional.empty(),
            "description",
            LocalizedStrings.of(Locale.US, "What is the question text?"),
            LocalizedStrings.of(),
            AddressQuestionDefinition.AddressValidationPredicates.create());

    AddressQuestionForm form = new AddressQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder(path);

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(originalQd);
  }
}
