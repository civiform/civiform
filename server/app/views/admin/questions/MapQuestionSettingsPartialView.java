package views.admin.questions;

import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.settings.SettingsManifest;
import views.admin.BaseView;

import javax.inject.Inject;

public final class MapQuestionSettingsPartialView extends BaseView<MapQuestionSettingsPartialViewModel> {
  @Inject
  public MapQuestionSettingsPartialView(
    TemplateEngine templateEngine,
    ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
    SettingsManifest settingsManifest) {
    super(templateEngine, playThymeleafContextFactory, settingsManifest);
  }

  @Override
  protected String pageTemplate() {
    return "admin/questions/MapQuestionSettingsPartial.html";
  }
}
