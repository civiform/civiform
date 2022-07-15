package controllers.admin;

import static annotations.FeatureFlags.ApplicationStatusTrackingEnabled;
import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.CiviFormController;
import java.util.stream.Stream;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.FormFactory;
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
  private final FormFactory formFactory;
  private final boolean statusTrackingEnabled;

  @Inject
  public AdminProgramStatusesController(
      ProgramService service,
      ProgramStatusesView statusesView,
      RequestChecker requestChecker,
      FormFactory formFactory,
      @ApplicationStatusTrackingEnabled boolean statusTrackingEnabled) {
    this.service = checkNotNull(service);
    this.statusesView = checkNotNull(statusesView);
    this.requestChecker = checkNotNull(requestChecker);
    this.formFactory = checkNotNull(formFactory);
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

    DynamicForm requestData = formFactory.form().bindFromRequest(request);
    String rawStatusText = requestData.get("status_text");
    if (Strings.isNullOrEmpty(rawStatusText)) {
      return badRequest("TODO: Fix: missing or empty status text");
    }
    final String statusText = rawStatusText.trim();
    String rawEmailBody = requestData.get("email_body");
    if (rawEmailBody == null) {
      return badRequest("TODO: Fix: missing email body");
    }
    final String emailBody = rawEmailBody.trim();

    StatusDefinitions current = program.statusDefinitions();
    boolean alreadyExists =
        current.getStatuses().stream()
            .map(StatusDefinitions.Status::statusText)
            .filter(existingStatus -> existingStatus.toLowerCase().equals(statusText.toLowerCase()))
            .findAny()
            .isPresent();
    if (alreadyExists) {
      return badRequest("TODO: Fix: Status already exists.");
    }

    // TODO(clouser): Add original name to form so that we can properly edit values.

    StatusDefinitions.Status newStatus =
        StatusDefinitions.Status.builder()
            .setStatusText(statusText)
            .setLocalizedStatusText(LocalizedStrings.withDefaultValue(statusText))
            .setEmailBodyText(emailBody)
            .setLocalizedEmailBodyText(LocalizedStrings.withDefaultValue(emailBody))
            .build();

    current.setStatuses(
        Stream.concat(current.getStatuses().stream(), Stream.of(newStatus))
            .collect(ImmutableList.toImmutableList()));
    service.setStatuses(programId, current);

    // Upon success, redirect to the index view.
    return redirect(routes.AdminProgramStatusesController.index(programId).url())
        .flashing("success", "Status created");
  }
}
