package views.questiontypes;

import lombok.Builder;
import lombok.Getter;
import services.applicant.question.FileUploadQuestion;
import views.BaseViewModel;

@Getter
@Builder(toBuilder = true)
public class FileUploadQuestionPartialViewModel implements BaseViewModel {
  private final FileUploadQuestion fileUploadQuestion;

  /**
   * Reverse-routed POST URL for {@link controllers.applicant.FileUploadController#hxRemoveFile}.
   */
  private final String hxRemoveFileUrl;
}
