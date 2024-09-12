package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import views.dev.IconsView;

public final class IconsController extends Controller {

  private final IconsView iconsView;

  @Inject
  public IconsController(IconsView iconsView) {
    this.iconsView = checkNotNull(iconsView);
  }

  public Result index(Http.Request request) {
    return ok(iconsView.render(request));
  }
}
