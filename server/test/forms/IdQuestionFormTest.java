package forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.types.IdQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;

public class IdQuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    IdQuestionForm form = new IdQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setMinLength("4");
    form.setMaxLength("6");
    QuestionDefinitionBuilder builder = form.getBuilder();

    IdQuestionDefinition expected =
        new IdQuestionDefinition(
            "name",
            Optional.empty(),
            "description",
            LocalizedStrings.of(Locale.US, "What is the question text?"),
            LocalizedStrings.empty(),
            IdQuestionDefinition.IdValidationPredicates.create(4, 6));

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    IdQuestionDefinition originalQd =
        new IdQuestionDefinition(
            "name",
            Optional.empty(),
            "description",
            LocalizedStrings.of(Locale.US, "What is the question text?"),
            LocalizedStrings.empty(),
            IdQuestionDefinition.IdValidationPredicates.create(4, 6));

    IdQuestionForm form = new IdQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder();

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(originalQd);
  }

  @Test
  public void getBuilder_emptyStringMinMax_noPredicateSet() throws Exception {
    IdQuestionForm form = new IdQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setMinLength("");
    form.setMaxLength("");
    QuestionDefinitionBuilder builder = form.getBuilder();

    IdQuestionDefinition expected =
        new IdQuestionDefinition(
            "name",
            Optional.empty(),
            "description",
            LocalizedStrings.of(Locale.US, "What is the question text?"),
            LocalizedStrings.empty(),
            IdQuestionDefinition.IdValidationPredicates.create());

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }
}
