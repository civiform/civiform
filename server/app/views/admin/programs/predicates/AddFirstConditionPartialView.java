package views.admin.programs.predicates;

import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.settings.SettingsManifest;
import views.admin.BaseView;

/**
 * Partial view for rendering AddFirstConditionPartial.html. This partial is used for rendering the
 * first AddConditionFragment on the predicate edit page.
 */
public final class AddFirstConditionPartialView
    extends BaseView<AddFirstConditionPartialViewModel> {
  @Inject
  public AddFirstConditionPartialView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest) {
    super(templateEngine, playThymeleafContextFactory, settingsManifest);
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/predicates/AddFirstConditionPartial.html";
  }
}
