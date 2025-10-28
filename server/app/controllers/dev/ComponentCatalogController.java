package controllers.dev;

import static com.google.gdata.util.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.dev.componentcatalog.CatalogPageView;
import views.dev.componentcatalog.CatalogPageViewModel;

@Slf4j
public class ComponentCatalogController extends Controller {
  private final CatalogPageView catalogPageView;

  @Inject
  ComponentCatalogController(CatalogPageView componentCatalogPageView) {
    this.catalogPageView = checkNotNull(componentCatalogPageView);
  }

  public Result index(Http.Request request) {
    var viewmodel = new CatalogPageViewModel();
    return ok(catalogPageView.render(request, viewmodel)).as(Http.MimeTypes.HTML);
  }
}
