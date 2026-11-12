package views.admin.apikeys;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.TransitionalLayoutBaseView;
import views.shared.LayoutDeps;

/** Thymeleaf view for the API keys index page, rendering ApiKeyIndexPage.html. */
public final class ApiKeyIndexPageView
    extends TransitionalLayoutBaseView<ApiKeyIndexPageViewModel> {

  @Inject
  public ApiKeyIndexPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(ApiKeyIndexPageViewModel model, Messages messages) {
    return model.getTitle();
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.API_KEYS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/apikeys/ApiKeyIndexPage.html";
  }
}
