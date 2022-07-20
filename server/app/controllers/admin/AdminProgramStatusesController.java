package controllers.admin;

import static annotations.FeatureFlags.ApplicationStatusTrackingEnabled;
import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import controllers.CiviFormController;
import forms.admin.ProgramStatusesEditForm;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.apache.commons.lang3.tuple.Pair;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Http.HttpVerbs;
import play.mvc.Result;
import services.CiviFormError;
import services.ErrorAnd;
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
    ProgramDefinition program = service.getProgramDefinition(programId);
    int previousStatusCount = program.statusDefinitions().getStatuses().size();

    Optional<Form<ProgramStatusesEditForm>> maybeEditForm = Optional.empty();
    if (request.method().equalsIgnoreCase(HttpVerbs.POST)) {
      Pair<Form<ProgramStatusesEditForm>, Optional<StatusDefinitions>> validated;
      try {
        validated = validateEditRequest(request, program);
      } catch (MissingStatusException e) {
        return redirect(routes.AdminProgramStatusesController.index(programId).url())
            .flashing(
                "error",
                "The status being edited no longer exists and may have been modified in a separate"
                    + " window.");
      }
      maybeEditForm = Optional.of(validated.getLeft());
      if (!validated.getLeft().hasErrors() && validated.getRight().isPresent()) {
        Result result = redirect(routes.AdminProgramStatusesController.index(programId).url());
        ErrorAnd<ProgramDefinition, CiviFormError> setStatusResult =
            service.setStatuses(programId, validated.getRight().get());
        if (setStatusResult.isError()) {
          String errorMessage = joinErrors(setStatusResult.getErrors());
          result = result.flashing("error", errorMessage);
        } else {
          result =
              result.flashing(
                  "success",
                  previousStatusCount == validated.getRight().get().getStatuses().size()
                      ? "Status updated"
                      : "Status created");
        }
        return result;
      }
    }

    return ok(statusesView.render(request, service.getProgramDefinition(programId), maybeEditForm));
  }

  private Pair<Form<ProgramStatusesEditForm>, Optional<StatusDefinitions>> validateEditRequest(
      Http.Request request, ProgramDefinition program) throws MissingStatusException {
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
    if (value.getOriginalStatusText().isEmpty()) {
      try {
        current.setStatuses(
            addStatus(
                current.getStatuses(),
                StatusDefinitions.Status.builder()
                    .setStatusText(value.getStatusText())
                    .setLocalizedStatusText(
                        LocalizedStrings.withDefaultValue(value.getStatusText()))
                    .setEmailBodyText(value.getEmailBody())
                    .setLocalizedEmailBodyText(
                        Optional.of(LocalizedStrings.withDefaultValue(value.getEmailBody())))
                    .build()));
      } catch (DuplicateStatusException e) {
        form = form.withError(ProgramStatusesEditForm.STATUS_TEXT_FORM_NAME, e.userFacingMessage);
      }
    } else {
      try {
        current.setStatuses(
            replaceStatus(
                current.getStatuses(),
                value.getOriginalStatusText(),
                (existingStatus) -> {
                  return StatusDefinitions.Status.builder()
                      .setStatusText(value.getStatusText())
                      .setEmailBodyText(value.getEmailBody())
                      // Note: We preserve the existing localized status / email body
                      // text so that existing translated content isn't destroyed upon
                      // editing status.
                      .setLocalizedStatusText(existingStatus.localizedStatusText())
                      .setLocalizedEmailBodyText(existingStatus.localizedEmailBodyText())
                      .build();
                }));
      } catch (DuplicateStatusException e) {
        form = form.withError(ProgramStatusesEditForm.STATUS_TEXT_FORM_NAME, e.userFacingMessage);
      }
    }

    if (form.hasErrors()) {
      return Pair.of(form, Optional.empty());
    }
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
    try {
      current.setStatuses(removeStatus(current.getStatuses(), statusText));
    } catch (MissingStatusException e) {
      return redirect(routes.AdminProgramStatusesController.index(programId).url())
          .flashing(
              "error",
              "The status being deleted no longer exists and may have been deleted in a"
                  + " separate window.");
    }
    ErrorAnd<ProgramDefinition, CiviFormError> setStatusResult =
        service.setStatuses(programId, current);
    Result result =
        redirect(routes.AdminProgramStatusesController.index(programId).url())
            .flashing("success", "Status deleted");
    if (setStatusResult.isError()) {
      String errorMessage = joinErrors(setStatusResult.getErrors());
      result = result.flashing("error", errorMessage);
    } else {
      result = result.flashing("success", "Status deleted");
    }

    return result;
  }

  private static ImmutableList<StatusDefinitions.Status> addStatus(
      ImmutableList<StatusDefinitions.Status> statuses, StatusDefinitions.Status newStatus)
      throws DuplicateStatusException {
    if (statuses.stream()
        .filter(s -> s.statusText().equals(newStatus.statusText()))
        .findAny()
        .isPresent()) {
      throw new DuplicateStatusException(newStatus.statusText());
    }
    return ImmutableList.<StatusDefinitions.Status>builder()
        .addAll(statuses)
        .add(newStatus)
        .build();
  }

  private static ImmutableList<StatusDefinitions.Status> replaceStatus(
      ImmutableList<StatusDefinitions.Status> statuses,
      String replaceStatusName,
      Function<StatusDefinitions.Status, StatusDefinitions.Status> replacerFunc)
      throws MissingStatusException, DuplicateStatusException {
    ImmutableMap<String, Integer> statusNameToIndex = statusNameToIndexMap(statuses);
    if (!statusNameToIndex.containsKey(replaceStatusName)) {
      throw new MissingStatusException();
    }
    StatusDefinitions.Status editedStatus =
        replacerFunc.apply(statuses.get(statusNameToIndex.get(replaceStatusName)));
    if (!replaceStatusName.equals(editedStatus.statusText())
        && statusNameToIndex.containsKey(editedStatus.statusText())) {
      throw new DuplicateStatusException(editedStatus.statusText());
    }
    return IntStream.range(0, statuses.size())
        .boxed()
        .map(
            i -> {
              if (i.equals(statusNameToIndex.get(replaceStatusName))) {
                return editedStatus;
              }
              return statuses.get(i);
            })
        .collect(ImmutableList.toImmutableList());
  }

  private static ImmutableList<StatusDefinitions.Status> removeStatus(
      ImmutableList<StatusDefinitions.Status> statuses, String removeStatusName)
      throws MissingStatusException {
    ImmutableMap<String, Integer> statusNameToIndex = statusNameToIndexMap(statuses);
    if (!statusNameToIndex.containsKey(removeStatusName)) {
      throw new MissingStatusException();
    }
    return IntStream.range(0, statuses.size())
        .boxed()
        .filter(i -> !i.equals(statusNameToIndex.get(removeStatusName)))
        .map(i -> statuses.get(i))
        .collect(ImmutableList.toImmutableList());
  }

  private static ImmutableMap<String, Integer> statusNameToIndexMap(
      ImmutableList<StatusDefinitions.Status> statuses) {
    return IntStream.range(0, statuses.size())
        .boxed()
        .collect(
            ImmutableMap.toImmutableMap(i -> statuses.get(i).statusText(), i -> i, (s1, s2) -> s1));
  }

  private static final class MissingStatusException extends Exception {}

  private static final class DuplicateStatusException extends Exception {
    final String userFacingMessage;

    DuplicateStatusException(String statusName) {
      super();
      this.userFacingMessage = String.format("A status with name %s already exists", statusName);
    }
  }
}
