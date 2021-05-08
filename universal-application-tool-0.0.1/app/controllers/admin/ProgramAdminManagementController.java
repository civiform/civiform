package controllers.admin;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static play.mvc.Results.badRequest;
import static play.mvc.Results.ok;
import static play.mvc.Results.redirect;

import auth.Authorizers;
import com.google.common.collect.ImmutableList;
import forms.AddProgramAdminForm;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.Form;
import play.data.FormFactory;
import play.libs.concurrent.HttpExecutionContext;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import services.role.RoleService;

public class ProgramAdminManagementController {

  private final RoleService roleService;
  private final FormFactory formFactory;
  private final HttpExecutionContext httpExecutionContext;

  @Inject
  public ProgramAdminManagementController(
      RoleService roleService, FormFactory formFactory, HttpExecutionContext httpExecutionContext) {
    this.roleService = roleService;
    this.formFactory = formFactory;
    this.httpExecutionContext = httpExecutionContext;
  }

  /** Displays a form for managing program admins of a given program. */
  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result edit(long programId) {
    return ok();
  }

  @Secure(authorizers = Authorizers.Labels.UAT_ADMIN)
  public Result update(Http.Request request, long programId) {
    Form<AddProgramAdminForm> form = formFactory.form(AddProgramAdminForm.class);
    if (form.hasErrors()) {
      return badRequest();
    }
    AddProgramAdminForm addAdminForm = form.bindFromRequest(request).get();

    roleService.makeProgramAdmins(programId, ImmutableList.copyOf(addAdminForm.getAdminEmails()));

    return redirect(routes.AdminProgramController.index());
  }
}
