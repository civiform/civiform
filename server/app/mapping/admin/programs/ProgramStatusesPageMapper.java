package mapping.admin.programs;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import forms.admin.ProgramStatusesForm;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import play.data.Form;
import play.data.validation.ValidationError;
import play.i18n.Messages;
import services.MessageKey;
import services.program.ProgramDefinition;
import services.statuses.StatusDefinitions;
import views.admin.programs.ProgramStatusesPageViewModel;
import views.admin.programs.ProgramStatusesPageViewModel.StatusFormModal;
import views.admin.programs.ProgramStatusesPageViewModel.StatusItem;
import views.admin.programs.ProgramStatusesPageViewModel.Toast;

/** Maps data to the ProgramStatusesPageViewModel for the "Manage application statuses" page. */
public final class ProgramStatusesPageMapper {

  /**
   * Maps the program's statuses, and any rejected create/edit submission, to the view model.
   *
   * @param program the program whose statuses are managed
   * @param activeStatusDefinitions the program's current statuses
   * @param maybeStatusForm set when re-rendering after a create/edit attempt was rejected; its
   *     values and errors are shown in the matching modal, which opens on page load
   * @param messages formats validation errors
   * @param hasTranslatableLocales whether the "Manage translations" button is shown
   * @param successMessage flash success message, rendered as a toast
   * @param errorMessage flash error message, rendered as an error toast
   */
  public ProgramStatusesPageViewModel map(
      ProgramDefinition program,
      StatusDefinitions activeStatusDefinitions,
      Optional<Form<ProgramStatusesForm>> maybeStatusForm,
      Messages messages,
      boolean hasTranslatableLocales,
      Optional<String> successMessage,
      Optional<String> errorMessage) {
    List<Toast> toasts = new ArrayList<>();
    successMessage.ifPresent(
        message ->
            toasts.add(
                Toast.builder()
                    .id(UUID.randomUUID().toString())
                    .message(message)
                    .type("SUCCESS")
                    .canDismiss(true)
                    .build()));
    errorMessage.ifPresent(
        message ->
            toasts.add(
                Toast.builder()
                    .id(UUID.randomUUID().toString())
                    .message("Error: " + message)
                    .type("ERROR")
                    .canDismiss(false)
                    .build()));

    StatusFormModal createStatusModal;
    if (isCreationForm(maybeStatusForm)) {
      createStatusModal =
          buildFormModal(
              maybeStatusForm.get(),
              messages,
              /* displayOnLoad= */ true,
              /* showEmailDeletionWarning= */ false);
    } else {
      createStatusModal =
          buildFormModal(
              new ProgramStatusesForm(),
              /* displayOnLoad= */ false,
              /* showEmailDeletionWarning= */ false);
    }

    ImmutableList<StatusItem> statusItems =
        activeStatusDefinitions.getStatuses().stream()
            .map(status -> buildStatusItem(status, maybeStatusForm, messages))
            .collect(ImmutableList.toImmutableList());

    return ProgramStatusesPageViewModel.builder()
        .toasts(toasts)
        .programDisplayName(program.localizedName().getDefault())
        .manageTranslationsUrl(
            hasTranslatableLocales
                ? routes.AdminProgramTranslationsController.redirectToFirstLocale(
                        program.adminName())
                    .url()
                : null)
        .createOrUpdateUrl(routes.AdminProgramStatusesController.createOrUpdate(program.id()).url())
        .deleteUrl(routes.AdminProgramStatusesController.delete(program.id()).url())
        .currentDefaultStatusText(
            activeStatusDefinitions
                .getDefaultStatus()
                .map(StatusDefinitions.Status::statusText)
                .orElse(null))
        .createStatusModal(createStatusModal)
        .statusItems(statusItems)
        .build();
  }

  private StatusItem buildStatusItem(
      StatusDefinitions.Status status,
      Optional<Form<ProgramStatusesForm>> maybeStatusForm,
      Messages messages) {
    boolean hasEmail = status.localizedEmailBodyText().isPresent();

    StatusFormModal editModal;
    if (isFormForStatus(maybeStatusForm, status)) {
      editModal =
          buildFormModal(
              maybeStatusForm.get(),
              messages,
              /* displayOnLoad= */ true,
              /* showEmailDeletionWarning= */ hasEmail);
    } else {
      editModal =
          buildFormModal(
              ProgramStatusesForm.fromStatus(status),
              /* displayOnLoad= */ false,
              /* showEmailDeletionWarning= */ hasEmail);
    }

    return StatusItem.builder()
        .statusText(status.statusText())
        .hasEmail(hasEmail)
        .defaultStatus(status.defaultStatus().orElse(false))
        .editModal(editModal)
        .deleteModalId(randomModalId())
        .build();
  }

  /** Builds a modal for a form that was not submitted, so it carries no errors. */
  private StatusFormModal buildFormModal(
      ProgramStatusesForm formData, boolean displayOnLoad, boolean showEmailDeletionWarning) {
    return formModalBuilder(formData, displayOnLoad, showEmailDeletionWarning)
        .globalErrors(ImmutableList.of())
        .statusTextErrors(ImmutableList.of())
        .emailBodyErrors(ImmutableList.of())
        .build();
  }

  /** Builds a modal re-rendering a submitted form along with its validation errors. */
  private StatusFormModal buildFormModal(
      Form<ProgramStatusesForm> form,
      Messages messages,
      boolean displayOnLoad,
      boolean showEmailDeletionWarning) {
    ImmutableList<String> globalErrors =
        form.globalErrors().stream()
            .map(error -> error.format(messages))
            .collect(ImmutableList.toImmutableList());

    return formModalBuilder(form.value().get(), displayOnLoad, showEmailDeletionWarning)
        .globalErrors(globalErrors)
        .statusTextErrors(
            formatFieldErrors(form.errors(ProgramStatusesForm.STATUS_TEXT_FORM_NAME), messages))
        .emailBodyErrors(
            formatFieldErrors(form.errors(ProgramStatusesForm.EMAIL_BODY_FORM_NAME), messages))
        .build();
  }

  private StatusFormModal.StatusFormModalBuilder formModalBuilder(
      ProgramStatusesForm formData, boolean displayOnLoad, boolean showEmailDeletionWarning) {
    return StatusFormModal.builder()
        .id(randomModalId())
        .creation(formData.getConfiguredStatusText().isEmpty())
        .displayOnLoad(displayOnLoad)
        .showEmailDeletionWarning(showEmailDeletionWarning)
        .configuredStatusText(formData.getConfiguredStatusText())
        .statusText(formData.getStatusText())
        .emailBody(formData.getEmailBody())
        .defaultStatus(formData.getDefaultStatus().orElse(false));
  }

  /** Field errors are wrapped in the "Error: " outline, as FieldWithLabel rendered them. */
  private static ImmutableList<String> formatFieldErrors(
      List<ValidationError> errors, Messages messages) {
    return errors.stream()
        .map(
            error ->
                messages.at(
                    MessageKey.TOAST_ERROR_MSG_OUTLINE.getKeyName(), error.format(messages)))
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Returns whether the form is for creating a new status (signified by the "configuredStatusText"
   * field being empty).
   */
  private static boolean isCreationForm(Optional<Form<ProgramStatusesForm>> maybeForm) {
    return maybeForm.map(f -> f.value().get().getConfiguredStatusText().isEmpty()).orElse(false);
  }

  /**
   * Returns whether the form matches the status object being rendered. The page has multiple inline
   * modals for editing and creating statuses.
   */
  private static boolean isFormForStatus(
      Optional<Form<ProgramStatusesForm>> maybeEditForm, StatusDefinitions.Status status) {
    return maybeEditForm.isPresent()
        && status
            .statusText()
            .equalsIgnoreCase(maybeEditForm.get().value().get().getConfiguredStatusText());
  }

  /** Mirrors Modal.randomModalId(): element ids must start with a letter, UUIDs may not. */
  private static String randomModalId() {
    return "uuid-" + UUID.randomUUID();
  }
}
