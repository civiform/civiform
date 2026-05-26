package views.admin.docs;

import com.google.common.collect.ImmutableList;
import javax.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.AdminLayoutBaseView;
import views.shared.LayoutDeps;
import views.shared.ScriptElementSettings;

public final class ApiDocsPageView extends AdminLayoutBaseView<ApiDocsPageViewModel> {
  @Inject
  public ApiDocsPageView(LayoutDeps adminLayoutDeps) {
    super(adminLayoutDeps);
  }

  @Override
  protected String pageTitle(ApiDocsPageViewModel model, Messages messages) {
    return messages.at("adminApiDocs.pageTitle");
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.API_DOCS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/docs/ApiDocsPage.html";
  }

  @Override
  protected ImmutableList<ScriptElementSettings> getPageBodyScripts() {
    return ImmutableList.of(
        ScriptElementSettings.builder()
            .src(bundledAssetsFinder.getPageBundle("admin/api_docs_page"))
            .type("module")
            .build());
  }
}
