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
import org.apache.commons.lang3.tuple.Pair;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.Form;
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

    Pair<Form<ProgramStatusesEditForm>, Optional<StatusDefinitions>> validated =
        validateEditRequest(request, program);
    if (!validated.getLeft().hasErrors() && validated.getRight().isPresent()) {
      service.setStatuses(programId, validated.getRight().get());
      // Upon success, redirect to the index view.
      return redirect(routes.AdminProgramStatusesController.index(programId).url())
          .flashing(
              "success",
              program.statusDefinitions().getStatuses().size()
                      == validated.getRight().get().getStatuses().size()
                  ? "Status updated"
                  : "Status created");
    }

    return ok(
        statusesView.render(
            request, service.getProgramDefinition(programId), Optional.of(validated.getLeft())));
  }

  private Pair<Form<ProgramStatusesEditForm>, Optional<StatusDefinitions>> validateEditRequest(
      Http.Request request, ProgramDefinition program) {
    Form<ProgramStatusesEditForm> form =
        formFactory
            .form(ProgramStatusesEditForm.class)
            .bindFromRequest(request, ProgramStatusesEditForm.FIELD_NAMES.toArray(new String[0]));
    if (form.value().isEmpty()) {
      return Pair.of(form, Optional.empty());
    }
    ProgramStatusesEditForm value = form.value().get();
    // TODO(#2752): This is messy, do another pass on it once more is fleshed out.
    StatusDefinitions current = program.statusDefinitions();
    int existingStatusIndex = matchingStatusIndex(value.getStatusText(), current);
    int originalStatusIndex = matchingStatusIndex(value.getOriginalStatusText(), current);
    if (value.getOriginalStatusText().isEmpty()) {
      if (existingStatusIndex != -1) {
        form =
            form.withGlobalError(
                String.format("A status with name %s already exists", value.getStatusText()));
      }
    } else {
      if (originalStatusIndex == -1) {
        form =
            form.withGlobalError(
                "The status being edited no longer exists and may have been modified in a separate"
                    + " window.");
      } else if (originalStatusIndex != existingStatusIndex && existingStatusIndex != -1) {
        form =
            form.withGlobalError(
                String.format("A status with the name %s already exists", value.getStatusText()));
      }
    }

    if (form.hasErrors()) {
      return Pair.of(form, Optional.empty());
    }

    List<StatusDefinitions.Status> statusesForUpdate =
        current.getStatuses().stream().collect(Collectors.toList());
    if (originalStatusIndex != -1) {
      StatusDefinitions.Status preExistingStatus = statusesForUpdate.get(originalStatusIndex);
      statusesForUpdate.set(
          originalStatusIndex,
          StatusDefinitions.Status.builder()
              .setStatusText(value.getStatusText())
              .setLocalizedStatusText(preExistingStatus.localizedStatusText())
              .setEmailBodyText(value.getEmailBody())
              .setLocalizedEmailBodyText(
                  preExistingStatus
                      .localizedEmailBodyText()
                      .orElse(LocalizedStrings.withDefaultValue(value.getEmailBody())))
              .build());
    } else {
      statusesForUpdate.add(
          StatusDefinitions.Status.builder()
              .setStatusText(value.getStatusText())
              .setLocalizedStatusText(LocalizedStrings.withDefaultValue(value.getStatusText()))
              .setEmailBodyText(value.getEmailBody())
              .setLocalizedEmailBodyText(LocalizedStrings.withDefaultValue(value.getEmailBody()))
              .build());
    }
    current.setStatuses(ImmutableList.copyOf(statusesForUpdate));

    return Pair.of(form, Optional.of(current));
  }

  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result delete(Http.Request request, long programId) throws ProgramNotFoundException {
    if (!statusTrackingEnabled) {
      return notFound("status tracking is not enabled");
    }
    requestChecker.throwIfProgramNotDraft(programId);
    ProgramDefinition program = service.getProgramDefinition(programId);

    DynamicForm requestData = formFactory.form().bindFromRequest(request);
    String rawStatusText = requestData.get(ProgramStatusesView.DELETE_STATUS_TEXT_NAME);
    if (Strings.isNullOrEmpty(rawStatusText)) {
      return badRequest("missing or empty status text");
    }
    final String statusText = rawStatusText.trim();

    StatusDefinitions current = program.statusDefinitions();
    List<StatusDefinitions.Status> statusesForUpdate =
        current.getStatuses().stream().collect(Collectors.toList());
    int toRemoveIndex = matchingStatusIndex(statusText, current);
    if (toRemoveIndex == -1) {
      return redirect(routes.AdminProgramStatusesController.index(programId).url())
          .flashing(
              "error",
              String.format(
                  "The status being removed no longer exists and may have been removed in a"
                      + " separate window."));
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
