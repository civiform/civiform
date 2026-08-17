package mapping.admin.apikeys;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import modules.MainModule;
import play.data.DynamicForm;
import services.apikey.ApiKeyService;
import views.admin.apikeys.ApiKeyNewOnePageViewModel;
import views.admin.apikeys.ApiKeyNewOnePageViewModel.ProgramCheckbox;

/** Maps data to the ApiKeyNewOnePageViewModel for the new API key page. */
public final class ApiKeyNewOnePageMapper {

  /**
   * Maps the programs a key can be granted access to, and any previously submitted form, to the
   * view model.
   *
   * @param programNames the programs a new key may be granted read access to
   * @param maybeForm the submitted form, present only when creation failed validation
   */
  public ApiKeyNewOnePageViewModel map(
      ImmutableSet<String> programNames, Optional<DynamicForm> maybeForm) {
    return ApiKeyNewOnePageViewModel.builder()
        .title("Create a new API key")
        // On first load with no program to grant access to, the page shows the
        // "create a program first" notice instead of the form. A rejected submission
        // always re-renders the form, as the legacy view did.
        .showForm(maybeForm.isPresent() || !programNames.isEmpty())
        .formActionUrl(controllers.admin.routes.AdminApiKeysController.create().url())
        .keyNameValue(fieldValue(maybeForm, ApiKeyService.FORM_FIELD_NAME_KEY_NAME))
        .expirationValue(fieldValue(maybeForm, ApiKeyService.FORM_FIELD_NAME_EXPIRATION))
        .subnetValue(fieldValue(maybeForm, ApiKeyService.FORM_FIELD_NAME_SUBNET))
        .keyNameError(fieldError(maybeForm, ApiKeyService.FORM_FIELD_NAME_KEY_NAME))
        .expirationError(fieldError(maybeForm, ApiKeyService.FORM_FIELD_NAME_EXPIRATION))
        .subnetError(fieldError(maybeForm, ApiKeyService.FORM_FIELD_NAME_SUBNET))
        .showProgramsError(
            maybeForm
                .map(form -> form.error(ApiKeyService.PROGRAMS_FIELD_GROUP_NAME).isPresent())
                .orElse(false))
        .programCheckboxes(buildProgramCheckboxes(programNames, maybeForm))
        .build();
  }

  private String fieldValue(Optional<DynamicForm> maybeForm, String key) {
    return maybeForm.flatMap(form -> form.value(key)).map(String::valueOf).orElse("");
  }

  // ApiKeyService adds these errors as literal English, and this page is English only,
  // so the outline the legacy view took from toast.errorMessageOutline is just a
  // prefix here.
  private String fieldError(Optional<DynamicForm> maybeForm, String key) {
    return maybeForm
        .flatMap(form -> form.error(key))
        .map(error -> "Error: " + error.message())
        .orElse(null);
  }

  private ImmutableList<ProgramCheckbox> buildProgramCheckboxes(
      ImmutableSet<String> programNames, Optional<DynamicForm> maybeForm) {
    return programNames.stream()
        .sorted(String::compareToIgnoreCase)
        .map(
            name -> {
              String slug = MainModule.SLUGIFIER.slugify(name);
              String fieldName = "grant-program-read[" + slug + "]";

              return ProgramCheckbox.builder()
                  .id(slug)
                  .name(fieldName)
                  .label(name)
                  .checked(maybeForm.map(form -> form.value(fieldName).isPresent()).orElse(false))
                  .build();
            })
        .collect(ImmutableList.toImmutableList());
  }
}
