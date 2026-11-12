package views.admin.ti;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.LegacyTailwindLayoutBaseView;
import views.shared.LayoutDeps;

/**
 * Thymeleaf view for the trusted intermediary group list page, rendering
 * TrustedIntermediaryGroupIndexPage.html. A 1:1 port of {@link TrustedIntermediaryGroupListView};
 * the redesigned page is {@link TrustedIntermediaryGroupListPageView}.
 */
public final class TrustedIntermediaryGroupIndexPageView
    extends LegacyTailwindLayoutBaseView<TrustedIntermediaryGroupIndexPageViewModel> {

  @Inject
  public TrustedIntermediaryGroupIndexPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(TrustedIntermediaryGroupIndexPageViewModel model, Messages messages) {
    return "Manage trusted intermediaries";
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.INTERMEDIARIES;
  }

  @Override
  protected String pageTemplate() {
    return "admin/ti/TrustedIntermediaryGroupIndexPage.html";
  }
}
