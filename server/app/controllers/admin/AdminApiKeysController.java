package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import annotations.BindingAnnotations.EnUsLang;
import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableSet;
import controllers.CiviFormController;
import java.util.Optional;
import javax.inject.Inject;
import mapping.admin.apikeys.ApiKeyCredentialsPageMapper;
import mapping.admin.apikeys.ApiKeyNewOnePageMapper;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.i18n.Messages;
import play.mvc.Http;
import play.mvc.Result;
import repository.ProgramRepository;
import repository.VersionRepository;
import services.apikey.ApiKeyCreationResult;
import services.apikey.ApiKeyService;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import views.admin.apikeys.ApiKeyCredentialsPageView;
import views.admin.apikeys.ApiKeyCredentialsPageViewModel;
import views.admin.apikeys.ApiKeyCredentialsView;
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
  private final ApiKeyNewOnePageView newOnePageView;
  private final ApiKeyCredentialsPageView credentialsPageView;
  private final ProgramService programService;
  private final FormFactory formFactory;
  private final ProgramRepository programRepository;
  private final SettingsManifest settingsManifest;
  private final Messages enUsMessages;

  @Inject
  public AdminApiKeysController(
      ApiKeyService apiKeyService,
      ApiKeyIndexView indexView,
      ApiKeyNewOneView newOneView,
      ApiKeyCredentialsView apiKeyCredentialsView,
      ApiKeyNewOnePageView newOnePageView,
      ApiKeyCredentialsPageView credentialsPageView,
      ProgramService programService,
      FormFactory formFactory,
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      ProgramRepository programRepository,
      SettingsManifest settingsManifest,
      @EnUsLang Messages enUsMessages) {
    super(profileUtils, versionRepository);
    this.apiKeyService = checkNotNull(apiKeyService);
    this.indexView = checkNotNull(indexView);
    this.newOneView = checkNotNull(newOneView);
    this.apiKeyCredentialsView = checkNotNull(apiKeyCredentialsView);
    this.newOnePageView = checkNotNull(newOnePageView);
    this.credentialsPageView = checkNotNull(credentialsPageView);
    this.programService = checkNotNull(programService);
    this.formFactory = checkNotNull(formFactory);
    this.programRepository = checkNotNull(programRepository);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.enUsMessages = checkNotNull(enUsMessages);
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
    CiviFormProfile profile = profileUtils.currentUserProfile(request);

    apiKeyService.retireApiKey(apiKeyId, profile);

    return redirect(routes.AdminApiKeysController.index().url());
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result newOne(Http.Request request) {
    ImmutableSet<String> programNames = programRepository.getAllNonExternalProgramNames();

    if (settingsManifest.getAdminUiMigrationJ2htmlToThymeleafScEnabled(request)) {
      ApiKeyNewOnePageViewModel model =
          new ApiKeyNewOnePageMapper()
              .map(
                  programNames,
                  /* maybeForm= */ Optional.empty(),
                  enUsMessages,
                  /* showForm= */ !programNames.isEmpty());
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
              .map(
                  programService.getAllNonExternalProgramNames(),
                  Optional.of(result.getForm()),
                  enUsMessages,
                  /* showForm= */ true);
      return badRequest(newOnePageView.render(request, model)).as(Http.MimeTypes.HTML);
    }

    return badRequest(
        newOneView.render(
            request,
            programService.getAllNonExternalProgramNames(),
            Optional.of(result.getForm())));
  }
}
