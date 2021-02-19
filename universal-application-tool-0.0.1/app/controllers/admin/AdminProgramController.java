package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import forms.ProgramForm;
import javax.inject.Inject;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.program.ProgramService;
import views.admin.ProgramListView;
import views.admin.ProgramNewOneView;

/**
 * This controller contains an action to handle HTTP requests to the UAT's Admin "Create
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
    this.service = checkNotNull(service);
    this.listView = checkNotNull(listView);
    this.newOneView = checkNotNull(newOneView);
    this.formFactory = checkNotNull(formFactory);
  }

  public Result index() {
    return ok(listView.render(this.service.listProgramDefinitions()));
  }

  public Result newOne(Request request) {
    return ok(newOneView.render(request));
  }

  public Result create(Request request) {
    Form<ProgramForm> programForm = formFactory.form(ProgramForm.class);
    ProgramForm program = programForm.bindFromRequest(request).get();
    service.createProgramDefinition(program.getName(), program.getDescription());
    return found(controllers.admin.routes.AdminProgramController.index());
  }
}
