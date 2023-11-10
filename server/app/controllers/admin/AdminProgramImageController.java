package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import controllers.CiviFormController;
import javax.inject.Inject;
import org.pac4j.play.java.Secure;
import play.mvc.Http;
import play.mvc.Result;
import repository.VersionRepository;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.admin.programs.ProgramImageView;

/** Controller for displaying and modifying the image (and alt text) associated with a program. */
public final class AdminProgramImageController extends CiviFormController {
  private final ProgramService programService;
  private final ProgramImageView programImageView;
  private final RequestChecker requestChecker;

  @Inject
  public AdminProgramImageController(
      ProgramService programService,
      ProgramImageView programImageView,
      RequestChecker requestChecker,
      ProfileUtils profileUtils,
      VersionRepository versionRepository) {
    super(profileUtils, versionRepository);
    this.programService = checkNotNull(programService);
    this.programImageView = checkNotNull(programImageView);
    this.requestChecker = checkNotNull(requestChecker);
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request, long programId) throws ProgramNotFoundException {
    requestChecker.throwIfProgramNotDraft(programId);
    return ok(programImageView.render(request, programService.getProgramDefinition(programId)));
  }
}
