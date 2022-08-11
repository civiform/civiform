package forms.translation;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.IntStream;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.i18n.Lang;
import play.libs.typedmap.TypedMap;
import play.mvc.Http;
import services.LocalizedStrings;
import services.program.LocalizationUpdate;
import services.program.ProgramDefinition;
import services.program.StatusDefinitions;

/**
 * Form for updating translations for programs. This isn't a typical Play form in that the number of
 * fields is dynamic (based on the collection of {@link StatusDefinitions.Status} associated with
 * the program). Rather than binding using formFactory.form(ProgramTranslationForm.class), use
 * ProgramTranslationForm.bindFromRequest()
 */
public final class ProgramTranslationForm {

  public static final String DISPLAY_NAME_FORM_NAME = "displayName";
  public static final String DISPLAY_DESCRIPTION_FORM_NAME = "displayDescription";

  private final DynamicForm form;
  private final int maxStatusTranslations;

  private static final Lang DEFAULT_LANG = new Lang(LocalizedStrings.DEFAULT_LOCALE);

  public static ProgramTranslationForm fromProgram(
      FormFactory formFactory, Locale locale, ProgramDefinition program) {
    ImmutableMap.Builder<String, String[]> formValuesBuilder =
        ImmutableMap.<String, String[]>builder()
            .put(
                DISPLAY_NAME_FORM_NAME,
                new String[] {program.localizedName().maybeGet(locale).orElse("")})
            .put(
                DISPLAY_DESCRIPTION_FORM_NAME,
                new String[] {program.localizedDescription().maybeGet(locale).orElse("")});
    ImmutableList<StatusDefinitions.Status> statuses = program.statusDefinitions().getStatuses();
    for (int i = 0; i < statuses.size(); i++) {
      StatusDefinitions.Status status = statuses.get(i);
      formValuesBuilder.put(configuredStatusFieldName(i), new String[] {status.statusText()});
      formValuesBuilder.put(
          statusTextFieldName(i),
          new String[] {status.localizedStatusText().maybeGet(locale).orElse("")});
      if (status.localizedEmailBodyText().isPresent()) {
        formValuesBuilder.put(
            statusEmailFieldName(i),
            new String[] {status.localizedEmailBodyText().get().maybeGet(locale).orElse("")});
      }
    }
    DynamicForm form =
        formFactory
            .form()
            .bindFromRequestData(
                DEFAULT_LANG,
                TypedMap.empty(),
                formValuesBuilder.build(),
                ImmutableMap.of(),
                allFieldNames(statuses.size()).toArray(new String[0]));
    return new ProgramTranslationForm(form, program.statusDefinitions().getStatuses().size());
  }

  public static ProgramTranslationForm bindFromRequest(
      FormFactory formFactory, Http.Request request, int maxStatusTranslations) {
    // We limit the number of status entries read from the form data to that of the
    // current configured set of statuses.
    DynamicForm form =
        formFactory
            .form()
            .bindFromRequest(request, allFieldNames(maxStatusTranslations).toArray(new String[0]));
    if (form.hasErrors()) {
      // This case is never expected since our form has no validation constraints on it.
      throw new RuntimeException(
          String.format("unexpected form errors: %s", form.errorsAsJson().toString()));
    }
    return new ProgramTranslationForm(form, maxStatusTranslations);
  }

  private static ImmutableList<String> allFieldNames(int maxStatusTranslations) {
    ImmutableList.Builder<String> builder =
        ImmutableList.<String>builder().add(DISPLAY_NAME_FORM_NAME, DISPLAY_DESCRIPTION_FORM_NAME);
    for (int i = 0; i < maxStatusTranslations; i++) {
      builder.add(configuredStatusFieldName(i), statusTextFieldName(i), statusEmailFieldName(i));
    }
    return builder.build();
  }

  private ProgramTranslationForm(DynamicForm form, int maxStatusTranslations) {
    this.form = checkNotNull(form);
    this.maxStatusTranslations = maxStatusTranslations;
  }

  private String getStringFormField(String fieldName) {
    // TODO(clouser): Maybe Optional<String>
    String result = form.rawData().getOrDefault(fieldName, "");
    return result != null ? result : "";
  }

  public LocalizationUpdate getUpdateData() {
    return LocalizationUpdate.builder()
        .setLocalizedDisplayName(getStringFormField(DISPLAY_NAME_FORM_NAME))
        .setLocalizedDisplayDescription(getStringFormField(DISPLAY_DESCRIPTION_FORM_NAME))
        .setStatuses(getStatusUpdates())
        .build();
  }

  private ImmutableList<LocalizationUpdate.StatusUpdate> getStatusUpdates() {
    return IntStream.range(0, maxStatusTranslations)
        .boxed()
        .map(
            i -> {
              LocalizationUpdate.StatusUpdate.Builder resultBuilder =
                  LocalizationUpdate.StatusUpdate.builder()
                      .setConfiguredStatusText(getStringFormField(configuredStatusFieldName(i)));
              String newStatusText = getStringFormField(statusTextFieldName(i));
              if (!Strings.isNullOrEmpty(newStatusText)) {
                resultBuilder.setLocalizedStatusText(Optional.of(newStatusText));
              }
              String newStatusEmail = getStringFormField(statusEmailFieldName(i));
              if (!Strings.isNullOrEmpty(newStatusEmail)) {
                resultBuilder.setLocalizedEmailBody(Optional.of(newStatusEmail));
              }
              return resultBuilder.build();
            })
        .collect(ImmutableList.toImmutableList());
  }

  public static String configuredStatusFieldName(int index) {
    return String.format("status-%d-configured", index);
  }

  public static String statusTextFieldName(int index) {
    return String.format("status-%d-statusText", index);
  }

  public static String statusEmailFieldName(int index) {
    return String.format("status-%d-email", index);
  }
}
