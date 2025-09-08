package views.admin.apibridge;

import com.google.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.settings.SettingsManifest;
import views.admin.BaseView;

/** View setup for rendering the MessagePartial.html */
public class MessagePartialView extends BaseView<MessagePartialViewModel> {

  @Inject
  public MessagePartialView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest) {
    super(templateEngine, playThymeleafContextFactory, settingsManifest);
  }

  @Override
  protected String pageTemplate() {
    return "admin/apibridge/MessagePartial";
  }
}
