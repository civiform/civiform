package mapping.admin.apikeys;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import modules.MainModule;
import play.data.DynamicForm;
import play.data.validation.ValidationError;
import play.i18n.Messages;
import services.MessageKey;
import services.apikey.ApiKeyService;
import views.admin.apikeys.ApiKeyNewOnePageViewModel;
import views.admin.apikeys.ApiKeyNewOnePageViewModel.FormField;
import views.admin.apikeys.ApiKeyNewOnePageViewModel.ProgramCheckbox;

/** Maps data to the ApiKeyNewOnePageViewModel for the new API key page. */
public final class ApiKeyNewOnePageMapper {

  /**
   * Maps the programs a key can be granted access to, and any previously submitted form, to the
   * view model.
   *
   * @param programNames the programs a new key may be granted read access to
   * @param maybeForm the submitted form, present only when creation failed validation
   * @param enUsMessages used to format field errors, as the legacy view did
   * @param showForm false renders the "create a program first" notice instead of the form
   */
  public ApiKeyNewOnePageViewModel map(
      ImmutableSet<String> programNames,
      Optional<DynamicForm> maybeForm,
      Messages enUsMessages,
      boolean showForm) {
    return ApiKeyNewOnePageViewModel.builder()
        .title("Create a new API key")
        .showForm(showForm)
        .formActionUrl(controllers.admin.routes.AdminApiKeysController.create().url())
        .keyNameField(
            buildField(
                ApiKeyService.FORM_FIELD_NAME_KEY_NAME,
                "API key name",
                /* type= */ "text",
                maybeForm,
                enUsMessages))
        .expirationField(
            buildField(
                ApiKeyService.FORM_FIELD_NAME_EXPIRATION,
                "Expiration date",
                /* type= */ "date",
                maybeForm,
                enUsMessages))
        .subnetField(
            buildField(
                ApiKeyService.FORM_FIELD_NAME_SUBNET,
                "API key subnet",
                /* type= */ "text",
                maybeForm,
                enUsMessages))
        .showProgramsError(
            maybeForm
                .map(form -> form.error(ApiKeyService.PROGRAMS_FIELD_GROUP_NAME).isPresent())
                .orElse(false))
        .programCheckboxes(buildProgramCheckboxes(programNames, maybeForm))
        .build();
  }

  /** The field name and the input id are both the form key, as they were in the legacy view. */
  private FormField buildField(
      String key,
      String label,
      String type,
      Optional<DynamicForm> maybeForm,
      Messages enUsMessages) {
    Optional<ValidationError> error = maybeForm.flatMap(form -> form.error(key));

    return FormField.builder()
        .id(key)
        .errorsId(String.format("%s-errors", key))
        .name(key)
        .label(label)
        .type(type)
        .value(maybeForm.flatMap(form -> form.value(key)).map(String::valueOf).orElse(""))
        .hasError(error.isPresent())
        .errorMessage(
            error
                .map(
                    validationError ->
                        enUsMessages.apply(
                            MessageKey.TOAST_ERROR_MSG_OUTLINE.getKeyName(),
                            validationError.format(enUsMessages)))
                .orElse(null))
        .build();
  }

  private ImmutableList<ProgramCheckbox> buildProgramCheckboxes(
      ImmutableSet<String> programNames, Optional<DynamicForm> maybeForm) {
    return programNames.stream()
        .sorted(String::compareToIgnoreCase)
        .map(
            name -> {
              String fieldName = programReadGrantFieldName(name);

              return ProgramCheckbox.builder()
                  .id(MainModule.SLUGIFIER.slugify(name))
                  .name(fieldName)
                  .label(name)
                  .checked(maybeForm.map(form -> form.value(fieldName).isPresent()).orElse(false))
                  .build();
            })
        .collect(ImmutableList.toImmutableList());
  }

  private String programReadGrantFieldName(String name) {
    return "grant-program-read[" + MainModule.SLUGIFIER.slugify(name) + "]";
  }
}
