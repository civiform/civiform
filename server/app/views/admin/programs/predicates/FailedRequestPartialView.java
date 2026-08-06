package views.admin.programs.predicates;

import javax.inject.Inject;
import views.BaseView;
import views.shared.BaseViewDeps;

/**
 * Partial view for rendering FailedRequestPartial.html. This partial is used for displaying HTMX
 * request errors.
 */
public final class FailedRequestPartialView extends BaseView<FailedRequestPartialViewModel> {
  @Inject
  public FailedRequestPartialView(BaseViewDeps baseViewDeps) {
    super(baseViewDeps);
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/predicates/FailedRequestPartial.html";
  }
}
