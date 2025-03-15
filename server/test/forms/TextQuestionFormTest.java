package forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.Test;
import services.LocalizedStrings;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;
import services.question.types.TextQuestionDefinition;

public class TextQuestionFormTest {

  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    UUID initialToken = UUID.randomUUID();
    TextQuestionForm form = new TextQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setMinLength("4");
    form.setMaxLength("6");
    form.setConcurrencyToken(initialToken.toString());
    QuestionDefinitionBuilder builder = form.getBuilder();

    TextQuestionDefinition expected =
        new TextQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("name")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
                .setQuestionHelpText(LocalizedStrings.empty())
                .setValidationPredicates(
                    TextQuestionDefinition.TextValidationPredicates.create(4, 6))
                .setConcurrencyToken(Optional.of(initialToken))
                .build());

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    UUID initialToken = UUID.randomUUID();
    TextQuestionDefinition originalQd =
        new TextQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("name")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
                .setQuestionHelpText(LocalizedStrings.empty())
                .setValidationPredicates(
                    TextQuestionDefinition.TextValidationPredicates.create(4, 6))
                .setConcurrencyToken(Optional.of(initialToken))
                .build());

    TextQuestionForm form = new TextQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder();

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(originalQd);
  }

  @Test
  public void getBuilder_emptyStringMinMax_noPredicateSet() throws Exception {
    UUID initialToken = UUID.randomUUID();
    TextQuestionForm form = new TextQuestionForm();
    form.setQuestionName("name");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setMinLength("");
    form.setMaxLength("");
    form.setConcurrencyToken(initialToken.toString());
    QuestionDefinitionBuilder builder = form.getBuilder();

    TextQuestionDefinition expected =
        new TextQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("name")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
                .setQuestionHelpText(LocalizedStrings.empty())
                .setConcurrencyToken(Optional.of(initialToken))
                .build());

    QuestionDefinition actual = builder.build();

    // so this issue is that one has a concurrency token and one doesn't, cause the form generates one if it's missing
    // it's a similar issue as the repo test.
    // i can write a new type of equals caues normally we do want a full equals
    // or I can add the token to the expected
    // or I can remove the token from the form
    // oh! i _should_ be comparing with the token cause that's what we're actually
    // testing here
    assertThat(actual).isEqualTo(expected);
  }
}
