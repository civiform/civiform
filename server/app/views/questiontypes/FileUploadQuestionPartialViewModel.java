package views.questiontypes;

import lombok.Builder;
import services.applicant.question.FileUploadQuestion;
import views.admin.BaseViewModel;

@Builder(toBuilder = true)
public class FileUploadQuestionPartialViewModel implements BaseViewModel {
  private final FileUploadQuestion fileUploadQuestion;

  public FileUploadQuestion getFileUploadQuestion() {
    return fileUploadQuestion;
  }
}
