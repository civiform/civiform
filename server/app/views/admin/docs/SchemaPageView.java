package views.admin.docs;

import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.BundledAssetsFinder;
import services.settings.SettingsManifest;
import views.admin.AdminLayout;
import views.admin.AdminLayoutBaseView;
import views.admin.ScriptElementSettings;

public final class SchemaPageView extends AdminLayoutBaseView<SchemaPageViewModel> {

  private final BundledAssetsFinder bundledAssetsFinder;

  @Inject
  public SchemaPageView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      BundledAssetsFinder bundledAssetsFinder,
      ProfileUtils profileUtils,
      SettingsManifest settingsManifest) {
    super(
        templateEngine,
        playThymeleafContextFactory,
        settingsManifest,
        bundledAssetsFinder,
        profileUtils);
    this.bundledAssetsFinder = bundledAssetsFinder;
  }

  @Override
  protected String pageTitle(SchemaPageViewModel model) {
    return "API schema";
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
            .build(),
        ScriptElementSettings.builder()
            .src(bundledAssetsFinder.getSwaggeruiPresetJs())
            .type("text/javascript")
            .build());
  }
}
