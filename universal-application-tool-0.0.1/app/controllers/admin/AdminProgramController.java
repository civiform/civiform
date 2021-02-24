package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Roles;
import forms.ProgramForm;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.admin.ProgramEditView;
import views.admin.ProgramIndexView;
import views.admin.ProgramNewOneView;

/** Controller for handling methods for admins managing program definitions. */
public class AdminProgramController extends Controller {

  private final ProgramService service;
  private final ProgramIndexView listView;
  private final ProgramNewOneView newOneView;
  private final ProgramEditView editView;
  private final FormFactory formFactory;

  @Inject
  public AdminProgramController(
      ProgramService service,
      ProgramIndexView listView,
      ProgramNewOneView newOneView,
      ProgramEditView editView,
      FormFactory formFactory) {
    this.service = checkNotNull(service);
    this.listView = checkNotNull(listView);
    this.newOneView = checkNotNull(newOneView);
    this.editView = checkNotNull(editView);
    this.formFactory = checkNotNull(formFactory);
  }

  @Secure(authorizers = Roles.UAT_ADMIN_AUTHORIZER)
  public Result index() {
    return ok(listView.render(this.service.listProgramDefinitions()));
  }

  @Secure(authorizers = Roles.UAT_ADMIN_AUTHORIZER)
  public Result newOne(Request request) {
    return ok(newOneView.render(request));
  }

  @Secure(authorizers = Roles.UAT_ADMIN_AUTHORIZER)
  public Result create(Request request) {
    Form<ProgramForm> programForm = formFactory.form(ProgramForm.class);
    ProgramForm program = programForm.bindFromRequest(request).get();
    service.createProgramDefinition(program.getName(), program.getDescription());
    return found(routes.AdminProgramController.index());
  }

  public Result edit(Request request, long id) {
    Optional<ProgramDefinition> program = service.getProgramDefinition(id);
    if (program.isEmpty()) {
      return notFound(String.format("Program ID %d not found.", id));
    } else {
      return ok(editView.render(request, program.get()));
    }
  }

  public Result update(Request request, long id) {
    Form<ProgramForm> programForm = formFactory.form(ProgramForm.class);
    ProgramForm program = programForm.bindFromRequest(request).get();
    try {
      service.updateProgramDefinition(id, program.getName(), program.getDescription());
      return found(routes.AdminProgramController.index());
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", id));
    }
  }
}
