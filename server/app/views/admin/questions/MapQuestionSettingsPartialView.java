package views.admin.questions;

import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.mvc.Http;
import services.settings.SettingsManifest;
import views.admin.BaseView;
import views.components.Icons;

public final class MapQuestionSettingsPartialView
    extends BaseView<MapQuestionSettingsPartialViewModel> {
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

  @Override
  protected void customizeContext(
      Http.Request request, ThymeleafModule.PlayThymeleafContext context) {
    context.setVariable("deleteIcon", Icons.DELETE);
  }
}
