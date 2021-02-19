package controllers.admin;

import javax.inject.Inject;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.program.ProgramService;
import views.admin.ProgramListView;
import views.admin.ProgramNewOneView;

/**
 * This controller contains an action to handle HTTP requests to the application's Admin "Create
 * application" page.
 */
public class AdminProgramController extends Controller {

  private final ProgramService service;
  private final ProgramListView listView;
  private final ProgramNewOneView newOneView;
  private final FormFactory formFactory;

  @Inject
  public AdminProgramController(
      ProgramService service,
      ProgramListView listView,
      ProgramNewOneView newOneView,
      FormFactory formFactory) {
    this.service = service;
    this.listView = listView;
    this.newOneView = newOneView;
    this.formFactory = formFactory;
  }

  public Result list() {
    return ok(listView.render(this.service.listProgramDefinitions()));
  }

  public Result newOne(Request request) {
    return ok(newOneView.render(request));
  }

  public Result create(Request request) {
    DynamicForm requestData = this.formFactory.form().bindFromRequest(request);
    String name = requestData.get("name");
    String description = requestData.get("description");
    service.createProgramDefinition(name, description);
    return list();
  }
}
