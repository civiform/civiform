package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import mapping.dev.IconsPageMapper;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.dev.IconsPageView;
import views.dev.IconsPageViewModel;

public final class IconsController extends Controller {

  private final IconsPageView iconsPageView;

  @Inject
  public IconsController(IconsPageView iconsPageView) {
    this.iconsPageView = checkNotNull(iconsPageView);
  }

  public Result index(Http.Request request) {
    IconsPageViewModel model = new IconsPageMapper().map();

    return ok(iconsPageView.render(request, model)).as(Http.MimeTypes.HTML);
  }
}
