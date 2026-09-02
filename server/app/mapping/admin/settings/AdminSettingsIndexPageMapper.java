package mapping.admin.settings;

import static services.settings.AbstractSettingsManifest.FEATURE_FLAG_SETTING_SECTION_NAME;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import controllers.admin.routes;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import modules.MainModule;
import play.mvc.Http;
import services.settings.SettingDescription;
import services.settings.SettingsManifest;
import services.settings.SettingsSection;
import services.settings.SettingsService.SettingsGroupUpdateResult.UpdateError;
import views.CiviFormMarkdown;
import views.admin.settings.AdminSettingsIndexPageViewModel;
import views.admin.settings.AdminSettingsIndexPageViewModel.FlashToast;
import views.admin.settings.AdminSettingsIndexPageViewModel.Section;
import views.admin.settings.AdminSettingsIndexPageViewModel.Setting;
import views.components.ToastMessage.ToastType;

/** Maps the server settings manifest to the {@link AdminSettingsIndexPageViewModel}. */
public final class AdminSettingsIndexPageMapper {

  /** The sections shown, in display order. Mirrors the legacy AdminSettingsIndexView.SECTIONS. */
  private static final ImmutableList<String> SECTIONS =
      ImmutableList.of(
          FEATURE_FLAG_SETTING_SECTION_NAME,
          "Branding",
          "Custom Text",
          "Email Addresses",
          "Data Export API",
          "Observability",
          "External Services",
          "Session Management",
          "Miscellaneous",
          "Experimental");

  /**
   * Maps the settings manifest to the view model.
   *
   * @param request the current request, which carries the writeable setting values
   * @param settingsManifest the manifest holding the sections and current setting values
   * @param civiFormMarkdown renders each setting's description to HTML
   * @param errorMessages validation errors from the last save attempt, keyed by variable name
   * @param successMessage the flashed success message, rendered as a toast
   * @param warningMessage the flashed warning message, rendered as a toast
   */
  public AdminSettingsIndexPageViewModel map(
      Http.Request request,
      SettingsManifest settingsManifest,
      CiviFormMarkdown civiFormMarkdown,
      Optional<ImmutableMap<String, UpdateError>> errorMessages,
      Optional<String> successMessage,
      Optional<String> warningMessage) {
    ImmutableMap<String, SettingsSection> allSections = settingsManifest.getSections();

    ImmutableList<Section> sections =
        SECTIONS.stream()
            .map(
                sectionName ->
                    mapSection(
                        request,
                        settingsManifest,
                        civiFormMarkdown,
                        errorMessages,
                        allSections.get(sectionName)))
            .collect(ImmutableList.toImmutableList());

    ImmutableList.Builder<FlashToast> flashToasts = ImmutableList.builder();
    successMessage.ifPresent(message -> flashToasts.add(flashToast(message, ToastType.SUCCESS)));
    warningMessage.ifPresent(message -> flashToasts.add(flashToast(message, ToastType.WARNING)));

    return AdminSettingsIndexPageViewModel.builder()
        .formActionUrl(routes.AdminSettingsController.update().url())
        .sections(sections)
        .flashToasts(flashToasts.build())
        // Legacy toasts were built with a random id.
        .errorToastId(errorMessages.isPresent() ? UUID.randomUUID().toString() : null)
        .build();
  }

  private static FlashToast flashToast(String message, ToastType type) {
    return FlashToast.builder()
        .id(UUID.randomUUID().toString())
        .message(message)
        .type(type)
        .build();
  }

  private static Section mapSection(
      Http.Request request,
      SettingsManifest settingsManifest,
      CiviFormMarkdown civiFormMarkdown,
      Optional<ImmutableMap<String, UpdateError>> errorMessages,
      SettingsSection settingsSection) {
    ImmutableList<Setting> settings =
        settingsSection.settings().stream()
            .filter(SettingDescription::shouldDisplay)
            .sorted(Comparator.comparing(SettingDescription::variableName))
            .map(
                settingDescription ->
                    mapSetting(
                        request,
                        settingsManifest,
                        civiFormMarkdown,
                        errorMessages,
                        settingDescription))
            .collect(ImmutableList.toImmutableList());

    ImmutableList<Section> subsections =
        settingsSection.subsections().stream()
            .filter(SettingsSection::shouldDisplay)
            .sorted(Comparator.comparing(SettingsSection::sectionName))
            .map(
                subsection ->
                    mapSection(
                        request, settingsManifest, civiFormMarkdown, errorMessages, subsection))
            .collect(ImmutableList.toImmutableList());

    return Section.builder()
        .name(settingsSection.sectionName())
        .slug(MainModule.SLUGIFIER.slugify(settingsSection.sectionName()))
        .settings(settings)
        .subsections(subsections)
        .build();
  }

  private static Setting mapSetting(
      Http.Request request,
      SettingsManifest settingsManifest,
      CiviFormMarkdown civiFormMarkdown,
      Optional<ImmutableMap<String, UpdateError>> errorMessages,
      SettingDescription settingDescription) {
    Optional<String> value =
        settingsManifest
            .getSettingDisplayValue(request, settingDescription)
            .filter(displayValue -> !displayValue.isBlank());
    Optional<UpdateError> updateError =
        errorMessages.flatMap(
            errors -> Optional.ofNullable(errors.get(settingDescription.variableName())));

    Setting.SettingBuilder setting =
        Setting.builder()
            .variableName(settingDescription.variableName())
            .descriptionHtml(civiFormMarkdown.render(settingDescription.settingDescription()))
            .readOnly(settingDescription.isReadOnly())
            .allowableValues(ImmutableList.of());

    // A switch expression so that a new SettingType is a compile error rather
    // than a setting that silently renders no input.
    return switch (settingDescription.settingType()) {
      case BOOLEAN ->
          setting
              .inputTypeName("BOOLEAN")
              .checked(value.map("TRUE"::equals).orElse(false))
              .errorMessage(updateError.map(UpdateError::errorMessage).orElse(null))
              .build();
      // String and list-of-strings settings shared one j2html builder, so they
      // share the string input.
      case LIST_OF_STRINGS, STRING ->
          setting
              .inputTypeName("STRING")
              .value(updateError.map(UpdateError::updatedValue).orElse(value.orElse("")))
              .errorMessage(updateError.map(UpdateError::errorMessage).orElse(null))
              .build();
      case ENUM ->
          setting
              .inputTypeName("ENUM")
              .value(value.orElse(""))
              .allowableValues(settingDescription.allowableValues().get())
              .build();
      // The legacy page parsed the display value as an int before rendering it,
      // and left the value attribute off entirely when the setting was unset.
      case INT ->
          setting
              .inputTypeName("INT")
              .value(value.map(Integer::parseInt).map(String::valueOf).orElse(null))
              .build();
    };
  }
}
