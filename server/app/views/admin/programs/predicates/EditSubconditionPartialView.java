package views.admin.programs.predicates;

import javax.inject.Inject;
import views.admin.BaseView;
import views.shared.BaseViewDeps;

/**
 * Partial view for rendering EditConditionPartial.html. This partial is used for editing a
 * subcondition within a condition of a predicate.
 */
public final class EditSubconditionPartialView extends BaseView<EditSubconditionPartialViewModel> {
  @Inject
  public EditSubconditionPartialView(BaseViewDeps baseViewDeps) {
    super(baseViewDeps);
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/predicates/EditSubconditionPartial.html";
  }
}
