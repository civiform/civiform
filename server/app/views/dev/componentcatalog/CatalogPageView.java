package views.dev.componentcatalog;

import com.google.inject.Inject;
import views.LayoutType;
import views.admin.DevLayoutBaseView;
import views.shared.LayoutDeps;

public class CatalogPageView extends DevLayoutBaseView<CatalogPageViewModel> {

  @Inject
  public CatalogPageView(LayoutDeps layoutDeps) {
    super(layoutDeps);
  }

  @Override
  protected String pageTitle(CatalogPageViewModel model) {
    return model.getLabel();
  }

  @Override
  protected LayoutType layoutType() {
    return LayoutType.CONTENT_WITH_RIGHT_SIDEBAR;
  }

  @Override
  protected String pageTemplate() {
    return "dev/componentcatalog/CatalogPage.html";
  }
}
