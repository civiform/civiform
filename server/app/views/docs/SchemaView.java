package views.docs;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.settings.SettingsManifest;
import views.admin.AdminBaseView;
import views.admin.AdminLayout;

public class SchemaView extends AdminBaseView<SchemaViewModel> {
  @Inject
  public SchemaView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest,
      AssetsFinder assetsFinder,
      ProfileUtils profileUtils) {
    super(
        templateEngine, playThymeleafContextFactory, settingsManifest, assetsFinder, profileUtils);
  }

  @Override
  protected String pageTitle() {
    return "API schema";
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.API_DOCS;
  }

  @Override
  protected String pageTemplate() {
    return "docs/SchemaView";
  }

  @Override
  protected ImmutableList<String> getPageStylesheets() {
    return ImmutableList.of(
        assetsFinder.path("swagger-ui/swagger-ui.css"),
        assetsFinder.path("stylesheets/tailwind.css"));
  }

  @Override
  protected ImmutableList<String> getPageBodyScripts() {
    return ImmutableList.of(
        assetsFinder.path("swagger-ui/swagger-ui-bundle.js"),
        assetsFinder.path("swagger-ui/swagger-ui-standalone-preset.js"));
  }
}
