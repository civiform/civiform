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
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramService;
import views.admin.programs.ProgramAdministratorProgramListView;

/** Controller for program admins to view programs. */
public class ProgramAdminController extends CiviFormController {
  private final ProgramAdministratorProgramListView listView;
  private final ProgramService programService;
  private final ProfileUtils profileUtils;

  @Inject
  public ProgramAdminController(
      ProgramAdministratorProgramListView listView,
      ProgramService programService,
      ProfileUtils profileUtils) {
    this.listView = Preconditions.checkNotNull(listView);
    this.programService = Preconditions.checkNotNull(programService);
    this.profileUtils = Preconditions.checkNotNull(profileUtils);
  }

  /** Return a HTML page showing all programs the program admin administers. */
  @Secure(authorizers = Authorizers.Labels.PROGRAM_ADMIN)
  public Result index(Http.Request request) {
    Optional<CiviFormProfile> profile = profileUtils.currentUserProfile(request);

    if (!profile.isPresent()) {
      throw new RuntimeException("No profile found for program admin");
    }

    ImmutableList<String> administeredPrograms =
        profile.get().getAccount().join().getAdministeredProgramNames();
    ActiveAndDraftPrograms activeAndDraftPrograms = this.programService.getActiveAndDraftPrograms();

    return ok(listView.render(activeAndDraftPrograms, administeredPrograms, profile));
  }
}
