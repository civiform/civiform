package forms;

import java.util.OptionalInt;
import services.question.types.FileUploadQuestionDefinition;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

/** Form for updating a file upload question. */
public class FileUploadQuestionForm extends QuestionForm {
  private OptionalInt maxFiles;

  public FileUploadQuestionForm() {
    super();
    maxFiles = OptionalInt.empty();
  }

  public FileUploadQuestionForm(FileUploadQuestionDefinition qd) {
    super(qd);
    maxFiles = qd.getMaxFiles();
  }

  public OptionalInt getMaxFiles() {
    return maxFiles;
  }

  /**
   * We use a string parameter here so that if the field is empty (i.e. unset), we can correctly set
   * to an empty OptionalInt. Since the HTML input is type "number", we can be sure this string is
   * in fact an integer when we parse it. If we instead used an int here, we see an "Invalid value"
   * error when binding the empty value in the form.
   */
  public void setMaxFiles(String maxFiles) {
    if (maxFiles.isEmpty()) {
      this.maxFiles = OptionalInt.empty();
    } else {
      this.maxFiles = OptionalInt.of(Integer.parseInt(maxFiles));
    }
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.FILEUPLOAD;
  }

  @Override
  public QuestionDefinitionBuilder getBuilder() {
    FileUploadQuestionDefinition.FileUploadValidationPredicates.Builder
        fileUploadPredicatesBuilder =
            FileUploadQuestionDefinition.FileUploadValidationPredicates.builder();

    fileUploadPredicatesBuilder.setMaxFiles(getMaxFiles());

    return super.getBuilder().setValidationPredicates(fileUploadPredicatesBuilder.build());
  }
}
