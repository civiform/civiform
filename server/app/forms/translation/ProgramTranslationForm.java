package forms.translation;

import static com.google.common.base.Preconditions.checkNotNull;

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
      ProgramDefinition program, Locale locale, FormFactory formFactory) {
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
      formValuesBuilder.put(
          statusEmailFieldName(i),
          new String[] {
            status
                .localizedEmailBodyText()
                .orElse(LocalizedStrings.empty())
                .maybeGet(locale)
                .orElse("")
          });
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
      Http.Request request, FormFactory formFactory, int maxStatusTranslations) {
    // We limit the number of status entries read from the form data to that of the
    // current configured set of statuses.
    DynamicForm form =
        formFactory
            .form()
            .bindFromRequest(request, allFieldNames(maxStatusTranslations).toArray(new String[0]));
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

  private Optional<String> getStringFormField(String fieldName) {
    return Optional.ofNullable(form.rawData().get(fieldName));
  }

  public LocalizationUpdate getUpdateData() {
    return LocalizationUpdate.builder()
        .setLocalizedDisplayName(getStringFormField(DISPLAY_NAME_FORM_NAME).orElse(""))
        .setLocalizedDisplayDescription(
            getStringFormField(DISPLAY_DESCRIPTION_FORM_NAME).orElse(""))
        .setStatuses(getStatusUpdates())
        .build();
  }

  private ImmutableList<LocalizationUpdate.StatusUpdate> getStatusUpdates() {
    return IntStream.range(0, maxStatusTranslations)
        .boxed()
        .map(
            i -> {
              Optional<String> maybeConfiguredStatusText =
                  getStringFormField(configuredStatusFieldName(i));
              Optional<String> maybeStatusText = getStringFormField(statusTextFieldName(i));
              Optional<String> maybeEmail = getStringFormField(statusEmailFieldName(i));
              if (maybeConfiguredStatusText.isEmpty() || maybeStatusText.isEmpty()) {
                return Optional.empty();
              }

              LocalizationUpdate.StatusUpdate.Builder resultBuilder =
                  LocalizationUpdate.StatusUpdate.builder()
                      .setConfiguredStatusText(maybeConfiguredStatusText.get());
              if (!maybeStatusText.get().isEmpty()) {
                resultBuilder.setLocalizedStatusText(maybeStatusText);
              }
              if (maybeEmail.isPresent() && !maybeEmail.get().isEmpty()) {
                resultBuilder.setLocalizedEmailBody(maybeEmail);
              }
              return Optional.of(resultBuilder.build());
            })
        .filter(maybeStatusUpdate -> maybeStatusUpdate.isPresent())
        .map(maybeStatusUpdate -> (LocalizationUpdate.StatusUpdate) maybeStatusUpdate.get())
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
