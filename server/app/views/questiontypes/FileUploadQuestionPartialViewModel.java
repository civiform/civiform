package views.questiontypes;

import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import services.applicant.ValidationErrorMessage;
import services.applicant.question.FileUploadQuestion;
import views.BaseViewModel;

@Getter
@Builder(toBuilder = true)
public class FileUploadQuestionPartialViewModel implements BaseViewModel {
  private final FileUploadQuestion fileUploadQuestion;

  private final long htmxProgramId;

  private final String htmxBlockId;

  @Default @Nullable private final ValidationErrorMessage fileTypeValidationError = null;
}
