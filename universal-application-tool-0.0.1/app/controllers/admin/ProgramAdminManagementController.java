package controllers.admin;

import static play.mvc.Results.badRequest;
import static play.mvc.Results.notFound;
import static play.mvc.Results.ok;
import static play.mvc.Results.redirect;

import auth.Authorizers;
import com.google.common.collect.ImmutableSet;
import forms.AddProgramAdminForm;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import services.CiviFormError;
import services.program.ProgramNotFoundException;
import services.role.RoleService;

public class ProgramAdminManagementController {

  private final RoleService roleService;
  private final FormFactory formFactory;

  @Inject
  public ProgramAdminManagementController(RoleService roleService, FormFactory formFactory) {
    this.roleService = roleService;
    this.formFactory = formFactory;
  }

  /** Displays a form for managing program admins of a given program. */
  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result edit(long programId) {
    // TODO: Return a view for editing program admin rights.
    return ok();
  }

  /**
   * Promotes the given account emails to the role of PROGRAM_ADMIN. If there are errors, return a
   * redirect with flashing error message.
   */
  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result update(Http.Request request, long programId) {
    Form<AddProgramAdminForm> form = formFactory.form(AddProgramAdminForm.class);
    if (form.hasErrors()) {
      return badRequest();
    }
    AddProgramAdminForm addAdminForm = form.bindFromRequest(request).get();

    try {
      Optional<CiviFormError> maybeError =
          roleService.makeProgramAdmins(
              programId, ImmutableSet.copyOf(addAdminForm.getAdminEmails()));
      Result result = redirect(routes.AdminProgramController.index());
      if (maybeError.isPresent()) {
        return result.flashing("error", maybeError.get().message());
      }
      return result;
    } catch (ProgramNotFoundException e) {
      return notFound(e.getLocalizedMessage());
    }
  }
}
