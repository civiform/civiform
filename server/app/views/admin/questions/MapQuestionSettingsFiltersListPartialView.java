package views.admin.questions;

import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.settings.SettingsManifest;
import views.admin.BaseView;

public final class MapQuestionSettingsFiltersListPartialView
    extends BaseView<MapQuestionSettingsFiltersListPartialViewModel> {
  @Inject
  public MapQuestionSettingsFiltersListPartialView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest) {
    super(templateEngine, playThymeleafContextFactory, settingsManifest);
  }

  @Override
  protected String pageTemplate() {
    return "admin/questions/MapQuestionSettingsFiltersListPartial.html";
  }
}
