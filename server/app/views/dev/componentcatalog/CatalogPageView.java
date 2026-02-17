package views.dev.componentcatalog;

import com.google.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.BundledAssetsFinder;
import services.settings.SettingsManifest;
import views.admin.DevLayoutBaseView;
import views.admin.LayoutType;

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
    return model.getLabel();
  }

  @Override
  protected LayoutType layoutType() {
    return LayoutType.CONTENT_WITH_RIGHT_SIDEBAR;
  }

  @Override
  protected String pageTemplate() {
    return "dev/componentcatalog/CatalogPage.html";
  }
}
