package controllers.admin;

import static annotations.FeatureFlags.ApplicationStatusTrackingEnabled;
import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import com.google.inject.Inject;
import controllers.CiviFormController;
import org.pac4j.play.java.Secure;
import play.mvc.Http;
import play.mvc.Result;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import views.admin.programs.ProgramStatusesView;

public final class AdminProgramStatusesController extends CiviFormController {

  private final ProgramService service;
  private final ProgramStatusesView statusesView;
  private final boolean statusTrackingEnabled;

  @Inject
  public AdminProgramStatusesController(
      ProgramService service,
      ProgramStatusesView statusesView,
      @ApplicationStatusTrackingEnabled boolean statusTrackingEnabled) {
    this.service = checkNotNull(service);
    this.statusesView = checkNotNull(statusesView);
    this.statusTrackingEnabled = statusTrackingEnabled;
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request, long programId) throws ProgramNotFoundException {
    if (!statusTrackingEnabled) {
      return notFound("status tracking is not enabled");
    }
    return ok(statusesView.render(service.getProgramDefinition(programId)));
  }
}
