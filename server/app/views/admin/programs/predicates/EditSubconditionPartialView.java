package views.admin.programs.predicates;

import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import services.settings.SettingsManifest;
import views.admin.BaseView;

/**
 * Partial view for rendering EditConditionPartial.html. This partial is used for editing a
 * subcondition within a condition of a predicate.
 */
public final class EditSubconditionPartialView extends BaseView<EditSubconditionPartialViewModel> {
  @Inject
  public EditSubconditionPartialView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      SettingsManifest settingsManifest) {
    super(templateEngine, playThymeleafContextFactory, settingsManifest);
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/predicates/EditSubconditionPartial.html";
  }
}
