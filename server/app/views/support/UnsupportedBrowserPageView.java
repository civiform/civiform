package views.support;

import javax.inject.Inject;
import play.i18n.Messages;
import views.BaseViewModel;
import views.shared.LayoutDeps;
import views.shared.UnauthenticatedLayoutBaseView;

/** Thymeleaf view for the page shown to browsers UnsupportedBrowserFilter rejects (IE). */
public final class UnsupportedBrowserPageView
    extends UnauthenticatedLayoutBaseView<UnsupportedBrowserPageView.Model> {

  /** Model for the unsupported browser page. */
  public record Model() implements BaseViewModel {}

  @Inject
  public UnsupportedBrowserPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(Model model, Messages messages) {
    return "The browser you are currently using is not supported";
  }

  @Override
  protected String pageHeading(Model model, Messages messages) {
    return "Unsupported browser";
  }

  @Override
  protected String pageTemplate() {
    return "support/UnsupportedBrowserPage.html";
  }
}
