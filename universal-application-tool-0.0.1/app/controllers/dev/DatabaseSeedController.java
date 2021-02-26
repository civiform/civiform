package controllers.dev;

import com.google.inject.Inject;
import play.mvc.Controller;
import play.mvc.Result;
import views.dev.DatabaseSeedView;

/** Controller for seeding the database with test content to develop against. */
public class DatabaseSeedController extends Controller {
  private final DatabaseSeedView view;

  @Inject
  public DatabaseSeedController(DatabaseSeedView view) {
    this.view = view;
  }

  public Result ask() {
    return ok(view.render());
  }

  public Result seed() {
    return ok("Seeds planted");
  }
}
