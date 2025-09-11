package views.admin.questions;

import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.settings.SettingsManifest;
import views.admin.BaseView;

public final class MapQuestionSettingsFiltersPartialView
    extends BaseView<MapQuestionSettingsFiltersPartialViewModel> {
  @Inject
  public MapQuestionSettingsFiltersPartialView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest) {
    super(templateEngine, playThymeleafContextFactory, settingsManifest);
  }

  @Override
  protected String pageTemplate() {
    return "admin/questions/MapQuestionSettingsFiltersPartial.html";
  }
}
