package views.dev;

import javax.inject.Inject;
import play.i18n.Messages;
import views.shared.LayoutDeps;
import views.shared.UnauthenticatedLayoutBaseView;

/** Thymeleaf view listing every SVG icon in {@link views.components.Icons}. */
public final class IconsPageView extends UnauthenticatedLayoutBaseView<IconsPageViewModel> {
  @Inject
  public IconsPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(IconsPageViewModel model, Messages messages) {
    return "Icons";
  }

  @Override
  protected String pageTemplate() {
    return "dev/IconsPage.html";
  }
}
