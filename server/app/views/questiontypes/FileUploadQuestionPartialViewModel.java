package views.questiontypes;

import lombok.Builder;
import services.applicant.question.FileUploadQuestion;
import views.BaseViewModel;

@Builder(toBuilder = true)
public class FileUploadQuestionPartialViewModel implements BaseViewModel {
  private final FileUploadQuestion fileUploadQuestion;
  private final String hxRemoveFileUrl;

  public FileUploadQuestion getFileUploadQuestion() {
    return fileUploadQuestion;
  }

  public String getHxRemoveFileUrl() {
    return hxRemoveFileUrl;
  }
}
