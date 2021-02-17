package controllers.admin;

import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.Result;
import services.program.ProgramService;
import views.admin.ProgramList;

/**
 * This controller contains an action to handle HTTP requests to the application's Admin "Create
 * application" page.
 */
public class AdminProgramController extends Controller {

  private final ProgramService service;
  private final ProgramList listView;

  @Inject
  public AdminProgramController(ProgramService service, ProgramList listView) {
    this.service = service;
    this.listView = listView;
  }

  public Result list() {
    return ok(listView.render(this.service.listProgramDefinitions()));
  }
}
