package forms;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import org.junit.Test;
import services.Path;
import services.question.types.FileUploadQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;

public class FileUploadQuestionFormTest {
  @Test
  public void getBuilder_returnsCompleteBuilder() throws Exception {
    Path path = Path.create("my.question.path.file_upload");

    FileUploadQuestionForm form = new FileUploadQuestionForm();
    form.setQuestionName("file upload");
    form.setQuestionDescription("description");
    form.setQuestionText("What is the question text?");
    form.setQuestionHelpText("");
    QuestionDefinitionBuilder builder = form.getBuilder(path);

    FileUploadQuestionDefinition expected =
        new FileUploadQuestionDefinition(
            "file upload",
            path,
            Optional.empty(),
            "description",
            ImmutableMap.of(Locale.US, "What is the question text?"),
            ImmutableMap.of());

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void getBuilder_withQdConstructor_returnsCompleteBuilder() throws Exception {
    Path path = Path.create("my.question.path.file_upload");

    FileUploadQuestionDefinition originalQd =
        new FileUploadQuestionDefinition(
            "file upload",
            path,
            Optional.empty(),
            "description",
            ImmutableMap.of(Locale.US, "What is the question text?"),
            ImmutableMap.of());

    FileUploadQuestionForm form = new FileUploadQuestionForm(originalQd);
    QuestionDefinitionBuilder builder = form.getBuilder(path);

    QuestionDefinition actual = builder.build();

    assertThat(actual).isEqualTo(originalQd);
  }
}
