package controllers.admin;

import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import controllers.CiviFormController;
import java.util.Optional;
import javax.inject.Inject;
import mapping.admin.programs.ProgramAdminIndexPageMapper;
import org.pac4j.play.java.Secure;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.DateConverter;
import services.cloud.PublicStorageClient;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import views.admin.programs.ProgramAdminIndexPageView;
import views.admin.programs.ProgramAdminIndexPageViewModel;
import views.admin.programs.ProgramAdministratorProgramListView;

/** Controller for program admins to view programs. */
public class ProgramAdminController extends CiviFormController {
  private final ProgramAdministratorProgramListView listView;
  private final ProgramAdminIndexPageView indexPageView;
  private final ProgramService programService;
  private final SettingsManifest settingsManifest;
  private final DateConverter dateConverter;
  private final PublicStorageClient publicStorageClient;
  private final String baseUrl;

  @Inject
  public ProgramAdminController(
      ProgramAdministratorProgramListView listView,
      ProgramAdminIndexPageView indexPageView,
      ProgramService programService,
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      SettingsManifest settingsManifest,
      DateConverter dateConverter,
      PublicStorageClient publicStorageClient,
      Config config) {
    super(profileUtils, versionRepository);
    this.listView = Preconditions.checkNotNull(listView);
    this.indexPageView = Preconditions.checkNotNull(indexPageView);
    this.programService = Preconditions.checkNotNull(programService);
    this.settingsManifest = Preconditions.checkNotNull(settingsManifest);
    this.dateConverter = Preconditions.checkNotNull(dateConverter);
    this.publicStorageClient = Preconditions.checkNotNull(publicStorageClient);
    this.baseUrl = Preconditions.checkNotNull(config).getString("base_url");
  }

  /** Return a HTML page showing all programs the program admin administers. */
  @Secure(authorizers = Authorizers.Labels.PROGRAM_ADMIN)
  public Result index(Http.Request request) {
    CiviFormProfile profile = profileUtils.currentUserProfile(request);

    ImmutableList<String> administeredPrograms =
        profile.getAccount().join().getAdministeredProgramNames();
    ActiveAndDraftPrograms activeAndDraftPrograms =
        this.programService.getActiveAndDraftProgramsWithoutQuestionLoad();

    if (settingsManifest.getAdminUiMigrationJ2htmlToThymeleafScEnabled(request)) {
      ProgramAdminIndexPageViewModel model =
          new ProgramAdminIndexPageMapper()
              .map(
                  activeAndDraftPrograms.getActivePrograms(),
                  administeredPrograms,
                  profile.isCiviFormAdmin(),
                  baseUrl,
                  dateConverter,
                  publicStorageClient);
      return ok(indexPageView.render(request, model)).as(Http.MimeTypes.HTML);
    }

    return ok(
        listView.render(
            request, activeAndDraftPrograms, administeredPrograms, Optional.of(profile)));
  }
}
