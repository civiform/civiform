package controllers.admin;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;
import static play.mvc.Results.redirect;

import auth.Authorizers;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import forms.ManageProgramAdminsForm;
import java.util.Optional;
import javax.inject.Inject;
import models.AccountModel;
import models.ProgramModel;
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
import views.components.ToastMessage;

/** Controller for admins to manage program admins of programs. */
public final class ProgramAdminManagementController {

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
    return this.loadProgram(request, programId, Optional.empty());
  }

  /** Removes `adminEmail` as a program admin for the program identified by `programId`. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result delete(Http.Request request, long programId) {
    Form<ManageProgramAdminsForm> form =
        formFactory.form(ManageProgramAdminsForm.class).bindFromRequest(request);
    try {
      roleService.removeProgramAdmins(programId, ImmutableSet.of(form.get().getAdminEmail()));
      return redirect(routes.ProgramAdminManagementController.edit(programId));
    } catch (ProgramNotFoundException e) {
      return notFound(e.getLocalizedMessage());
    }
  }

  /** Adds a new admin email. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result add(Http.Request request, long programId) {
    Form<ManageProgramAdminsForm> form =
        formFactory.form(ManageProgramAdminsForm.class).bindFromRequest(request);
    String adminEmail = Strings.nullToEmpty(form.get().getAdminEmail()).trim();
    if (adminEmail.isEmpty()) {
      ToastMessage message = ToastMessage.errorNonLocalized("Enter an admin email");
      return this.loadProgram(request, programId, Optional.of(message));
    }
    try {
      Optional<CiviFormError> maybeError =
          roleService.makeProgramAdmins(programId, ImmutableSet.of(adminEmail));

      if (maybeError.isEmpty()) {
        return redirect(routes.ProgramAdminManagementController.edit(programId));
      }

      ToastMessage message = ToastMessage.errorNonLocalized(maybeError.get().message());

      return this.loadProgram(request, programId, Optional.of(message));
    } catch (ProgramNotFoundException e) {
      return notFound(e.getLocalizedMessage());
    }
  }

  /**
   * Displays a form for managing program admins of a given program. Displays a message as an error
   * toast if provided.
   */
  private Result loadProgram(Http.Request request, long programId, Optional<ToastMessage> message) {
    try {
      Optional<ProgramModel> program =
          programRepository.lookupProgram(programId).toCompletableFuture().join();

      if (program.isEmpty()) {
        return notFound(String.format("Program with ID %s was not found", programId));
      } else {
        ImmutableList<String> programAdmins =
            programRepository.getProgramAdministrators(programId).stream()
                .map(AccountModel::getEmailAddress)
                .collect(toImmutableList());

        return ok(
            manageAdminsView.render(
                request,
                programRepository.getProgramDefinition(program.get()),
                programAdmins,
                message));
      }
    } catch (ProgramNotFoundException e) {
      return notFound(e.getLocalizedMessage());
    }
  }
}
