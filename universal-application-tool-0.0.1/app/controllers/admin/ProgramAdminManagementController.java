package controllers.admin;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static play.mvc.Results.badRequest;
import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;
import static play.mvc.Results.redirect;

import auth.Authorizers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import forms.ManageProgramAdminsForm;
import java.util.Optional;
import javax.inject.Inject;
import models.Account;
import models.Program;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import repository.ProgramRepository;
import services.CiviFormError;
import services.program.ProgramNotFoundException;
import services.role.RoleService;
import views.admin.programs.ManageProgramAdminsView;

/** Controller for admins to manage program admins of programs. */
public class ProgramAdminManagementController {

  private final ManageProgramAdminsView manageAdminsView;
  private final ProgramRepository programRepository;
  private final RoleService roleService;
  private final FormFactory formFactory;

  @Inject
  public ProgramAdminManagementController(
      ManageProgramAdminsView manageAdminsView,
      ProgramRepository programRepository,
      RoleService roleService,
      FormFactory formFactory) {
    this.manageAdminsView = manageAdminsView;
    this.programRepository = programRepository;
    this.roleService = roleService;
    this.formFactory = formFactory;
  }

  /** Displays a form for managing program admins of a given program. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result edit(Http.Request request, long programId) {
    Optional<Program> program =
        programRepository.lookupProgram(programId).toCompletableFuture().join();
    if (program.isEmpty()) {
      return notFound(String.format("Program with ID %s was not found", programId));
    }

    try {
      ImmutableList<String> programAdmins =
          programRepository.getProgramAdministrators(programId).stream()
              .map(Account::getEmailAddress)
              .collect(toImmutableList());
      return ok(
          manageAdminsView.render(
              request, program.get().getProgramDefinition(), programAdmins, ""));
    } catch (ProgramNotFoundException e) {
      return notFound(e.getLocalizedMessage());
    }
  }

  /**
   * Promotes the given account emails to the role of PROGRAM_ADMIN. If there are errors, return a
   * redirect with flashing error message.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result update(Http.Request request, long programId) {
    Form<ManageProgramAdminsForm> form = formFactory.form(ManageProgramAdminsForm.class);
    if (form.hasErrors()) {
      return badRequest();
    }
    ManageProgramAdminsForm manageAdminForm = form.bindFromRequest(request).get();

    try {
      // Remove first, in case the admin accidentally removed an admin and then re-added them.
      roleService.removeProgramAdmins(
          programId, ImmutableSet.copyOf(manageAdminForm.getRemoveAdminEmails()));
      Optional<CiviFormError> maybeError =
          roleService.makeProgramAdmins(
              programId, ImmutableSet.copyOf(manageAdminForm.getAdminEmails()));
      Result result = redirect(routes.AdminProgramController.index());

      if(maybeError.isPresent()){   
        Optional<Program> program =
            programRepository.lookupProgram(programId).toCompletableFuture().join();
        if (program.isEmpty()) {
          return notFound(String.format("Program with ID %s was not found", programId));
        } else{
          ImmutableList<String> programAdmins =
              programRepository.getProgramAdministrators(programId).stream()
                  .map(Account::getEmailAddress)
                  .collect(toImmutableList());
          return ok(
            manageAdminsView.render(
                request,
                program.get().getProgramDefinition(),
                programAdmins,
                maybeError.get().message()));
        }     
      }
      return result;
    } catch (ProgramNotFoundException e) {
      return notFound(e.getLocalizedMessage());
    }
  }
}
