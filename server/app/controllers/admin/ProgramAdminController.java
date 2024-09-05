package controllers.admin;

import auth.Authorizers;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import controllers.CiviFormController;
import java.util.Optional;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramService;
import views.admin.programs.ProgramAdministratorProgramListView;

/** Controller for program admins to view programs. */
public class ProgramAdminController extends CiviFormController {
  private final ProgramAdministratorProgramListView listView;
  private final ProgramService programService;

  @Inject
  public ProgramAdminController(
      ProgramAdministratorProgramListView listView,
      ProgramService programService,
      ProfileUtils profileUtils,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.listView = Preconditions.checkNotNull(listView);
    this.programService = Preconditions.checkNotNull(programService);
  }

  /** Return a HTML page showing all programs the program admin administers. */
  @Secure(authorizers = Authorizers.Labels.PROGRAM_ADMIN)
  public Result index(Http.Request request) {
    CiviFormProfile profile = profileUtils.currentUserProfile(request);

    ImmutableList<String> administeredPrograms =
        profile.getAccount().join().getAdministeredProgramNames();
    ActiveAndDraftPrograms activeAndDraftPrograms =
        this.programService.getActiveAndDraftProgramsWithoutQuestionLoad();

    return ok(
        listView.render(
            request, activeAndDraftPrograms, administeredPrograms, Optional.of(profile)));
  }
}
