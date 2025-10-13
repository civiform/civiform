package views.admin.programs.predicates;

import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.settings.SettingsManifest;
import views.admin.BaseView;

/**
 * Partial view for rendering FailedRequestPartial.html. This partial is used for displaying HTMX
 * request errors.
 */
public final class FailedRequestPartialView extends BaseView<FailedRequestPartialViewModel> {
  @Inject
  public FailedRequestPartialView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest) {
    super(templateEngine, playThymeleafContextFactory, settingsManifest);
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/predicates/FailedRequestPartial.html";
  }
}
