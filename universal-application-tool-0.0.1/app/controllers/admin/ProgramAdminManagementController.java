package controllers.admin;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static play.mvc.Results.badRequest;
import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;
import static play.mvc.Results.redirect;

import auth.Authorizers;
import forms.AddProgramAdminForm;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import repository.ProgramRepository;
import repository.UserRepository;

public class ProgramAdminManagementController {

  private final UserRepository userRepository;
  private final ProgramRepository programRepository;
  private final FormFactory formFactory;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public ProgramAdminManagementController(
      UserRepository userRepository,
      ProgramRepository programRepository,
      FormFactory formFactory,
      HttpExecutionContext httpExecutionContext) {
    this.userRepository = userRepository;
    this.programRepository = programRepository;
    this.formFactory = formFactory;
    this.httpExecutionContext = httpExecutionContext;
  }

  /** Displays a form for managing program admins of a given program. */
  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result edit(long programId) {
    return ok();
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public CompletionStage<Result> update(Http.Request request, long programId) {
    Form<AddProgramAdminForm> form = formFactory.form(AddProgramAdminForm.class);
    if (form.hasErrors()) {
      return supplyAsync(() -> badRequest());
    }
    AddProgramAdminForm addAdminForm = form.bindFromRequest(request).get();

    return programRepository
        .lookupProgram(programId)
        .thenApplyAsync(
            program -> {
              if (program.isEmpty()) {
                return notFound(String.format("No program with ID %d was found", programId));
              }
              addAdminForm
                  .getAdminEmails()
                  .forEach(
                      email ->
                          userRepository.addAdministeredProgram(
                              email, program.get().getProgramDefinition()));
              return redirect(routes.AdminProgramController.index());
            },
            httpExecutionContext.current());
  }
}
