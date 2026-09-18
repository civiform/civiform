package views.admin.programs.predicates;

import com.google.inject.Inject;
import views.admin.AdminLayout;
import views.admin.TransitionalLayoutBaseView;
import views.shared.LayoutDeps;

/**
 * Page view for rendering EditPredicatePageView.html. This page is used for editing predicates of a
 * block in a program.
 */
public class EditPredicatePageView extends TransitionalLayoutBaseView<EditPredicatePageViewModel> {
  @Inject
  public EditPredicatePageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.PROGRAMS;
  }

  @Override
  protected String pageTemplate() {
    return "admin/programs/predicates/EditPredicatePageView.html";
  }

  @Override
  @SuppressWarnings("deprecation")
  protected boolean isWidescreen() {
    return true;
  }
}
