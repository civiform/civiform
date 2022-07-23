package controllers.admin;

import static annotations.FeatureFlags.ApplicationStatusTrackingEnabled;
import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import controllers.CiviFormController;
import forms.admin.ProgramStatusesEditForm;
import java.util.Optional;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import services.CiviFormError;
import services.ErrorAnd;
import services.LocalizedStrings;
import services.program.DuplicateStatusException;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.program.StatusDefinitions;
import views.admin.programs.ProgramStatusesView;

/**
 * Controller for displaying and modifying the {@link StatusDefinitions} associated with a program.
 * This has logic for displaying, creating, editing, and deleting statuses.
 */
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

  /** Displays the list of {@link StatusDefinitions} associated with the program. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request, long programId) throws ProgramNotFoundException {
    if (!statusTrackingEnabled) {
      return notFound("status tracking is not enabled");
    }
    requestChecker.throwIfProgramNotDraft(programId);

    return ok(
        statusesView.render(
            request,
            service.getProgramDefinition(programId),
            /* maybeStatusForm= */ Optional.empty()));
  }

  /**
   * Creates a new status or updates an existing status associated with the program. Creation is
   * signified by the "originalStatusText" form parameter being set to empty. Updates are signified
   * by the "originalStatusText" form parameter being set to a status that already exists in the
   * current program definition.
   *
   * <p>If a status is created, it's appended to the end of the list of current statuses. If a
   * status is edited, it's replaced in-line.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result createOrUpdate(Http.Request request, long programId)
      throws ProgramNotFoundException {
    if (!statusTrackingEnabled) {
      return notFound("status tracking is not enabled");
    }
    requestChecker.throwIfProgramNotDraft(programId);
    ProgramDefinition program = service.getProgramDefinition(programId);
    int previousStatusCount = program.statusDefinitions().getStatuses().size();

    Form<ProgramStatusesEditForm> form =
        formFactory
            .form(ProgramStatusesEditForm.class)
            .bindFromRequest(request, ProgramStatusesEditForm.FIELD_NAMES.toArray(new String[0]));
    if (form.hasErrors()) {
      // Redirecting to the index view would re-render the statuses and lose
      // any form values / errors. Instead, re-render the view at this URL
      // whenever there is are form validation errors, which preserves the
      // form values.
      return ok(statusesView.render(request, program, Optional.of(form)));
    }
    final ErrorAnd<ProgramDefinition, CiviFormError> mutateResult;
    try {
      mutateResult = createOrEditStatusFromFormData(program, form.get());
    } catch (DuplicateStatusException e) {
      // Redirecting to the index view would re-render the statuses and lose
      // any form values / errors. Instead, re-render the view at this URL
      // whenever there is are form validation errors, which preserves the
      // form values.
      form = form.withError(ProgramStatusesEditForm.STATUS_TEXT_FORM_NAME, e.userFacingMessage());
      return ok(statusesView.render(request, program, Optional.of(form)));
    }
    final String indexUrl = routes.AdminProgramStatusesController.index(programId).url();
    if (mutateResult.isError()) {
      return redirect(indexUrl).flashing("error", joinErrors(mutateResult.getErrors()));
    }
    return redirect(indexUrl)
        .flashing(
            "success",
            previousStatusCount == mutateResult.getResult().statusDefinitions().getStatuses().size()
                ? "Status updated"
                : "Status created");
  }

  private ErrorAnd<ProgramDefinition, CiviFormError> createOrEditStatusFromFormData(
      ProgramDefinition program, ProgramStatusesEditForm formData)
      throws ProgramNotFoundException, DuplicateStatusException {
    // An empty "originalStatusText" parameter indicates that a new
    // status should be created.
    if (formData.getOriginalStatusText().isEmpty()) {
      return service.appendStatus(
          program.id(),
          StatusDefinitions.Status.builder()
              .setStatusText(formData.getStatusText())
              .setLocalizedStatusText(LocalizedStrings.withDefaultValue(formData.getStatusText()))
              .setEmailBodyText(formData.getEmailBody())
              .setLocalizedEmailBodyText(
                  Optional.of(LocalizedStrings.withDefaultValue(formData.getEmailBody())))
              .build());
    }
    return service.editStatus(
        program.id(),
        formData.getOriginalStatusText(),
        (existingStatus) -> {
          return StatusDefinitions.Status.builder()
              .setStatusText(formData.getStatusText())
              .setEmailBodyText(formData.getEmailBody())
              // Note: We preserve the existing localized status / email body
              // text so that existing translated content isn't destroyed upon
              // editing status.
              .setLocalizedStatusText(existingStatus.localizedStatusText())
              .setLocalizedEmailBodyText(existingStatus.localizedEmailBodyText())
              .build();
        });
  }

  /**
   * Deletes a status that is currently associated with the program as signified by the
   * "deleteStatusText" form parameter.
   */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result delete(Http.Request request, long programId) throws ProgramNotFoundException {
    if (!statusTrackingEnabled) {
      return notFound("status tracking is not enabled");
    }
    requestChecker.throwIfProgramNotDraft(programId);
    ProgramDefinition program = service.getProgramDefinition(programId);

    DynamicForm requestData = formFactory.form().bindFromRequest(request);
    String rawDeleteStatusText = requestData.get(ProgramStatusesView.DELETE_STATUS_TEXT_NAME);
    if (Strings.isNullOrEmpty(rawDeleteStatusText)) {
      return badRequest("missing or empty status text");
    }
    final String deleteStatusText = rawDeleteStatusText.trim();

    final String indexUrl = routes.AdminProgramStatusesController.index(programId).url();
    ErrorAnd<ProgramDefinition, CiviFormError> deleteStatusResult =
        service.deleteStatus(program.id(), deleteStatusText);
    if (deleteStatusResult.isError()) {
      return redirect(indexUrl).flashing("error", joinErrors(deleteStatusResult.getErrors()));
    }
    return redirect(indexUrl).flashing("success", "Status deleted");
  }
}
