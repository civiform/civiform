package controllers.admin;

import static annotations.FeatureFlags.ApplicationStatusTrackingEnabled;
import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.CiviFormController;
import forms.admin.ProgramStatusesEditForm;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
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
    return ok(
        statusesView.render(request, service.getProgramDefinition(programId), Optional.empty()));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result edit(Http.Request request, long programId) throws ProgramNotFoundException {
    if (!statusTrackingEnabled) {
      return notFound("status tracking is not enabled");
    }
    requestChecker.throwIfProgramNotDraft(programId);
    ProgramDefinition program = service.getProgramDefinition(programId);

    ProgramStatusesEditForm form =
        ProgramStatusesEditForm.fromRequest(request, program, formFactory);
    if (!form.hasErrors()) {
      service.setStatuses(programId, form.getEditedStatuses());
      // Upon success, redirect to the index view.
      return redirect(routes.AdminProgramStatusesController.index(programId).url())
          .flashing(
              "success",
              form.rawFormValues().getOriginalStatusText().isEmpty()
                  ? "Status created"
                  : "Status updated");
    }

    return ok(
        statusesView.render(request, service.getProgramDefinition(programId), Optional.of(form)));
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
      return badRequest("TODO(#2752): Fix: missing or empty status text");
    }
    final String statusText = rawStatusText.trim();

    StatusDefinitions current = program.statusDefinitions();
    List<StatusDefinitions.Status> statusesForUpdate =
        current.getStatuses().stream().collect(Collectors.toList());
    int toRemoveIndex = matchingStatusIndex(statusText, current);
    if (toRemoveIndex == -1) {
      return badRequest("TODO(#2752): Fix: Could not find status to remove");
    }
    statusesForUpdate.remove(toRemoveIndex);
    current.setStatuses(ImmutableList.copyOf(statusesForUpdate));
    service.setStatuses(programId, current);

    // Upon success, redirect to the index view.
    return redirect(routes.AdminProgramStatusesController.index(programId).url())
        .flashing("success", "Status deleted");
  }

  private static int matchingStatusIndex(String statusText, StatusDefinitions statuses) {
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
