package views.admin.programs.predicates;

import javax.inject.Inject;
import views.BaseView;
import views.shared.BaseViewDeps;

/**
 * Partial view for rendering EditConditionPartial.html. This partial is used for editing a
 * condition within a predicate.
 */
public final class EditConditionPartialView extends BaseView<EditConditionPartialViewModel> {
  @Inject
  public EditConditionPartialView(BaseViewDeps baseViewDeps) {
    super(baseViewDeps);
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/predicates/EditConditionPartial.html";
  }
}
