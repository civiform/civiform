package views.admin.ti;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.AdminLayout;
import views.admin.LegacyTailwindLayoutBaseView;
import views.shared.LayoutDeps;

/**
 * Thymeleaf view for the trusted intermediary group edit page, rendering
 * EditTrustedIntermediaryGroupPage.html. A 1:1 port of {@link EditTrustedIntermediaryGroupView};
 * slated for replacement by an {@code AdminLayoutBaseView} page on redesign.
 */
public final class EditTrustedIntermediaryGroupPageView
    extends LegacyTailwindLayoutBaseView<EditTrustedIntermediaryGroupPageViewModel> {

  @Inject
  public EditTrustedIntermediaryGroupPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(EditTrustedIntermediaryGroupPageViewModel model, Messages messages) {
    return "Trusted intermediary groups";
  }

  @Override
  protected AdminLayout.NavPage activeNavigationPage() {
    return AdminLayout.NavPage.INTERMEDIARIES;
  }

  @Override
  protected String pageTemplate() {
    return "admin/ti/EditTrustedIntermediaryGroupPage.html";
  }
}
