package views.admin.programs.predicates;

import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.settings.SettingsManifest;
import views.admin.BaseView;

/**
 * Partial view for rendering ConditionListPartial.html. This partial is used for displaying a list
 * of conditions within a predicate.
 */
public final class ConditionListPartialView extends BaseView<ConditionListPartialViewModel> {
  @Inject
  public ConditionListPartialView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest) {
    super(templateEngine, playThymeleafContextFactory, settingsManifest);
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/predicates/ConditionListPartial.html";
  }
}
