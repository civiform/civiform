package views.admin.docs;

import com.google.common.collect.ImmutableList;
import javax.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.AdminLayoutBaseView;
import views.shared.LayoutDeps;
import views.shared.ScriptElementSettings;

public final class SchemaPageView extends AdminLayoutBaseView<SchemaPageViewModel> {
  @Inject
  public SchemaPageView(LayoutDeps adminLayoutDeps) {
    super(adminLayoutDeps);
  }

  @Override
  protected String pageTitle(SchemaPageViewModel model, Messages messages) {
    return messages.at("adminSchemaViewer.pageTitle");
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.API_DOCS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/docs/SchemaPage.html";
  }

  @Override
  protected ImmutableList<String> getPageStylesheets() {
    return ImmutableList.of(bundledAssetsFinder.getSwaggerUiCss());
  }

  @Override
  protected ImmutableList<ScriptElementSettings> getPageHeadScripts() {
    return ImmutableList.of(
        ScriptElementSettings.builder()
            .src(bundledAssetsFinder.getSwaggerUiJs())
            .type("text/javascript")
            .crossOrigin("anonymous")
            .build(),
        ScriptElementSettings.builder()
            .src(bundledAssetsFinder.getSwaggeruiPresetJs())
            .type("text/javascript")
            .crossOrigin("anonymous")
            .build(),
        ScriptElementSettings.builder()
            .src(bundledAssetsFinder.getPageBundle("admin/api_docs_schema_page"))
            .type("module")
            .build());
  }
}
