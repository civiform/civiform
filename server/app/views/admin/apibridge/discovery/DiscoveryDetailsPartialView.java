package views.admin.apibridge.discovery;

import com.google.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.settings.SettingsManifest;
import views.admin.BaseView;

/** View setup for rendering the DiscoveryDetailsPartial.html */
public class DiscoveryDetailsPartialView extends BaseView<DiscoveryDetailsPartialViewModel> {

  @Inject
  public DiscoveryDetailsPartialView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest) {
    super(templateEngine, playThymeleafContextFactory, settingsManifest);
  }

  @Override
  protected String pageTemplate() {
    return "admin/apibridge/discovery/DiscoveryDetailsPartial";
  }
}
