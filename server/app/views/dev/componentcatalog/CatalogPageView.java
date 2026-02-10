package views.dev.componentcatalog;

import com.google.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.BundledAssetsFinder;
import services.settings.SettingsManifest;
import views.admin.DevLayoutBaseView;

public class CatalogPageView extends DevLayoutBaseView<CatalogPageViewModel> {

  @Inject
  public CatalogPageView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest,
      BundledAssetsFinder bundledAssetsFinder) {
    super(templateEngine, playThymeleafContextFactory, settingsManifest, bundledAssetsFinder);
  }

  @Override
  protected String pageTitle(CatalogPageViewModel model) {
    return "Component Catalog";
  }

  @Override
  protected String pageTemplate() {
    return "dev/componentcatalog/CatalogPage.html";
  }
}
