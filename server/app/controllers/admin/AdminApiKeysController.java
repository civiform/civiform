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
import repository.VersionRepository;
import services.PageNumberBasedPaginationSpec;
import services.apikey.ApiKeyCreationResult;
import services.apikey.ApiKeyService;
import services.program.ProgramService;
import views.admin.apikeys.ApiKeyIndexView;
import views.admin.apikeys.ApiKeyNewOneView;
import views.admin.programs.ApiKeyCredentialsView;

/** Controller for admins managing ApiKeys. */
public class AdminApiKeysController extends CiviFormController {

  private final ApiKeyService apiKeyService;
  private final ApiKeyIndexView indexView;
  private final ApiKeyNewOneView newOneView;
  private final ApiKeyCredentialsView apiKeyCredentialsView;
  private final ProgramService programService;
  private final FormFactory formFactory;

  @Inject
  public AdminApiKeysController(
      ApiKeyService apiKeyService,
      ApiKeyIndexView indexView,
      ApiKeyNewOneView newOneView,
      ApiKeyCredentialsView apiKeyCredentialsView,
      ProgramService programService,
      FormFactory formFactory,
      ProfileUtils profileUtils,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.apiKeyService = checkNotNull(apiKeyService);
    this.indexView = checkNotNull(indexView);
    this.newOneView = checkNotNull(newOneView);
    this.apiKeyCredentialsView = checkNotNull(apiKeyCredentialsView);
    this.programService = checkNotNull(programService);
    this.formFactory = checkNotNull(formFactory);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request) {
    return ok(
        indexView.render(
            request,
            // The backend service supports pagination but the front end doesn't
            // in its initial implementation so we load all of them here.
            apiKeyService.listApiKeys(PageNumberBasedPaginationSpec.MAX_PAGE_SIZE_SPEC),
            programService.getAllProgramNames()));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result retire(Http.Request request, Long apiKeyId) {
    Optional<CiviFormProfile> profile = profileUtils.currentUserProfile(request);

    if (profile.isEmpty()) {
      throw new RuntimeException("Unable to resolve profile.");
    }

    apiKeyService.retireApiKey(apiKeyId, profile.get());

    return redirect(routes.AdminApiKeysController.index().url());
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result newOne(Http.Request request) {
    return ok(newOneView.render(request, programService.getActiveProgramNames()));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result create(Http.Request request) {
    Optional<CiviFormProfile> profile = profileUtils.currentUserProfile(request);

    if (profile.isEmpty()) {
      throw new RuntimeException("Unable to resolve profile.");
    }

    DynamicForm form = formFactory.form().bindFromRequest(request);
    ApiKeyCreationResult result = apiKeyService.createApiKey(form, profile.get());

    if (result.isSuccessful()) {
      return created(
          apiKeyCredentialsView.render(
              result.getApiKey(),
              result.getEncodedCredentials(),
              result.getKeyId(),
              result.getKeySecret()));
    }

    return badRequest(
        newOneView.render(
            request, programService.getActiveProgramNames(), Optional.of(result.getForm())));
  }
}
