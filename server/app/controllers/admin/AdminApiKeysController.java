package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import controllers.CiviFormController;
import java.util.Optional;
import javax.inject.Inject;
import mapping.admin.apikeys.ApiKeyCredentialsPageMapper;
import mapping.admin.apikeys.ApiKeyIndexPageMapper;
import mapping.admin.apikeys.ApiKeyNewOnePageMapper;
import mapping.admin.apikeys.ApiKeyStatus;
import models.ApiKeyModel;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import repository.ProgramRepository;
import repository.VersionRepository;
import services.DateConverter;
import services.apikey.ApiKeyCreationResult;
import services.apikey.ApiKeyService;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import views.admin.apikeys.ApiKeyCredentialsPageView;
import views.admin.apikeys.ApiKeyCredentialsPageViewModel;
import views.admin.apikeys.ApiKeyCredentialsView;
import views.admin.apikeys.ApiKeyIndexPageView;
import views.admin.apikeys.ApiKeyIndexPageViewModel;
import views.admin.apikeys.ApiKeyIndexView;
import views.admin.apikeys.ApiKeyNewOnePageView;
import views.admin.apikeys.ApiKeyNewOnePageViewModel;
import views.admin.apikeys.ApiKeyNewOneView;

/** Controller for admins managing ApiKeys. */
public class AdminApiKeysController extends CiviFormController {

  private final ApiKeyService apiKeyService;
  private final ApiKeyIndexView indexView;
  private final ApiKeyNewOneView newOneView;
  private final ApiKeyCredentialsView apiKeyCredentialsView;
  private final ApiKeyIndexPageView indexPageView;
  private final ApiKeyNewOnePageView newOnePageView;
  private final ApiKeyCredentialsPageView credentialsPageView;
  private final ProgramService programService;
  private final FormFactory formFactory;
  private final ProgramRepository programRepository;
  private final DateConverter dateConverter;
  private final SettingsManifest settingsManifest;

  @Inject
  public AdminApiKeysController(
      ApiKeyService apiKeyService,
      ApiKeyIndexView indexView,
      ApiKeyNewOneView newOneView,
      ApiKeyCredentialsView apiKeyCredentialsView,
      ApiKeyIndexPageView indexPageView,
      ApiKeyNewOnePageView newOnePageView,
      ApiKeyCredentialsPageView credentialsPageView,
      ProgramService programService,
      FormFactory formFactory,
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      ProgramRepository programRepository,
      DateConverter dateConverter,
      SettingsManifest settingsManifest) {
    super(profileUtils, versionRepository);
    this.apiKeyService = checkNotNull(apiKeyService);
    this.indexView = checkNotNull(indexView);
    this.newOneView = checkNotNull(newOneView);
    this.apiKeyCredentialsView = checkNotNull(apiKeyCredentialsView);
    this.indexPageView = checkNotNull(indexPageView);
    this.newOnePageView = checkNotNull(newOnePageView);
    this.credentialsPageView = checkNotNull(credentialsPageView);
    this.programService = checkNotNull(programService);
    this.formFactory = checkNotNull(formFactory);
    this.programRepository = checkNotNull(programRepository);
    this.dateConverter = checkNotNull(dateConverter);
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request) {
    return renderIndex(request, ApiKeyStatus.ACTIVE, apiKeyService.listActiveApiKeys());
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result indexRetired(Http.Request request) {
    return renderIndex(request, ApiKeyStatus.RETIRED, apiKeyService.listRetiredApiKeys());
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result indexExpired(Http.Request request) {
    return renderIndex(request, ApiKeyStatus.EXPIRED, apiKeyService.listExpiredApiKeys());
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result retire(Http.Request request, Long apiKeyId) {
    CiviFormProfile profile = profileUtils.currentUserProfile(request);

    apiKeyService.retireApiKey(apiKeyId, profile);

    return redirect(routes.AdminApiKeysController.index().url());
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result newOne(Http.Request request) {
    ImmutableSet<String> programNames = programRepository.getAllNonExternalProgramNames();

    if (settingsManifest.getAdminUiMigrationJ2htmlToThymeleafScEnabled(request)) {
      ApiKeyNewOnePageViewModel model =
          new ApiKeyNewOnePageMapper().map(programNames, /* maybeForm= */ Optional.empty());
      return ok(newOnePageView.render(request, model)).as(Http.MimeTypes.HTML);
    }

    if (programNames.isEmpty()) {
      return ok(newOneView.renderNoPrograms(request));
    } else {
      return ok(newOneView.render(request, programNames));
    }
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result create(Http.Request request) {
    CiviFormProfile profile = profileUtils.currentUserProfile(request);

    DynamicForm form = formFactory.form().bindFromRequest(request);
    ApiKeyCreationResult result = apiKeyService.createApiKey(form, profile);

    if (result.isSuccessful()) {
      if (settingsManifest.getAdminUiMigrationJ2htmlToThymeleafScEnabled(request)) {
        ApiKeyCredentialsPageViewModel model =
            new ApiKeyCredentialsPageMapper()
                .map(
                    result.getApiKey(),
                    result.getEncodedCredentials(),
                    result.getKeyId(),
                    result.getKeySecret());
        return created(credentialsPageView.render(request, model)).as(Http.MimeTypes.HTML);
      }

      return created(
          apiKeyCredentialsView.render(
              request,
              result.getApiKey(),
              result.getEncodedCredentials(),
              result.getKeyId(),
              result.getKeySecret()));
    }

    if (settingsManifest.getAdminUiMigrationJ2htmlToThymeleafScEnabled(request)) {
      ApiKeyNewOnePageViewModel model =
          new ApiKeyNewOnePageMapper()
              .map(programService.getAllNonExternalProgramNames(), Optional.of(result.getForm()));
      return badRequest(newOnePageView.render(request, model)).as(Http.MimeTypes.HTML);
    }

    return badRequest(
        newOneView.render(
            request,
            programService.getAllNonExternalProgramNames(),
            Optional.of(result.getForm())));
  }

  private Result renderIndex(
      Http.Request request, ApiKeyStatus selectedStatus, ImmutableList<ApiKeyModel> apiKeys) {
    ImmutableSet<String> allProgramNames = programService.getAllProgramNames();

    if (settingsManifest.getAdminUiMigrationJ2htmlToThymeleafScEnabled(request)) {
      ApiKeyIndexPageViewModel model =
          new ApiKeyIndexPageMapper().map(selectedStatus, apiKeys, allProgramNames, dateConverter);
      return ok(indexPageView.render(request, model)).as(Http.MimeTypes.HTML);
    }

    return ok(indexView.render(request, selectedStatus.displayName(), apiKeys, allProgramNames));
  }
}
