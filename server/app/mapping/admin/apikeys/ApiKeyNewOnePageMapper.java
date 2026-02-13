package mapping.admin.apikeys;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import modules.MainModule;
import play.data.DynamicForm;
import services.apikey.ApiKeyService;
import views.admin.apikeys.ApiKeyNewOnePageViewModel;
import views.admin.apikeys.ApiKeyNewOnePageViewModel.ProgramCheckboxData;

/** Maps data to the ApiKeyNewOnePageViewModel. */
public final class ApiKeyNewOnePageMapper {

  public ApiKeyNewOnePageViewModel map(
      ImmutableSet<String> programNames, Optional<DynamicForm> maybeDynamicForm) {
    ImmutableList<ProgramCheckboxData> programs =
        programNames.stream()
            .sorted(String::compareToIgnoreCase)
            .map(
                name -> {
                  String slug = MainModule.SLUGIFIER.slugify(name);
                  String fieldName = "grant-program-read[" + slug + "]";
                  boolean checked =
                      maybeDynamicForm.map(form -> form.value(fieldName).isPresent()).orElse(false);
                  return new ProgramCheckboxData(name, slug, fieldName, checked);
                })
            .collect(ImmutableList.toImmutableList());

    Optional<String> keyNameError =
        maybeDynamicForm.flatMap(
            form ->
                form.error(ApiKeyService.FORM_FIELD_NAME_KEY_NAME).map(e -> e.messages().get(0)));
    Optional<String> expirationError =
        maybeDynamicForm.flatMap(
            form ->
                form.error(ApiKeyService.FORM_FIELD_NAME_EXPIRATION).map(e -> e.messages().get(0)));
    Optional<String> subnetError =
        maybeDynamicForm.flatMap(
            form -> form.error(ApiKeyService.FORM_FIELD_NAME_SUBNET).map(e -> e.messages().get(0)));
    Optional<String> programsError =
        maybeDynamicForm.flatMap(
            form ->
                form.error(ApiKeyService.PROGRAMS_FIELD_GROUP_NAME)
                    .map(e -> "Error: You must select at least one program."));

    return ApiKeyNewOnePageViewModel.builder()
        .hasPrograms(!programNames.isEmpty())
        .programs(programs)
        .keyNameValue(
            maybeDynamicForm
                .flatMap(form -> form.value(ApiKeyService.FORM_FIELD_NAME_KEY_NAME))
                .map(String::valueOf)
                .orElse(""))
        .expirationValue(
            maybeDynamicForm
                .flatMap(form -> form.value(ApiKeyService.FORM_FIELD_NAME_EXPIRATION))
                .map(String::valueOf)
                .orElse(""))
        .subnetValue(
            maybeDynamicForm
                .flatMap(form -> form.value(ApiKeyService.FORM_FIELD_NAME_SUBNET))
                .map(String::valueOf)
                .orElse(""))
        .keyNameError(keyNameError)
        .expirationError(expirationError)
        .subnetError(subnetError)
        .programsError(programsError)
        .build();
  }
}
