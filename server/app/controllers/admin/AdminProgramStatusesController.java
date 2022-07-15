package controllers.admin;

import static annotations.FeatureFlags.ApplicationStatusTrackingEnabled;
import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.CiviFormController;
import java.util.List;
import java.util.stream.Collectors;
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
  public Result edit(Http.Request request, long programId) throws ProgramNotFoundException {
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

    String originalStatusText = requestData.get("original_status_text");
    if (originalStatusText == null) {
      return badRequest("TODO: Fix: missing original_status_text");
    }

    // TODO(clouser): This is messy, do another pass on it once more is fleshed out.
    StatusDefinitions current = program.statusDefinitions();
    int existingStatusIndex = matchingStatusIndex(statusText, current);
    int originalStatusIndex = matchingStatusIndex(originalStatusText, current);
    if (originalStatusText.isEmpty()) {
      if (existingStatusIndex != -1) {
        // This corresponds to creating a new status with the same name as an existing one.
        return badRequest("TODO: Fix: Status already exists.");
      }
    } else {
      if (originalStatusIndex == -1) {
        return badRequest(
            "TODO: Fix: Status doesn't exist in the list we're trying to update. Nothing to move.");
      } else if (originalStatusIndex != existingStatusIndex && existingStatusIndex != -1) {
        return badRequest(
            "TODO: Fix: Trying to update an existing status to a name that's already present.");
      }
    }

    List<StatusDefinitions.Status> statusesForUpdate =
        current.getStatuses().stream().collect(Collectors.toList());
    if (originalStatusIndex != -1) {
      StatusDefinitions.Status preExistingStatus = statusesForUpdate.get(originalStatusIndex);
      statusesForUpdate.set(
          originalStatusIndex,
          StatusDefinitions.Status.builder()
              .setStatusText(statusText)
              .setLocalizedStatusText(preExistingStatus.localizedStatusText())
              .setEmailBodyText(emailBody)
              .setLocalizedEmailBodyText(
                  preExistingStatus
                      .localizedEmailBodyText()
                      .orElse(LocalizedStrings.withDefaultValue(emailBody)))
              .build());
    } else {
      statusesForUpdate.add(
          StatusDefinitions.Status.builder()
              .setStatusText(statusText)
              .setLocalizedStatusText(LocalizedStrings.withDefaultValue(statusText))
              .setEmailBodyText(emailBody)
              .setLocalizedEmailBodyText(LocalizedStrings.withDefaultValue(emailBody))
              .build());
    }
    current.setStatuses(ImmutableList.copyOf(statusesForUpdate));
    service.setStatuses(programId, current);

    // Upon success, redirect to the index view.
    return redirect(routes.AdminProgramStatusesController.index(programId).url())
        .flashing("success", originalStatusIndex == -1 ? "Status created" : "Status updated");
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result delete(Http.Request request, long programId) throws ProgramNotFoundException {
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

    StatusDefinitions current = program.statusDefinitions();
    List<StatusDefinitions.Status> statusesForUpdate =
        current.getStatuses().stream().collect(Collectors.toList());
    int toRemoveIndex = matchingStatusIndex(statusText, current);
    if (toRemoveIndex == -1) {
      return badRequest("TODO: Fix: Could not find status to remove");
    }
    statusesForUpdate.remove(toRemoveIndex);
    current.setStatuses(ImmutableList.copyOf(statusesForUpdate));
    service.setStatuses(programId, current);

    // Upon success, redirect to the index view.
    return redirect(routes.AdminProgramStatusesController.index(programId).url())
        .flashing("success", "Status deleted");
  }

  private int matchingStatusIndex(String statusText, StatusDefinitions statuses) {
    for (int i = 0; i < statuses.getStatuses().size(); i++) {
      if (statuses
          .getStatuses()
          .get(i)
          .statusText()
          .toLowerCase()
          .equals(statusText.toLowerCase())) {
        return i;
      }
    }
    return -1;
  }
}
