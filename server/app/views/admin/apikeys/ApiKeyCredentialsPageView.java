package views.admin.apikeys;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.LegacyTailwindLayoutBaseView;
import views.shared.LayoutDeps;

/** Thymeleaf view for the API key credentials page, rendering ApiKeyCredentialsPage.html. */
public final class ApiKeyCredentialsPageView
    extends LegacyTailwindLayoutBaseView<ApiKeyCredentialsPageViewModel> {

  @Inject
  public ApiKeyCredentialsPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(ApiKeyCredentialsPageViewModel model, Messages messages) {
    return model.getTitle();
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.API_KEYS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/apikeys/ApiKeyCredentialsPage.html";
  }
}
