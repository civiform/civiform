package views.admin.apibridge.programbridge.fragments;

import com.google.inject.Inject;
import views.BaseView;
import views.shared.BaseViewDeps;

/** View object for rendering the program bridge edit pages form */
public class ProgramBridgeEditPartialView extends BaseView<ProgramBridgeEditPartialViewModel> {
  @Inject
  public ProgramBridgeEditPartialView(BaseViewDeps baseViewDeps) {
    super(baseViewDeps);
  }

  @Override
  protected String pageTemplate() {
    return "admin/apibridge/programbridge/fragments/ProgramBridgeEditPartial";
  }
}
