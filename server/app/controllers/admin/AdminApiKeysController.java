package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import controllers.CiviFormController;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import services.apikey.ApiKeyService;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.admin.programs.ApiKeyNewOneView;

/** Controller for admins managing ApiKeys. */
public class AdminApiKeysController extends CiviFormController {

  private final ApiKeyService apiKeyService;
  private final ApiKeyNewOneView newOneView;
  private final ProgramService programService;
  private final FormFactory formFactory;
  private final ProfileUtils profileUtils;

  @Inject
  public AdminApiKeysController(
      ApiKeyNewOneView newOneView,
      ProgramService programService,
      FormFactory formFactory,
      ApiKeyService apiKeyService,
      ProfileUtils profileUtils) {
    this.apiKeyService = checkNotNull(apiKeyService);
    this.newOneView = checkNotNull(newOneView);
    this.programService = checkNotNull(programService);
    this.formFactory = checkNotNull(formFactory);
    this.profileUtils = checkNotNull(profileUtils);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index() {
    return ok();
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result newOne(Http.Request request) {
    return ok(newOneView.render(request, formFactory.form(), programService.getAllProgramNames()));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result create(Http.Request request) {
    Optional<CiviFormProfile> profile = profileUtils.currentUserProfile(request);

    if (!profile.isPresent()) {
      throw new RuntimeException("Unable to resolve profile.");
    }

    DynamicForm form = formFactory.form().bindFromRequest(request);

    try {
      apiKeyService.createApiKey(form, profile.get());
    } catch (ProgramNotFoundException e) {
      return notFound(e.toString());
    }

    if (form.hasErrors()) {
      return badRequest();
      // return badRequest(newOneView.render(request, form, programService.getAllProgramNames()));
    }

    // render the post-create view with the result object
    return ok();
  }
}
