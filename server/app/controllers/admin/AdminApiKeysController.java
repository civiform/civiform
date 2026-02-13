package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableSet;
import controllers.CiviFormController;
import java.util.Optional;
import javax.inject.Inject;
import mapping.admin.apikeys.ApiKeyCredentialsPageMapper;
import mapping.admin.apikeys.ApiKeyIndexPageMapper;
import mapping.admin.apikeys.ApiKeyNewOnePageMapper;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.Form;
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
import views.admin.apikeys.ApiKeyCredentialsView;
import views.admin.apikeys.ApiKeyIndexPageView;
import views.admin.apikeys.ApiKeyIndexView;
import views.admin.apikeys.ApiKeyNewOnePageView;
import views.admin.apikeys.ApiKeyNewOneView;
import views.admin.apikeys.CreateApiKeyCommand;

/** Controller for admins managing ApiKeys. */
public class AdminApiKeysController extends CiviFormController {

  private final ApiKeyService apiKeyService;
  private final ApiKeyIndexView indexView;
  private final ApiKeyIndexPageView apiKeyIndexPageView;
  private final ApiKeyNewOneView newOneView;
  private final ApiKeyNewOnePageView apiKeyNewOnePageView;
  private final ApiKeyCredentialsView apiKeyCredentialsView;
  private final ApiKeyCredentialsPageView apiKeyCredentialsPageView;
  private final ProgramService programService;
  private final FormFactory formFactory;
  private final ProgramRepository programRepository;
  private final SettingsManifest settingsManifest;
  private final DateConverter dateConverter;

  @Inject
  public AdminApiKeysController(
      ApiKeyService apiKeyService,
      ApiKeyIndexView indexView,
      ApiKeyIndexPageView apiKeyIndexPageView,
      ApiKeyNewOneView newOneView,
      ApiKeyNewOnePageView apiKeyNewOnePageView,
      ApiKeyCredentialsView apiKeyCredentialsView,
      ApiKeyCredentialsPageView apiKeyCredentialsPageView,
      ProgramService programService,
      FormFactory formFactory,
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      ProgramRepository programRepository,
      SettingsManifest settingsManifest,
      DateConverter dateConverter) {
    super(profileUtils, versionRepository);
    this.apiKeyService = checkNotNull(apiKeyService);
    this.indexView = checkNotNull(indexView);
    this.apiKeyIndexPageView = checkNotNull(apiKeyIndexPageView);
    this.newOneView = checkNotNull(newOneView);
    this.apiKeyNewOnePageView = checkNotNull(apiKeyNewOnePageView);
    this.apiKeyCredentialsView = checkNotNull(apiKeyCredentialsView);
    this.apiKeyCredentialsPageView = checkNotNull(apiKeyCredentialsPageView);
    this.programService = checkNotNull(programService);
    this.formFactory = checkNotNull(formFactory);
    this.programRepository = checkNotNull(programRepository);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.dateConverter = checkNotNull(dateConverter);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request) {
    if (settingsManifest.getAdminUiMigrationScEnabled(request)) {
      ApiKeyIndexPageMapper mapper = new ApiKeyIndexPageMapper();
      return ok(apiKeyIndexPageView.render(
              request,
              mapper.map(
                  "Active",
                  apiKeyService.listActiveApiKeys(),
                  programService.getAllProgramNames(),
                  dateConverter)))
          .as(Http.MimeTypes.HTML);
    }
    return ok(
        indexView.render(
            request,
            /* selectedStatus= */ "Active",
            apiKeyService.listActiveApiKeys(),
            programService.getAllProgramNames()));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result indexRetired(Http.Request request) {
    if (settingsManifest.getAdminUiMigrationScEnabled(request)) {
      ApiKeyIndexPageMapper mapper = new ApiKeyIndexPageMapper();
      return ok(apiKeyIndexPageView.render(
              request,
              mapper.map(
                  "Retired",
                  apiKeyService.listRetiredApiKeys(),
                  programService.getAllProgramNames(),
                  dateConverter)))
          .as(Http.MimeTypes.HTML);
    }
    return ok(
        indexView.render(
            request,
            /* selectedStatus= */ "Retired",
            apiKeyService.listRetiredApiKeys(),
            programService.getAllProgramNames()));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result indexExpired(Http.Request request) {
    if (settingsManifest.getAdminUiMigrationScEnabled(request)) {
      ApiKeyIndexPageMapper mapper = new ApiKeyIndexPageMapper();
      return ok(apiKeyIndexPageView.render(
              request,
              mapper.map(
                  "Expired",
                  apiKeyService.listExpiredApiKeys(),
                  programService.getAllProgramNames(),
                  dateConverter)))
          .as(Http.MimeTypes.HTML);
    }
    return ok(
        indexView.render(
            request,
            /* selectedStatus= */ "Expired",
            apiKeyService.listExpiredApiKeys(),
            programService.getAllProgramNames()));
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

    if (settingsManifest.getAdminUiMigrationScEnabled(request)) {
      ApiKeyNewOnePageMapper mapper = new ApiKeyNewOnePageMapper();
      return ok(apiKeyNewOnePageView.render(request, mapper.map(programNames, Optional.empty())))
          .as(Http.MimeTypes.HTML);
    }

    if (programNames.isEmpty()) {
      return ok(newOneView.renderNoPrograms(request));
    } else {
      return ok(newOneView.render(request, programNames));
    }
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result create(Http.Request request) {

    if (settingsManifest.getAdminUiMigrationScEnabled(request)) {
      CiviFormProfile profile = profileUtils.currentUserProfile(request);
      DynamicForm form = formFactory.form().bindFromRequest(request);
      ApiKeyCreationResult result = apiKeyService.createApiKey(form, profile);
      Form<CreateApiKeyCommand> form1 =
          formFactory.form(CreateApiKeyCommand.class).bindFromRequest(request);

      if (form1.hasErrors()) {
        throw new RuntimeException("ERROR");
      }

      if (result.isSuccessful()) {
        ApiKeyCredentialsPageMapper credMapper = new ApiKeyCredentialsPageMapper();
        return created(
                apiKeyCredentialsPageView.render(
                    request,
                    credMapper.map(
                        result.getApiKey().getName(),
                        result.getEncodedCredentials(),
                        result.getKeyId(),
                        result.getKeySecret())))
            .as(Http.MimeTypes.HTML);
      }

      ApiKeyNewOnePageMapper newOneMapper = new ApiKeyNewOnePageMapper();
      return badRequest(
              apiKeyNewOnePageView.render(
                  request,
                  newOneMapper.map(
                      programService.getAllNonExternalProgramNames(),
                      Optional.of(result.getForm()))))
          .as(Http.MimeTypes.HTML);
    }

    // old
    CiviFormProfile profile = profileUtils.currentUserProfile(request);
    DynamicForm form = formFactory.form().bindFromRequest(request);
    ApiKeyCreationResult result = apiKeyService.createApiKey(form, profile);
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
            request,
            programService.getAllNonExternalProgramNames(),
            Optional.of(result.getForm())));
  }
}
