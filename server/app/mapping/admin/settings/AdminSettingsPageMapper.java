package mapping.admin.settings;

import static services.settings.AbstractSettingsManifest.FEATURE_FLAG_SETTING_SECTION_NAME;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Comparator;
import java.util.Optional;
import modules.MainModule;
import play.mvc.Http;
import services.settings.SettingDescription;
import services.settings.SettingsManifest;
import services.settings.SettingsSection;
import services.settings.SettingsService.SettingsGroupUpdateResult.UpdateError;
import views.CiviFormMarkdown;
import views.admin.settings.AdminSettingsPageViewModel;
import views.admin.settings.AdminSettingsPageViewModel.SettingData;
import views.admin.settings.AdminSettingsPageViewModel.SettingsSectionData;

/** Maps data to the AdminSettingsPageViewModel. */
public final class AdminSettingsPageMapper {

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

  public AdminSettingsPageViewModel map(
      Http.Request request,
      SettingsManifest settingsManifest,
      CiviFormMarkdown civiFormMarkdown,
      Optional<ImmutableMap<String, UpdateError>> errorMessages) {
    ImmutableMap<String, SettingsSection> sections = settingsManifest.getSections();

    ImmutableList<SettingsSectionData> sectionDataList =
        SECTIONS.stream()
            .filter(sections::containsKey)
            .map(
                sectionName ->
                    buildSectionData(
                        request,
                        errorMessages,
                        sections.get(sectionName),
                        settingsManifest,
                        civiFormMarkdown))
            .collect(ImmutableList.toImmutableList());

    return AdminSettingsPageViewModel.builder()
        .sections(sectionDataList)
        .errorMessages(errorMessages)
        .build();
  }

  private SettingsSectionData buildSectionData(
      Http.Request request,
      Optional<ImmutableMap<String, UpdateError>> errorMessages,
      SettingsSection section,
      SettingsManifest settingsManifest,
      CiviFormMarkdown civiFormMarkdown) {
    ImmutableList<SettingData> settings =
        section.settings().stream()
            .filter(SettingDescription::shouldDisplay)
            .sorted(Comparator.comparing(SettingDescription::variableName))
            .map(
                sd ->
                    buildSettingData(
                        request, errorMessages, sd, settingsManifest, civiFormMarkdown))
            .collect(ImmutableList.toImmutableList());

    ImmutableList<SettingsSectionData> subsections =
        section.subsections().stream()
            .filter(SettingsSection::shouldDisplay)
            .sorted(Comparator.comparing(SettingsSection::sectionName))
            .map(
                sub ->
                    buildSectionData(
                        request, errorMessages, sub, settingsManifest, civiFormMarkdown))
            .collect(ImmutableList.toImmutableList());

    return new SettingsSectionData(
        section.sectionName(),
        MainModule.SLUGIFIER.slugify(section.sectionName()),
        settings,
        subsections);
  }

  private SettingData buildSettingData(
      Http.Request request,
      Optional<ImmutableMap<String, UpdateError>> errorMessages,
      SettingDescription settingDescription,
      SettingsManifest settingsManifest,
      CiviFormMarkdown civiFormMarkdown) {
    String descriptionHtml = civiFormMarkdown.render(settingDescription.settingDescription());

    String value =
        settingsManifest
            .getSettingDisplayValue(request, settingDescription)
            .filter(v -> !v.isBlank())
            .orElse("");

    Optional<UpdateError> maybeError =
        errorMessages.flatMap(
            errors ->
                Optional.ofNullable(errors.getOrDefault(settingDescription.variableName(), null)));

    return new SettingData(
        settingDescription.variableName(),
        descriptionHtml,
        settingDescription.settingType().name(),
        value,
        settingDescription.isReadOnly(),
        settingDescription.allowableValues(),
        maybeError.map(UpdateError::errorMessage),
        maybeError.map(UpdateError::updatedValue));
  }
}
