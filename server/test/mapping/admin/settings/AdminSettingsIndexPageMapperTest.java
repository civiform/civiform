package mapping.admin.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import services.settings.SettingDescription;
import services.settings.SettingMode;
import services.settings.SettingType;
import services.settings.SettingsManifest;
import services.settings.SettingsSection;
import services.settings.SettingsService.SettingsGroupUpdateResult.UpdateError;
import views.CiviFormMarkdown;
import views.admin.settings.AdminSettingsIndexPageViewModel;
import views.admin.settings.AdminSettingsIndexPageViewModel.FlashToast;
import views.admin.settings.AdminSettingsIndexPageViewModel.Section;
import views.admin.settings.AdminSettingsIndexPageViewModel.Setting;
import views.components.ToastMessage.ToastType;

public final class AdminSettingsIndexPageMapperTest {

  /** Every section the page renders, in display order. */
  private static final ImmutableList<String> SECTION_NAMES =
      ImmutableList.of(
          "Feature Flags",
          "Branding",
          "Custom Text",
          "Email Addresses",
          "Data Export API",
          "Observability",
          "External Services",
          "Session Management",
          "Miscellaneous",
          "Experimental");

  private SettingsManifest settingsManifest;
  private Http.Request request;
  private AdminSettingsIndexPageMapper mapper;

  @Before
  public void setUp() {
    settingsManifest = mock(SettingsManifest.class);
    request = mock(Http.Request.class);
    mapper = new AdminSettingsIndexPageMapper();

    setSections();
  }

  /** Stubs the manifest with every section empty, then applies the given sections over them. */
  private void setSections(SettingsSection... sections) {
    Map<String, SettingsSection> allSections = new LinkedHashMap<>();
    SECTION_NAMES.forEach(name -> allSections.put(name, section(name, ImmutableList.of())));
    for (SettingsSection section : sections) {
      allSections.put(section.sectionName(), section);
    }

    when(settingsManifest.getSections()).thenReturn(ImmutableMap.copyOf(allSections));
  }

  private static SettingsSection section(
      String name, ImmutableList<SettingDescription> settings, SettingsSection... subsections) {
    return SettingsSection.create(
        name, name + " description", ImmutableList.copyOf(subsections), settings);
  }

  private static SettingDescription setting(String variableName, SettingType type) {
    return setting(variableName, type, SettingMode.ADMIN_WRITEABLE);
  }

  private static SettingDescription setting(
      String variableName, SettingType type, SettingMode mode) {
    return SettingDescription.create(
        variableName, variableName + " description", /* isRequired= */ false, type, mode);
  }

  private void stubDisplayValue(SettingDescription settingDescription, String value) {
    when(settingsManifest.getSettingDisplayValue(any(), eq(settingDescription)))
        .thenReturn(Optional.of(value));
  }

  private AdminSettingsIndexPageViewModel map() {
    return map(/* errorMessages= */ Optional.empty());
  }

  private AdminSettingsIndexPageViewModel map(
      Optional<ImmutableMap<String, UpdateError>> errorMessages) {
    return mapper.map(
        request,
        settingsManifest,
        new CiviFormMarkdown(),
        errorMessages,
        /* successMessage= */ Optional.empty(),
        /* warningMessage= */ Optional.empty());
  }

  private static Setting onlySetting(AdminSettingsIndexPageViewModel model, String sectionName) {
    Section section =
        model.getSections().stream()
            .filter(candidate -> candidate.getName().equals(sectionName))
            .findFirst()
            .orElseThrow();
    return section.getSettings().get(0);
  }

  @Test
  public void map_setsFormActionUrl() {
    assertThat(map().getFormActionUrl())
        .isEqualTo(controllers.admin.routes.AdminSettingsController.update().url());
  }

  @Test
  public void map_setsSectionsInDisplayOrder() {
    ImmutableList<String> names =
        map().getSections().stream().map(Section::getName).collect(ImmutableList.toImmutableList());

    assertThat(names).isEqualTo(SECTION_NAMES);
  }

  @Test
  public void map_slugifiesSectionNames() {
    ImmutableList<String> slugs =
        map().getSections().stream().map(Section::getSlug).collect(ImmutableList.toImmutableList());

    assertThat(slugs).contains("feature-flags", "data-export-api", "session-management");
  }

  @Test
  public void map_omitsSettingsThatShouldNotDisplay() {
    setSections(
        section(
            "Branding",
            ImmutableList.of(
                setting("VISIBLE_SETTING", SettingType.STRING),
                setting("SECRET_SETTING", SettingType.STRING, SettingMode.SECRET),
                setting("HIDDEN_SETTING", SettingType.STRING, SettingMode.HIDDEN))));

    Setting onlyVisible = onlySetting(map(), "Branding");

    assertThat(onlyVisible.getVariableName()).isEqualTo("VISIBLE_SETTING");
  }

  @Test
  public void map_sortsSettingsByVariableName() {
    setSections(
        section(
            "Branding",
            ImmutableList.of(
                setting("ZULU", SettingType.STRING), setting("ALPHA", SettingType.STRING))));

    ImmutableList<String> names =
        map().getSections().get(1).getSettings().stream()
            .map(Setting::getVariableName)
            .collect(ImmutableList.toImmutableList());

    assertThat(names).containsExactly("ALPHA", "ZULU");
  }

  @Test
  public void map_sortsSubsectionsByNameAndOmitsEmptyOnes() {
    setSections(
        section(
            "Branding",
            ImmutableList.of(),
            section("Zulu", ImmutableList.of(setting("ZULU_SETTING", SettingType.STRING))),
            section("Alpha", ImmutableList.of(setting("ALPHA_SETTING", SettingType.STRING))),
            section(
                "Hidden",
                ImmutableList.of(setting("SECRET", SettingType.STRING, SettingMode.SECRET)))));

    ImmutableList<String> subsectionNames =
        map().getSections().get(1).getSubsections().stream()
            .map(Section::getName)
            .collect(ImmutableList.toImmutableList());

    assertThat(subsectionNames).containsExactly("Alpha", "Zulu");
  }

  @Test
  public void map_rendersSettingDescriptionAsMarkdown() {
    setSections(section("Branding", ImmutableList.of(setting("LOGO_URL", SettingType.STRING))));

    assertThat(onlySetting(map(), "Branding").getDescriptionHtml())
        .isEqualTo("<p>LOGO_URL description</p>\n");
  }

  @Test
  public void map_setsStringSettingValue() {
    SettingDescription logoUrl = setting("LOGO_URL", SettingType.STRING);
    setSections(section("Branding", ImmutableList.of(logoUrl)));
    stubDisplayValue(logoUrl, "https://example.com/logo.png");

    Setting setting = onlySetting(map(), "Branding");

    assertThat(setting.getInputTypeName()).isEqualTo("STRING");
    assertThat(setting.getValue()).isEqualTo("https://example.com/logo.png");
  }

  @Test
  public void map_setsListOfStringsSettingAsStringInput() {
    SettingDescription allowedHosts = setting("ALLOWED_HOSTS", SettingType.LIST_OF_STRINGS);
    setSections(section("Branding", ImmutableList.of(allowedHosts)));
    stubDisplayValue(allowedHosts, "a.example.com, b.example.com");

    Setting setting = onlySetting(map(), "Branding");

    assertThat(setting.getInputTypeName()).isEqualTo("STRING");
    assertThat(setting.getValue()).isEqualTo("a.example.com, b.example.com");
  }

  @Test
  public void map_treatsBlankDisplayValueAsUnset() {
    SettingDescription logoUrl = setting("LOGO_URL", SettingType.STRING);
    setSections(section("Branding", ImmutableList.of(logoUrl)));
    stubDisplayValue(logoUrl, "   ");

    assertThat(onlySetting(map(), "Branding").getValue()).isEmpty();
  }

  @Test
  public void map_stringSettingKeepsRejectedValueAndError() {
    SettingDescription themeColor = setting("THEME_COLOR_PRIMARY", SettingType.STRING);
    setSections(section("Branding", ImmutableList.of(themeColor)));
    stubDisplayValue(themeColor, "#01587d");
    ImmutableMap<String, UpdateError> errors =
        ImmutableMap.of("THEME_COLOR_PRIMARY", UpdateError.create("#19baff", "Bad contrast"));

    Setting setting = onlySetting(map(Optional.of(errors)), "Branding");

    assertThat(setting.getValue()).isEqualTo("#19baff");
    assertThat(setting.getErrorMessage()).isEqualTo("Bad contrast");
  }

  @Test
  public void map_setsBooleanSettingChecked() {
    SettingDescription flag = setting("MY_FLAG", SettingType.BOOLEAN);
    setSections(section("Feature Flags", ImmutableList.of(flag)));
    stubDisplayValue(flag, "TRUE");

    Setting setting = onlySetting(map(), "Feature Flags");

    assertThat(setting.getInputTypeName()).isEqualTo("BOOLEAN");
    assertThat(setting.isChecked()).isTrue();
  }

  @Test
  public void map_booleanSettingIsUncheckedWhenNotTrue() {
    SettingDescription flag = setting("MY_FLAG", SettingType.BOOLEAN);
    setSections(section("Feature Flags", ImmutableList.of(flag)));
    stubDisplayValue(flag, "FALSE");

    assertThat(onlySetting(map(), "Feature Flags").isChecked()).isFalse();
  }

  @Test
  public void map_setsEnumValueAndAllowableValues() {
    SettingDescription storageService =
        SettingDescription.create(
            "STORAGE_SERVICE_NAME",
            "Where files are stored",
            /* isRequired= */ false,
            SettingType.ENUM,
            SettingMode.ADMIN_WRITEABLE,
            ImmutableList.of("AWS", "AZURE"));
    setSections(section("External Services", ImmutableList.of(storageService)));
    stubDisplayValue(storageService, "AZURE");

    Setting setting = onlySetting(map(), "External Services");

    assertThat(setting.getInputTypeName()).isEqualTo("ENUM");
    assertThat(setting.getValue()).isEqualTo("AZURE");
    assertThat(setting.getAllowableValues()).containsExactly("AWS", "AZURE");
  }

  @Test
  public void map_setsIntValue() {
    SettingDescription timeout = setting("SESSION_TIMEOUT_MINUTES", SettingType.INT);
    setSections(section("Session Management", ImmutableList.of(timeout)));
    stubDisplayValue(timeout, "30");

    Setting setting = onlySetting(map(), "Session Management");

    assertThat(setting.getInputTypeName()).isEqualTo("INT");
    assertThat(setting.getValue()).isEqualTo("30");
  }

  @Test
  public void map_unsetIntValueIsNull() {
    setSections(
        section(
            "Session Management",
            ImmutableList.of(setting("SESSION_TIMEOUT_MINUTES", SettingType.INT))));

    assertThat(onlySetting(map(), "Session Management").getValue()).isNull();
  }

  @Test
  public void map_setsReadOnlyForSettingsAdminsCannotWrite() {
    setSections(
        section(
            "Miscellaneous",
            ImmutableList.of(
                setting("IMAGE_TAG", SettingType.STRING, SettingMode.ADMIN_READABLE))));

    assertThat(onlySetting(map(), "Miscellaneous").isReadOnly()).isTrue();
  }

  @Test
  public void map_setsErrorToastIdWhenTheUpdateFailed() {
    AdminSettingsIndexPageViewModel model =
        map(Optional.of(ImmutableMap.of("LOGO_URL", UpdateError.create("bad", "Invalid"))));

    assertThat(model.getErrorToastId()).isNotEmpty();
  }

  @Test
  public void map_hasNoErrorToastIdWithoutErrors() {
    assertThat(map().getErrorToastId()).isNull();
  }

  @Test
  public void map_mapsFlashMessagesToToasts() {
    AdminSettingsIndexPageViewModel model =
        mapper.map(
            request,
            settingsManifest,
            new CiviFormMarkdown(),
            /* errorMessages= */ Optional.empty(),
            Optional.of("Settings updated"),
            Optional.of("No changes to save"));

    assertThat(model.getFlashToasts())
        .extracting(FlashToast::getMessage, FlashToast::getType)
        .containsExactly(
            tuple("Settings updated", ToastType.SUCCESS),
            tuple("No changes to save", ToastType.WARNING));
  }

  @Test
  public void map_hasNoFlashToastsWithoutFlashMessages() {
    assertThat(map().getFlashToasts()).isEmpty();
  }
}
