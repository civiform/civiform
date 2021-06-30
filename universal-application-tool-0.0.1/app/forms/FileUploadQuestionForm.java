package forms;

import services.question.types.FileUploadQuestionDefinition;
import services.question.types.QuestionType;

/** Form for updating a file upload question. */
public class FileUploadQuestionForm extends QuestionForm {
  public FileUploadQuestionForm() {
    super();
  }

  public FileUploadQuestionForm(FileUploadQuestionDefinition qd) {
    super(qd);
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.FILEUPLOAD;
  }
}
