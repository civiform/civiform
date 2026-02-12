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

  public Result defaultIndex(Http.Request request) {
    return redirect(routes.ComponentCatalogController.controlIndex("button").url());
  }

  public Result controlIndex(Http.Request request, String controlName) {
    if (!CatalogPageViewModel.controlExists(controlName)) {
      return notFound();
    }

    var viewModel = CatalogPageViewModel.builder().controlName(controlName).build();
    return ok(catalogPageView.render(request, viewModel)).as(Http.MimeTypes.HTML);
  }
}
