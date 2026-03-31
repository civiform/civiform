package views.admin.programs.predicates;

import javax.inject.Inject;
import views.admin.BaseView;
import views.shared.BaseViewDeps;

/**
 * Partial view for rendering SubconditionListPartial.html. This partial is used for displaying a
 * list of subconditions within a predicate condition.
 */
public final class SubconditionListPartialView extends BaseView<SubconditionListPartialViewModel> {
  @Inject
  public SubconditionListPartialView(BaseViewDeps baseViewDeps) {
    super(baseViewDeps);
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/predicates/SubconditionListPartial.html";
  }
}
