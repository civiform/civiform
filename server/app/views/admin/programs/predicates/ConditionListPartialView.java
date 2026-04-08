package views.admin.programs.predicates;

import javax.inject.Inject;
import views.admin.BaseView;
import views.shared.BaseViewDeps;

/**
 * Partial view for rendering ConditionListPartial.html. This partial is used for displaying a list
 * of conditions within a predicate.
 */
public final class ConditionListPartialView extends BaseView<ConditionListPartialViewModel> {
  @Inject
  public ConditionListPartialView(BaseViewDeps baseViewDeps) {
    super(baseViewDeps);
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/predicates/ConditionListPartial.html";
  }
}
