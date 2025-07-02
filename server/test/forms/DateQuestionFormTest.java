package forms;

import static org.assertj.core.api.Assertions.assertThat;
import static services.question.types.DateQuestionDefinition.DateValidationOption.DateType.ANY;
import static services.question.types.DateQuestionDefinition.DateValidationOption.DateType.APPLICATION_DATE;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.types.DateQuestionDefinition;
import services.question.types.DateQuestionDefinition.DateValidationOption;
import services.question.types.DateQuestionDefinition.DateValidationPredicates;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;

public class DateQuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    UUID initialToken = UUID.randomUUID();
    DateQuestionForm form = new DateQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setConcurrencyToken(initialToken);
    QuestionDefinitionBuilder builder = form.getBuilder();

    DateQuestionDefinition expected =
        new DateQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("name")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
                .setQuestionHelpText(LocalizedStrings.empty())
                .setConcurrencyToken(initialToken)
                .build());

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    DateValidationOption minDate =
        DateValidationOption.builder().setDateType(APPLICATION_DATE).build();
    DateValidationOption maxDate = DateValidationOption.builder().setDateType(ANY).build();
    DateValidationPredicates validationPredicates =
        DateValidationPredicates.create(Optional.of(minDate), Optional.of(maxDate));
    DateQuestionDefinition originalQd =
        new DateQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("name")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
                .setQuestionHelpText(LocalizedStrings.empty())
                .setConcurrencyToken(UUID.randomUUID())
                .setValidationPredicates(validationPredicates)
                .build());

    DateQuestionForm form = new DateQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder();

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(originalQd);
  }
}
