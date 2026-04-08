package views.questiontypes;

import javax.inject.Inject;
import views.admin.BaseView;
import views.shared.BaseViewDeps;

public class FileUploadQuestionPartialView extends BaseView<FileUploadQuestionPartialViewModel> {
  @Inject
  public FileUploadQuestionPartialView(BaseViewDeps baseViewDeps) {
    super(baseViewDeps);
  }

  @Override
  protected String pageTemplate() {
    return "questiontypes/FileUploadQuestionPartial";
  }
}
