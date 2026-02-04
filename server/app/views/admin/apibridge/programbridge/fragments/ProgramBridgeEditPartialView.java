package views.admin.apibridge.programbridge.fragments;

import com.google.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.settings.SettingsManifest;
import views.admin.BaseView;

/** View object for rendering the program bridge edit pages form */
public class ProgramBridgeEditPartialView extends BaseView<ProgramBridgeEditPartialViewModel> {
  @Inject
  public ProgramBridgeEditPartialView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest) {
    super(templateEngine, playThymeleafContextFactory, settingsManifest);
  }

  @Override
  protected String pageTemplate() {
    return "admin/apibridge/programbridge/fragments/ProgramBridgeEditPartial";
  }
}
