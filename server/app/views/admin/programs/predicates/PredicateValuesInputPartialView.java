package views.admin.programs.predicates;

import javax.inject.Inject;
import views.admin.BaseView;
import views.shared.BaseViewDeps;

/**
 * Partial view for rendering PredicateValuesInputPartial.html. This partial is used for entering
 * values into the subconditions of a predicate.
 */
public final class PredicateValuesInputPartialView
    extends BaseView<PredicateValuesInputPartialViewModel> {
  @Inject
  public PredicateValuesInputPartialView(BaseViewDeps baseViewDeps) {
    super(baseViewDeps);
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/predicates/PredicateValuesInputPartial.html";
  }
}
