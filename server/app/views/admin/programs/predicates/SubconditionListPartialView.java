package views.admin.programs.predicates;

import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.settings.SettingsManifest;
import views.admin.BaseView;

/**
 * Partial view for rendering SubconditionListPartial.html. This partial is used for displaying a
 * list of subconditions within a predicate condition.
 */
public final class SubconditionListPartialView extends BaseView<SubconditionListPartialViewModel> {
  @Inject
  public SubconditionListPartialView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest) {
    super(templateEngine, playThymeleafContextFactory, settingsManifest);
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/predicates/SubconditionListPartial.html";
  }
}
