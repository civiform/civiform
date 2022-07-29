package controllers.admin;

import static annotations.FeatureFlags.ApplicationStatusTrackingEnabled;
import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import com.google.inject.Inject;
import controllers.CiviFormController;
import forms.admin.ProgramStatusesForm;
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
   * Creates a new status or updates an existing status associated with the program.
   *
   * <p>Creation is signified by the "configuredStatusText" form parameter being set to empty.
   * Updates are signified by the "configuredStatusText" form parameter being set to a status that
   * already exists in the current program definition.
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

    Form<ProgramStatusesForm> form =
        formFactory
            .form(ProgramStatusesForm.class)
            .bindFromRequest(request, ProgramStatusesForm.FIELD_NAMES.toArray(new String[0]));
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
      form = form.withError(ProgramStatusesForm.STATUS_TEXT_FORM_NAME, e.userFacingMessage());
      return ok(statusesView.render(request, program, Optional.of(form)));
    }
    final String indexUrl = routes.AdminProgramStatusesController.index(programId).url();
    if (mutateResult.isError()) {
      return redirect(indexUrl).flashing("error", joinErrors(mutateResult.getErrors()));
    }
    boolean isUpdate =
        previousStatusCount == mutateResult.getResult().statusDefinitions().getStatuses().size();
    return redirect(indexUrl).flashing("success", isUpdate ? "Status updated" : "Status created");
  }

  private ErrorAnd<ProgramDefinition, CiviFormError> createOrEditStatusFromFormData(
      ProgramDefinition program, ProgramStatusesForm formData)
      throws ProgramNotFoundException, DuplicateStatusException {
    // An empty "configuredStatusText" parameter indicates that a new
    // status should be created.
    if (formData.getConfiguredStatusText().isEmpty()) {
      StatusDefinitions.Status.Builder newStatusBuilder =
          StatusDefinitions.Status.builder()
              .setStatusText(formData.getStatusText())
              .setLocalizedStatusText(LocalizedStrings.withDefaultValue(formData.getStatusText()));
      if (!formData.getEmailBody().isBlank()) {
        newStatusBuilder =
            newStatusBuilder
                .setEmailBodyText(Optional.of(formData.getEmailBody()))
                .setLocalizedEmailBodyText(
                    Optional.of(LocalizedStrings.withDefaultValue(formData.getEmailBody())));
      }
      return service.appendStatus(program.id(), newStatusBuilder.build());
    }
    return service.editStatus(
        program.id(),
        formData.getConfiguredStatusText(),
        (existingStatus) -> {
          StatusDefinitions.Status.Builder builder =
              StatusDefinitions.Status.builder()
                  .setStatusText(formData.getStatusText())
                  // TODO(#2752): Consider always setting the
                  // English localized text from this handler and
                  // disabling the English translations UI.
                  // Note: We preserve the existing localized status / email body
                  // text so that existing translated content isn't destroyed upon
                  // editing status.
                  .setLocalizedStatusText(existingStatus.localizedStatusText());
          if (existingStatus.localizedEmailBodyText().isPresent()) {
            builder.setLocalizedEmailBodyText(existingStatus.localizedEmailBodyText());
          }
          if (!formData.getEmailBody().isBlank()) {
            builder.setEmailBodyText(Optional.of(formData.getEmailBody()));
          }
          return builder.build();
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
    // TODO(#2752): Consider only trimming the user-provided data before serializing
    // to a program's statuses here and when editing a status.
    final String deleteStatusText = rawDeleteStatusText != null ? rawDeleteStatusText.trim() : "";
    if (deleteStatusText.isBlank()) {
      return badRequest("missing or empty status text");
    }

    final String indexUrl = routes.AdminProgramStatusesController.index(programId).url();
    ErrorAnd<ProgramDefinition, CiviFormError> deleteStatusResult =
        service.deleteStatus(program.id(), deleteStatusText);
    if (deleteStatusResult.isError()) {
      return redirect(indexUrl).flashing("error", joinErrors(deleteStatusResult.getErrors()));
    }
    return redirect(indexUrl).flashing("success", "Status deleted");
  }
}
