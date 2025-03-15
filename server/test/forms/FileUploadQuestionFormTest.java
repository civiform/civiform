package forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import java.util.Locale;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.types.FileUploadQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;

public class FileUploadQuestionFormTest {
  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    UUID initialToken = UUID.randomUUID();
    FileUploadQuestionForm form = new FileUploadQuestionForm();
    form.setQuestionName("file upload");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    form.setConcurrencyToken(initialToken);
    form.setMaxFiles("4");
    QuestionDefinitionBuilder builder = form.getBuilder();

    FileUploadQuestionDefinition expected =
        new FileUploadQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("file upload")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
                .setQuestionHelpText(LocalizedStrings.empty())
                .setConcurrencyToken(initialToken)
                .setValidationPredicates(
                    FileUploadQuestionDefinition.FileUploadValidationPredicates.builder()
                        .setMaxFiles(OptionalInt.of(4))
                        .build())
                .build());

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    FileUploadQuestionDefinition originalQd =
        new FileUploadQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("file upload")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
                .setQuestionHelpText(LocalizedStrings.empty())
                .setConcurrencyToken(UUID.randomUUID())
                .setValidationPredicates(
                    FileUploadQuestionDefinition.FileUploadValidationPredicates.builder()
                        .setMaxFiles(OptionalInt.of(4))
                        .build())
                .build());

    FileUploadQuestionForm form = new FileUploadQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder();

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(originalQd);
  }

  @Test
  public void setMaxFiles_emptyStringClearsMaxFiles() throws Exception {
    FileUploadQuestionForm form = new FileUploadQuestionForm();
    form.setMaxFiles("4");

    assertThat(form.getMaxFiles().getAsInt()).isEqualTo(4);

    form.setMaxFiles("");

    assertThat(form.getMaxFiles()).isEmpty();
  }

  @Test
  public void setMaxFiles_invalidStringThrowsException() throws Exception {
    FileUploadQuestionForm form = new FileUploadQuestionForm();

    assertThrows(NumberFormatException.class, () -> form.setMaxFiles("four"));
  }
}
