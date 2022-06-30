package controllers.dev;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import com.typesafe.config.Config;
import play.Environment;
import play.mvc.Result;
import views.dev.IconsView;

public final class IconsController extends DevController {

  private final IconsView iconsView;

  @Inject
  public IconsController(IconsView iconsView, Environment environment, Config configuration) {
    super(environment, configuration);
    this.iconsView = checkNotNull(iconsView);
  }

  public Result index() {
    if (!isDevOrStagingEnvironment()) {
      return notFound();
    }
    return ok(iconsView.render());
  }
}
