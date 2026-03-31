package views.admin.apibridge;

import com.google.inject.Inject;
import views.admin.BaseView;
import views.shared.BaseViewDeps;

/** View setup for rendering the MessagePartial.html */
public class MessagePartialView extends BaseView<MessagePartialViewModel> {

  @Inject
  public MessagePartialView(BaseViewDeps baseViewDeps) {
    super(baseViewDeps);
  }

  @Override
  protected String pageTemplate() {
    return "admin/apibridge/MessagePartial";
  }
}
