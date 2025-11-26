package views.dev.componentcatalog;

import com.google.inject.Inject;
import controllers.AssetsFinder;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.ViteService;
import services.settings.SettingsManifest;
import views.admin.DevLayoutBaseView;

public class CatalogPageView extends DevLayoutBaseView<CatalogPageViewModel> {

  @Inject
  public CatalogPageView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest,
      ViteService viteService,
      AssetsFinder assetFinder) {
    super(templateEngine, playThymeleafContextFactory, settingsManifest, viteService, assetFinder);
  }

  @Override
  protected String pageTitle() {
    return "Component Catalog";
  }

  @Override
  protected String pageTemplate() {
    return "dev/componentcatalog/CatalogPage.html";
  }
}
