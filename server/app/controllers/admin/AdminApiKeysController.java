package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableSet;
import controllers.CiviFormController;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.apikey.ApiKeyCreationResult;
import services.apikey.ApiKeyService;
import services.program.ProgramService;
import views.admin.apikeys.ApiKeyCredentialsView;
import views.admin.apikeys.ApiKeyIndexView;
import views.admin.apikeys.ApiKeyNewOneView;

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
            /* selectedStatus= */ "Active",
            apiKeyService.listActiveApiKeys(),
            programService.getAllProgramNames()));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result indexRetired(Http.Request request) {
    return ok(
        indexView.render(
            request,
            /* selectedStatus= */ "Retired",
            apiKeyService.listRetiredApiKeys(),
            programService.getAllProgramNames()));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result indexExpired(Http.Request request) {
    return ok(
        indexView.render(
            request,
            /* selectedStatus= */ "Expired",
            apiKeyService.listExpiredApiKeys(),
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
    ImmutableSet<String> programNames = programService.getActiveProgramNames();

    if (programNames.isEmpty()) {
      return ok(newOneView.renderNoPrograms(request));
    } else {
      return ok(newOneView.render(request, programNames));
    }
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
              request,
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
