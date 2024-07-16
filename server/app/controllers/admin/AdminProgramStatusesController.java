package controllers.admin;

import static com.google.common.base.Preconditions.checkNotNull;

import auth.Authorizers;
import auth.ProfileUtils;
import com.google.inject.Inject;
import controllers.CiviFormController;
import controllers.FlashKey;
import forms.admin.ProgramStatusesForm;
import java.util.Optional;
import org.pac4j.play.java.Secure;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Http;
import play.mvc.Result;
import repository.ApplicationStatusesRepository;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.LocalizedStrings;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;
import services.statuses.DuplicateStatusException;
import services.statuses.StatusDefinitions;
import services.statuses.StatusService;
import views.admin.programs.ProgramStatusesView;

/**
 * Controller for displaying and modifying the {@link StatusDefinitions} associated with a program.
 * This has logic for displaying, creating, editing, and deleting statuses.
 */
public final class AdminProgramStatusesController extends CiviFormController {

  private final StatusService service;
  private final ProgramStatusesView statusesView;
  private final RequestChecker requestChecker;
  private final FormFactory formFactory;
  private final ApplicationStatusesRepository applicationStatusesRepository;
  private final ProgramService programService;

  @Inject
  public AdminProgramStatusesController(
      StatusService service,
      ProgramStatusesView statusesView,
      RequestChecker requestChecker,
      FormFactory formFactory,
      ProfileUtils profileUtils,
      VersionRepository versionRepository,
      ApplicationStatusesRepository applicationStatusesRepository,
      ProgramService programService) {
    super(profileUtils, versionRepository);
    this.service = checkNotNull(service);
    this.statusesView = checkNotNull(statusesView);
    this.requestChecker = checkNotNull(requestChecker);
    this.formFactory = checkNotNull(formFactory);
    this.applicationStatusesRepository = checkNotNull(applicationStatusesRepository);
    this.programService = checkNotNull(programService);
  }

  /** Displays the list of {@link StatusDefinitions} associated with the program. */
  @Secure(authorizers = Authorizers.Labels.CIVIFORM_ADMIN)
  public Result index(Http.Request request, long programId) throws ProgramNotFoundException {
    requestChecker.throwIfProgramNotDraft(programId);
    ProgramDefinition program = programService.getFullProgramDefinition(programId);
    return ok(
        statusesView.render(
            request,
            program,
            applicationStatusesRepository.lookupActiveStatusDefinitions(program.adminName()),
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
  public Result createOrUpdate(Http.Request request, Long programId)
      throws ProgramNotFoundException {
    requestChecker.throwIfProgramNotDraft(programId);
    ProgramDefinition program = programService.getFullProgramDefinition(programId);
    StatusDefinitions activeStatusDefinitions =
        applicationStatusesRepository.lookupActiveStatusDefinitions(program.adminName());
    int previousStatusCount = activeStatusDefinitions.getStatuses().size();
    Optional<StatusDefinitions.Status> previousDefaultStatus =
        activeStatusDefinitions.getDefaultStatus();

    Form<ProgramStatusesForm> form =
        formFactory
            .form(ProgramStatusesForm.class)
            .bindFromRequest(request, ProgramStatusesForm.FIELD_NAMES.toArray(new String[0]));
    if (form.hasErrors()) {
      // Redirecting to the index view would re-render the statuses and lose
      // any form values / errors. Instead, re-render the view at this URL
      // whenever there is are form validation errors, which preserves the
      // form values.
      return ok(statusesView.render(request, program, activeStatusDefinitions, Optional.of(form)));
    }
    // Because we need to look directly at the value sent (or absent) in the form data
    // itself to determine if a checkbox is checked, we need to calculate this here
    // before passing the value into createOrEditStatusFromFormData. If the checkbox
    // is not checked, no value is sent. If it is checked, "on" is sent for the value.
    Optional<String> maybeSetDefault =
        Optional.ofNullable(form.rawData().get(ProgramStatusesForm.DEFAULT_CHECKBOX_NAME));
    final boolean setDefault;
    if (maybeSetDefault.map(String::isBlank).orElse(true)) {
      // Empty (box was not checked) or blank
      setDefault = false;
    } else if (maybeSetDefault.get().equals("on")) {
      setDefault = true;
    } else {
      return badRequest(
          String.format(
              "%s value is invalid: %s",
              ProgramStatusesForm.DEFAULT_CHECKBOX_NAME, maybeSetDefault.get()));
    }

    final ErrorAnd<StatusDefinitions, CiviFormError> mutateResult;
    try {
      mutateResult = createOrEditStatusFromFormData(programId, form.get(), setDefault);
    } catch (DuplicateStatusException e) {
      // Redirecting to the index view would re-render the statuses and lose
      // any form values / errors. Instead, re-render the view at this URL
      // whenever there is are form validation errors, which preserves the
      // form values.
      form = form.withError(ProgramStatusesForm.STATUS_TEXT_FORM_NAME, e.userFacingMessage());
      return ok(statusesView.render(request, program, activeStatusDefinitions, Optional.of(form)));
    }
    final String indexUrl = routes.AdminProgramStatusesController.index(programId).url();
    if (mutateResult.isError()) {
      return redirect(indexUrl).flashing(FlashKey.ERROR, joinErrors(mutateResult.getErrors()));
    }
    boolean isUpdate = previousStatusCount == mutateResult.getResult().getStatuses().size();
    String toastMessage = isUpdate ? "Status updated" : "Status created";
    final ProgramStatusesForm programStatusesForm = form.get();
    if (setDefault
        && previousDefaultStatus
            .map(status -> !status.matches(programStatusesForm.getConfiguredStatusText()))
            .orElse(true)) {
      toastMessage =
          programStatusesForm.getStatusText() + " has been updated to the default status";
    }
    return redirect(indexUrl).flashing(FlashKey.SUCCESS, toastMessage);
  }

  private ErrorAnd<StatusDefinitions, CiviFormError> createOrEditStatusFromFormData(
      Long programId, ProgramStatusesForm formData, boolean setDefault)
      throws ProgramNotFoundException, DuplicateStatusException {
    // An empty "configuredStatusText" parameter indicates that a new
    // status should be created.
    if (formData.getConfiguredStatusText().isEmpty()) {
      StatusDefinitions.Status.Builder newStatusBuilder =
          StatusDefinitions.Status.builder()
              .setStatusText(formData.getStatusText())
              .setLocalizedStatusText(LocalizedStrings.withDefaultValue(formData.getStatusText()))
              .setDefaultStatus(Optional.of(setDefault));
      if (!formData.getEmailBody().isBlank()) {
        newStatusBuilder =
            newStatusBuilder.setLocalizedEmailBodyText(
                Optional.of(LocalizedStrings.withDefaultValue(formData.getEmailBody())));
      }
      return service.appendStatus(programId, newStatusBuilder.build());
    }

    return service.editStatus(
        programId,
        formData.getConfiguredStatusText(),
        (existingStatus) -> {
          StatusDefinitions.Status.Builder builder =
              StatusDefinitions.Status.builder()
                  .setStatusText(formData.getStatusText())
                  .setLocalizedStatusText(
                      existingStatus
                          .localizedStatusText()
                          .updateTranslation(
                              LocalizedStrings.DEFAULT_LOCALE, formData.getStatusText()))
                  .setDefaultStatus(Optional.of(setDefault));
          if (!formData.getEmailBody().isBlank()) {
            // Only carry forward translated content if a non-empty email body
            // has been provided. Any other translations will be lost when
            // this occurs.
            if (existingStatus.localizedEmailBodyText().isPresent()) {
              // Preserve any existing translations.
              LocalizedStrings updated =
                  existingStatus
                      .localizedEmailBodyText()
                      .get()
                      .updateTranslation(LocalizedStrings.DEFAULT_LOCALE, formData.getEmailBody());
              builder.setLocalizedEmailBodyText(Optional.of(updated));
            } else {
              builder.setLocalizedEmailBodyText(
                  Optional.of(LocalizedStrings.withDefaultValue(formData.getEmailBody())));
            }
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
    requestChecker.throwIfProgramNotDraft(programId);
    DynamicForm requestData = formFactory.form().bindFromRequest(request);
    String rawDeleteStatusText = requestData.get(ProgramStatusesView.DELETE_STATUS_TEXT_NAME);
    final String deleteStatusText = rawDeleteStatusText != null ? rawDeleteStatusText : "";
    if (deleteStatusText.isBlank()) {
      return badRequest("missing or empty status text");
    }

    final String indexUrl = routes.AdminProgramStatusesController.index(programId).url();
    ErrorAnd<StatusDefinitions, CiviFormError> deleteStatusResult =
        service.deleteStatus(programId, deleteStatusText);
    if (deleteStatusResult.isError()) {
      return redirect(indexUrl)
          .flashing(FlashKey.ERROR, joinErrors(deleteStatusResult.getErrors()));
    }
    return redirect(indexUrl).flashing(FlashKey.SUCCESS, "Status deleted");
  }
}
