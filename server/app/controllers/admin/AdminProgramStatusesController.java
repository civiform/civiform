package controllers.admin;

import static annotations.FeatureFlags.ApplicationStatusTrackingEnabled;
import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.CiviFormController;
import java.util.stream.Stream;
import org.pac4j.play.java.Secure;
import play.mvc.Http;
import play.mvc.Result;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.StatusDefinitions;
import views.admin.programs.ProgramStatusesView;

public final class AdminProgramStatusesController extends CiviFormController {

  private final ProgramService service;
  private final ProgramStatusesView statusesView;
  private final RequestChecker requestChecker;
  private final boolean statusTrackingEnabled;

  @Inject
  public AdminProgramStatusesController(
      ProgramService service,
      ProgramStatusesView statusesView,
      RequestChecker requestChecker,
      @ApplicationStatusTrackingEnabled boolean statusTrackingEnabled) {
    this.service = checkNotNull(service);
    this.statusesView = checkNotNull(statusesView);
    this.requestChecker = checkNotNull(requestChecker);
    this.statusTrackingEnabled = statusTrackingEnabled;
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request, long programId) throws ProgramNotFoundException {
    if (!statusTrackingEnabled) {
      return notFound("status tracking is not enabled");
    }
    requestChecker.throwIfProgramNotDraft(programId);
    return ok(statusesView.render(request, service.getProgramDefinition(programId)));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result newOne(Http.Request request, long programId) throws ProgramNotFoundException {
    if (!statusTrackingEnabled) {
      return notFound("status tracking is not enabled");
    }
    requestChecker.throwIfProgramNotDraft(programId);
    ProgramDefinition program = service.getProgramDefinition(programId);

    // TODO(clouser): Check body.
    StatusDefinitions.Status newStatus =
        StatusDefinitions.Status.builder()
            .setStatusText("A status")
            .setLocalizedStatusText(LocalizedStrings.withDefaultValue("A status"))
            .setEmailBodyText("Some email body content")
            .setLocalizedEmailBodyText(LocalizedStrings.withDefaultValue("Some email body content"))
            .build();

    // TODO(clouser): Validate that the new status doesn't already exist, case insensitive.
    StatusDefinitions current = program.statusDefinitions();
    current.setStatuses(
        Stream.concat(current.getStatuses().stream(), Stream.of(newStatus))
            .collect(ImmutableList.toImmutableList()));
    service.setStatuses(programId, current);

    // Upon success, redirect to the index view.
    return redirect(routes.AdminProgramStatusesController.index(programId).url())
        .flashing("success", "Status created");
  }
}
