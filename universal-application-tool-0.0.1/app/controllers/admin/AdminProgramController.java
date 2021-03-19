package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import controllers.CiviFormController;
import forms.ProgramForm;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.CiviFormError;
import services.ErrorAnd;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.admin.programs.ProgramEditView;
import views.admin.programs.ProgramIndexView;
import views.admin.programs.ProgramNewOneView;

/** Controller for handling methods for admins managing program definitions. */
public class AdminProgramController extends CiviFormController {

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

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result index() {
    return ok(listView.render(this.service.listProgramDefinitions()));
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result newOne(Request request) {
    return ok(newOneView.render(request));
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result create(Request request) {
    Form<ProgramForm> programForm = formFactory.form(ProgramForm.class);
    ProgramForm program = programForm.bindFromRequest(request).get();
    ErrorAnd<ProgramDefinition, CiviFormError> result =
        service.createProgramDefinition(program.getName(), program.getDescription());
    if (result.isError()) {
      String errorMessage = joinErrors(result.getErrors());
      return ok(newOneView.render(request, program, errorMessage));
    }
    return redirect(routes.AdminProgramController.index().url());
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result edit(Request request, long id) {
    try {
      ProgramDefinition program = service.getProgramDefinition(id);
      return ok(editView.render(request, program));
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    }
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result update(Request request, long id) {
    Form<ProgramForm> programForm = formFactory.form(ProgramForm.class);
    ProgramForm program = programForm.bindFromRequest(request).get();
    try {
      ErrorAnd<ProgramDefinition, CiviFormError> result =
          service.updateProgramDefinition(id, program.getName(), program.getDescription());
      if (result.isError()) {
        String errorMessage = joinErrors(result.getErrors());
        return ok(editView.render(request, id, program, errorMessage));
      }
      return redirect(routes.AdminProgramController.index().url());
    } catch (ProgramNotFoundException e) {
      return notFound(String.format("Program ID %d not found.", id));
    }
  }
}
