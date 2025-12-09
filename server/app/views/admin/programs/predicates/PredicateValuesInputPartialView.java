package views.admin.programs.predicates;

import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.settings.SettingsManifest;
import views.admin.BaseView;

/**
 * Partial view for rendering PredicateValuesInputPartial.html. This partial is used for entering
 * values into the subconditions of a predicate.
 */
public final class PredicateValuesInputPartialView
    extends BaseView<PredicateValuesInputPartialViewModel> {
  @Inject
  public PredicateValuesInputPartialView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest) {
    super(templateEngine, playThymeleafContextFactory, settingsManifest);
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/predicates/PredicateValuesInputPartial.html";
  }
}
