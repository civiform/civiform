package controllers.admin;

import controllers.AssetsFinder;
import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.admin.create_app;

/**
 * This controller contains an action to handle HTTP requests to the application's Admin "Create
 * application" page.
 */
public class CreateAppController extends Controller {

  private final AssetsFinder assetsFinder;

  @Inject
  public CreateAppController(AssetsFinder assetsFinder) {
    this.assetsFinder = assetsFinder;
  }

  // TODO(natsid): We probably want something similar to the example app's
  //  "main" template, which contains the header, styles, etc that are common
  //  to all pages of the app.

  /** An action that renders an HTML page. */
  public Result index() {
    return ok(create_app.render("Create a new application", assetsFinder));
  }
}
