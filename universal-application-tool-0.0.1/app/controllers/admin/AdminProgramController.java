package controllers.admin;

import java.util.Map;
import javax.inject.Inject;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.program.ProgramService;
import views.admin.ProgramList;
import views.admin.ProgramNewOne;

/**
 * This controller contains an action to handle HTTP requests to the application's Admin "Create
 * application" page.
 */
public class AdminProgramController extends Controller {

  private final ProgramService service;
  private final ProgramList listView;
  private final ProgramNewOne newOneView;

  @Inject
  public AdminProgramController(
      ProgramService service, ProgramList listView, ProgramNewOne newOneView) {
    this.service = service;
    this.listView = listView;
    this.newOneView = newOneView;
  }

  public Result list() {
    return ok(listView.render(this.service.listProgramDefinitions()));
  }

  public Result newOne(Request request) {
    return ok(newOneView.render(request));
  }

  public Result create(Request request) {
    Map<String, String[]> form = request.body().asFormUrlEncoded();
    service.createProgramDefinition(form.get("name")[0], form.get("description")[0]);
    return list();
  }
}
