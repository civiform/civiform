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
import mapping.admin.programs.ProgramAdminListPageMapper;
import org.pac4j.play.java.Secure;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramService;
import services.settings.SettingsManifest;
import views.admin.programs.ProgramAdminListPageView;
import views.admin.programs.ProgramAdminListPageViewModel;
import views.admin.programs.ProgramAdministratorProgramListView;

/** Controller for program admins to view programs. */
public class ProgramAdminController extends CiviFormController {
  private final ProgramAdministratorProgramListView listView;
  private final ProgramAdminListPageView adminListPageView;
  private final ProgramService programService;
  private final SettingsManifest settingsManifest;
  private final String baseUrl;

  @Inject
  public ProgramAdminController(
      ProgramAdministratorProgramListView listView,
      ProgramAdminListPageView adminListPageView,
      ProgramService programService,
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      SettingsManifest settingsManifest,
      Config config) {
    super(profileUtils, versionRepository);
    this.listView = Preconditions.checkNotNull(listView);
    this.adminListPageView = Preconditions.checkNotNull(adminListPageView);
    this.programService = Preconditions.checkNotNull(programService);
    this.settingsManifest = Preconditions.checkNotNull(settingsManifest);
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

    if (settingsManifest.getAdminUiMigrationScEnabled(request)) {
      ProgramAdminListPageViewModel model =
          new ProgramAdminListPageMapper()
              .map(activeAndDraftPrograms, administeredPrograms, baseUrl);
      return ok(adminListPageView.render(request, model)).as(Http.MimeTypes.HTML);
    }

    return ok(
        listView.render(
            request, activeAndDraftPrograms, administeredPrograms, Optional.of(profile)));
  }
}
