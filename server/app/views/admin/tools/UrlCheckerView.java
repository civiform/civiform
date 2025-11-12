package views.admin.tools;

import auth.ProfileUtils;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.settings.SettingsManifest;
import views.admin.AdminLayoutBaseView;

public class UrlCheckerView extends AdminLayoutBaseView<UrlCheckerViewModel> {
  @Inject
  public UrlCheckerView(
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
    return "URL Checker";
  }

  @Override
  protected String pageTemplate() {
    return "admin/tools/UrlChecker.html";
  }
}
