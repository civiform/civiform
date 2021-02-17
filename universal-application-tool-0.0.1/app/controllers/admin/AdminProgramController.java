package controllers.admin;

import controllers.AssetsFinder;
import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.Result;
import repository.ProgramRepository;
import services.program.ProgramService;
import views.admin.ProgramList;
import views.html.admin.create_app;

/**
 * This controller contains an action to handle HTTP requests to the application's Admin "Create
 * application" page.
 */
public class AdminProgramController extends Controller {

  private final AssetsFinder assetsFinder;
  private final ProgramService service;
  private final ProgramList listView;

  @Inject
  public AdminProgramController(
      AssetsFinder assetsFinder, ProgramService service, ProgramList listView) {
    this.assetsFinder = assetsFinder;
    this.service = service;
    this.listView = listView;
  }

  public Result list() {
    return ok(listView.render(this.service));
  }
}
