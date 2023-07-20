package forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.Test;
import services.LocalizedStrings;
import services.question.types.FileUploadQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionDefinitionConfig;

public class FileUploadQuestionFormTest {
  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    FileUploadQuestionForm form = new FileUploadQuestionForm();
    form.setQuestionName("file upload");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    QuestionDefinitionBuilder builder = form.getBuilder();

    FileUploadQuestionDefinition expected =
        new FileUploadQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName("file upload")
                .setDescription("description")
                .setQuestionText(LocalizedStrings.of(Locale.US, "What is the question text?"))
                .setQuestionHelpText(LocalizedStrings.empty())
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
                .build());

    FileUploadQuestionForm form = new FileUploadQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder();

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(originalQd);
  }
}
