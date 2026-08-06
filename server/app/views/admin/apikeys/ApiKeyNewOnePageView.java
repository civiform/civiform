package views.admin.apikeys;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.LegacyTailwindLayoutBaseView;
import views.shared.LayoutDeps;

/** Thymeleaf view for the new API key page, rendering ApiKeyNewOnePage.html. */
public final class ApiKeyNewOnePageView
    extends LegacyTailwindLayoutBaseView<ApiKeyNewOnePageViewModel> {

  @Inject
  public ApiKeyNewOnePageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(ApiKeyNewOnePageViewModel model, Messages messages) {
    return model.getTitle();
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.API_KEYS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/apikeys/ApiKeyNewOnePage.html";
  }
}
