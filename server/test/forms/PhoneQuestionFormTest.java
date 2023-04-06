package forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.types.PhoneQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;

public class PhoneQuestionFormTest {
  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    PhoneQuestionForm form = new PhoneQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is your phone number?");
    form.setQuestionHelpText("");
    QuestionDefinitionBuilder builder = form.getBuilder();

    PhoneQuestionDefinition expected =
        new PhoneQuestionDefinition(
            "name",
            Optional.empty(),
            "description",
            LocalizedStrings.of(Locale.US, "What is your phone number?"),
            LocalizedStrings.empty());

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    PhoneQuestionDefinition originalQd =
        new PhoneQuestionDefinition(
            "name",
            Optional.empty(),
            "description",
            LocalizedStrings.of(Locale.US, "What is your Phone Number?"),
            LocalizedStrings.empty());

    PhoneQuestionForm form = new PhoneQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder();

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(originalQd);
  }
}
