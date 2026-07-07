package views.admin.apibridge.programbridge;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.TransitionalLayoutBaseView;
import views.shared.LayoutDeps;

/** View object for rendering the program bridge edit page */
public class EditPageView extends TransitionalLayoutBaseView<EditPageViewModel> {
  @Inject
  public EditPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(EditPageViewModel model, Messages messages) {
    return "Edit Program Bridge Definitions";
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.PROGRAMS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/apibridge/programbridge/EditPage";
  }
}
