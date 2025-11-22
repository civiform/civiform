package views.admin.tools;

import auth.ProfileUtils;
import com.google.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.ViteService;
import services.settings.SettingsManifest;
import views.admin.AdminLayoutBaseView;

public class UrlCheckerView extends AdminLayoutBaseView<UrlCheckerViewModel> {
  @Inject
  public UrlCheckerView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest,
      ViteService viteService,
      ProfileUtils profileUtils) {
    super(templateEngine, playThymeleafContextFactory, settingsManifest, viteService, profileUtils);
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
