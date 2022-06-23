package controllers.admin;

import static annotations.FeatureFlags.ApplicationStatusTrackingEnabled;

import auth.Authorizers;
import com.google.inject.Inject;
import controllers.CiviFormController;
import org.pac4j.play.java.Secure;
import play.mvc.Http;
import play.mvc.Result;
import views.admin.programs.ProgramStatusesView;

public final class AdminProgramStatusesController extends CiviFormController {

  private final ProgramStatusesView statusesView;
  private final boolean statusTrackingEnabled;

  @Inject
  public AdminProgramStatusesController(
      ProgramStatusesView statusesView,
      @ApplicationStatusTrackingEnabled boolean statusTrackingEnabled) {
    this.statusesView = statusesView;
    this.statusTrackingEnabled = statusTrackingEnabled;
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request, long programId) {
    if (!statusTrackingEnabled) {
      return notFound("status tracking is not enabled");
    }
    return ok(statusesView.render());
  }
}
