package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.DeploymentType;
import views.dev.IconsView;

public final class IconsController extends Controller {

  private final IconsView iconsView;
  private final boolean isDevOrStaging;

  @Inject
  public IconsController(IconsView iconsView, DeploymentType deploymentType) {
    this.iconsView = checkNotNull(iconsView);
    this.isDevOrStaging = deploymentType.isDevOrStaging();
  }

  public Result index(Http.Request request) {
    if (!isDevOrStaging) {
      return notFound();
    }
    return ok(iconsView.render(request));
  }
}
