package views.admin.tools;

import com.google.inject.Inject;
import play.i18n.Messages;
import views.admin.TransitionalLayoutBaseView;
import views.shared.LayoutDeps;

public class UrlCheckerView extends TransitionalLayoutBaseView<UrlCheckerViewModel> {
  @Inject
  public UrlCheckerView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(UrlCheckerViewModel model, Messages messages) {
    return "URL Checker";
  }

  @Override
  protected String pageTemplate() {
    return "admin/tools/UrlChecker.html";
  }
}
