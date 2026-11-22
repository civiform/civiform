package services.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Optional;
import models.SettingsGroupModel;
import org.junit.Test;
import repository.SettingsGroupRepository;

public class SettingsManifestTest {

  private static SettingDescription BOOL_VARIABLE =
      SettingDescription.create(
          "BOOL_VARIABLE",
          "Fake subsection variable for testing",
          true,
          SettingType.BOOLEAN,
          SettingMode.ADMIN_READABLE);
  private static SettingDescription STRING_VARIABLE =
      SettingDescription.create(
          "STRING_VARIABLE",
          "Fake subsection variable for testing",
          true,
          SettingType.STRING,
          SettingMode.ADMIN_READABLE);
  private static SettingDescription ENUM_VARIABLE =
      SettingDescription.create(
          "ENUM_VARIABLE",
          "Fake subsection variable for testing",
          true,
          SettingType.ENUM,
          SettingMode.ADMIN_READABLE,
          ImmutableList.of("foo", "bar", "baz"));
  private static SettingDescription LIST_OF_STRINGS_VARIABLE =
      SettingDescription.create(
          "LIST_OF_STRINGS_VARIABLE",
          "Fake subsection variable for testing",
          true,
          SettingType.LIST_OF_STRINGS,
          SettingMode.ADMIN_READABLE);
  private static SettingDescription INT_VARIABLE =
      SettingDescription.create(
          "INT_VARIABLE",
          "Fake subsection variable for testing",
          true,
          SettingType.INT,
          SettingMode.ADMIN_READABLE);
  private static SettingDescription UNSET_STRING_VARIABLE =
      SettingDescription.create(
          "UNSET_STRING_VARIABLE",
          "Fake subsection variable for testing",
          true,
          SettingType.INT,
          SettingMode.ADMIN_READABLE);
  private static SettingDescription ADMIN_WRITEABLE_VARIABLE =
      SettingDescription.create(
          "ADMIN_WRITEABLE_VARIABLE",
          "Fake admin writeable variable for testing",
          true,
          SettingType.STRING,
          SettingMode.ADMIN_WRITEABLE);
  private static SettingDescription FEATURE_FLAG_VARIABLE =
      SettingDescription.create(
          "FEATURE_FLAG_VARIABLE",
          "Fake feature flag for testing",
          true,
          SettingType.BOOLEAN,
          SettingMode.ADMIN_WRITEABLE);

  private static Config CONFIG =
      ConfigFactory.parseMap(
          ImmutableMap.<String, Object>builder()
              .put("bool_variable", false)
              .put("string_variable", "my-var")
              .put("list_of_strings_variable", ImmutableList.of("one", "two", "three"))
              .put("int_variable", 11)
              .put("enum_variable", "foo")
              .put("admin_writeable_variable", "admin-val")
              .put("feature_flag_variable", true)
              .build());

  private static ImmutableMap<String, SettingsSection> SECTIONS =
      ImmutableMap.of(
          "TEST_SECTION",
          SettingsSection.create(
              "Test Section",
              "Fake section for testing.",
              ImmutableList.of(
                  SettingsSection.create(
                      "Test Subsection",
                      "Fake subsection for testing",
                      ImmutableList.of(),
                      ImmutableList.of(BOOL_VARIABLE))),
              ImmutableList.of(
                  SettingDescription.create(
                      "STRING_VARIABLE",
                      "Fake string variable for testing",
                      true,
                      SettingType.STRING,
                      SettingMode.ADMIN_READABLE),
                  ADMIN_WRITEABLE_VARIABLE)),
          AbstractSettingsManifest.FEATURE_FLAG_SETTING_SECTION_NAME,
          SettingsSection.create(
              "Feature Flags",
              "Fake feature flags section for testing.",
              ImmutableList.of(),
              ImmutableList.of(FEATURE_FLAG_VARIABLE)));
  private SettingsManifest testManifest = new SettingsManifest(SECTIONS, CONFIG);

  private SettingsManifest manifestWithStoredBool =
      manifestWithStoredSettings(
          Optional.of(new SettingsGroupModel(ImmutableMap.of("BOOL_VARIABLE", "true"), "test")));

  private SettingsManifest manifestWithStoredOverrides =
      manifestWithStoredSettings(
          Optional.of(
              new SettingsGroupModel(
                  ImmutableMap.of(
                      "BOOL_VARIABLE", "true",
                      "STRING_VARIABLE", "overridden-var",
                      "INT_VARIABLE", "42",
                      "LIST_OF_STRINGS_VARIABLE", "a,b,c"),
                  "test")));

  private static SettingsManifest manifestWithStoredSettings(
      Optional<SettingsGroupModel> settingsGroup) {
    SettingsGroupRepository repository = mock(SettingsGroupRepository.class);
    when(repository.getCurrentSettingsSync()).thenReturn(settingsGroup);

    return new SettingsManifest(SECTIONS, CONFIG, () -> repository);
  }

  @Test
  public void gettingSections() {
    assertThat(testManifest.getSections()).isEqualTo(SECTIONS);
  }

  @Test
  public void getSettingDisplayValue() {
    assertThat(manifestWithStoredBool.getSettingDisplayValue(STRING_VARIABLE))
        .isEqualTo(Optional.of("my-var"));
    assertThat(manifestWithStoredBool.getSettingDisplayValue(INT_VARIABLE))
        .isEqualTo(Optional.of("11"));
    assertThat(manifestWithStoredBool.getSettingDisplayValue(LIST_OF_STRINGS_VARIABLE))
        .isEqualTo(Optional.of("one, two, three"));
    assertThat(manifestWithStoredBool.getSettingDisplayValue(BOOL_VARIABLE))
        .isEqualTo(Optional.of("TRUE"));
    assertThat(manifestWithStoredBool.getSettingDisplayValue(ENUM_VARIABLE))
        .isEqualTo(Optional.of("foo"));
    assertThat(manifestWithStoredBool.getSettingDisplayValue(UNSET_STRING_VARIABLE))
        .isEqualTo(Optional.empty());
  }

  @Test
  public void getWriteableBool_configOnlyManifest_returnsHoconValue() {
    assertThat(testManifest.getWriteableBool("BOOL_VARIABLE")).isFalse();
  }

  @Test
  public void getWriteableBool_noStoredSettingsGroup_returnsHoconValue() {
    SettingsManifest manifest = manifestWithStoredSettings(Optional.empty());

    assertThat(manifest.getWriteableBool("BOOL_VARIABLE")).isFalse();
  }

  @Test
  public void getWriteableBool_settingNotInStoredSettings_returnsHoconValue() {
    assertThat(manifestWithStoredBool.getWriteableBool("FEATURE_FLAG_VARIABLE")).isTrue();
  }

  @Test
  public void getWriteableBool_withStoredSetting_returnsStoredValue() {
    assertThat(manifestWithStoredBool.getWriteableBool("BOOL_VARIABLE")).isTrue();
  }

  @Test
  public void getBool_withStoredSetting_returnsHoconValue() {
    assertThat(manifestWithStoredBool.getBool("BOOL_VARIABLE")).isFalse();
  }

  @Test
  public void getBool_missingKey_returnsFalse() {
    assertThat(testManifest.getBool("NONEXISTENT_VARIABLE")).isFalse();
  }

  @Test
  public void getString_noStoredSettings_returnsHoconValue() {
    assertThat(testManifest.getSettingDisplayValue(STRING_VARIABLE))
        .isEqualTo(Optional.of("my-var"));
  }

  @Test
  public void getString_withStoredSetting_returnsStoredValue() {
    assertThat(manifestWithStoredOverrides.getSettingDisplayValue(STRING_VARIABLE))
        .isEqualTo(Optional.of("overridden-var"));
  }

  @Test
  public void getInt_noStoredSettings_returnsHoconValue() {
    assertThat(testManifest.getSettingDisplayValue(INT_VARIABLE)).isEqualTo(Optional.of("11"));
  }

  @Test
  public void getInt_withStoredSetting_returnsStoredValue() {
    assertThat(manifestWithStoredOverrides.getSettingDisplayValue(INT_VARIABLE))
        .isEqualTo(Optional.of("42"));
  }

  @Test
  public void getListOfStrings_noStoredSettings_returnsHoconValue() {
    assertThat(testManifest.getSettingDisplayValue(LIST_OF_STRINGS_VARIABLE))
        .isEqualTo(Optional.of("one, two, three"));
  }

  @Test
  public void getListOfStrings_withStoredSetting_returnsStoredValue() {
    assertThat(manifestWithStoredOverrides.getSettingDisplayValue(LIST_OF_STRINGS_VARIABLE))
        .isEqualTo(Optional.of("a, b, c"));
  }

  @Test
  public void storedSettings_areReadOnEachCall() {
    SettingsGroupRepository repository = mock(SettingsGroupRepository.class);
    when(repository.getCurrentSettingsSync())
        .thenReturn(
            Optional.of(new SettingsGroupModel(ImmutableMap.of("BOOL_VARIABLE", "true"), "test")))
        .thenReturn(
            Optional.of(new SettingsGroupModel(ImmutableMap.of("BOOL_VARIABLE", "false"), "test")));
    SettingsManifest manifest = new SettingsManifest(SECTIONS, CONFIG, () -> repository);

    assertThat(manifest.getWriteableBool("BOOL_VARIABLE")).isTrue();
    assertThat(manifest.getWriteableBool("BOOL_VARIABLE")).isFalse();
  }

  @Test
  public void getSettingSerializationValue_bool() {
    assertThat(testManifest.getSettingSerializationValue(BOOL_VARIABLE))
        .isEqualTo(Optional.of("false"));
  }

  @Test
  public void getSettingSerializationValue_string() {
    assertThat(testManifest.getSettingSerializationValue(STRING_VARIABLE))
        .isEqualTo(Optional.of("my-var"));
  }

  @Test
  public void getSettingSerializationValue_int() {
    assertThat(testManifest.getSettingSerializationValue(INT_VARIABLE))
        .isEqualTo(Optional.of("11"));
  }

  @Test
  public void getSettingSerializationValue_enum() {
    assertThat(testManifest.getSettingSerializationValue(ENUM_VARIABLE))
        .isEqualTo(Optional.of("foo"));
  }

  @Test
  public void getSettingSerializationValue_listOfStrings() {
    assertThat(testManifest.getSettingSerializationValue(LIST_OF_STRINGS_VARIABLE))
        .isEqualTo(Optional.of("one,two,three"));
  }

  @Test
  public void getSettingSerializationValue_unsetVariable_returnsEmpty() {
    assertThat(testManifest.getSettingSerializationValue(UNSET_STRING_VARIABLE))
        .isEqualTo(Optional.empty());
  }

  @Test
  public void getAllFeatureFlagsSorted_returnsFeatureFlagsOnly() {
    var flags = testManifest.getAllFeatureFlagsSorted();
    assertThat(flags).containsKey("FEATURE_FLAG_VARIABLE");
    assertThat(flags.get("FEATURE_FLAG_VARIABLE")).isTrue();
    assertThat(flags).doesNotContainKey("BOOL_VARIABLE");
    assertThat(flags).doesNotContainKey("STRING_VARIABLE");
  }

  @Test
  public void getAllFeatureFlagsSorted_noFeatureFlagsSection_returnsEmpty() {
    SettingsManifest manifestWithoutFlags =
        new SettingsManifest(
            ImmutableMap.of(
                "TEST_SECTION",
                SettingsSection.create(
                    "Test Section",
                    "Fake section",
                    ImmutableList.of(),
                    ImmutableList.of(BOOL_VARIABLE))),
            CONFIG);
    assertThat(manifestWithoutFlags.getAllFeatureFlagsSorted()).isEmpty();
  }

  @Test
  public void getAllSettingDescriptions_flattensSectionsAndSubsections() {
    var descriptions = testManifest.getAllSettingDescriptions();
    var variableNames =
        descriptions.stream()
            .map(SettingDescription::variableName)
            .collect(ImmutableList.toImmutableList());
    // From TEST_SECTION subsection
    assertThat(variableNames).contains("BOOL_VARIABLE");
    // From TEST_SECTION top-level
    assertThat(variableNames).contains("STRING_VARIABLE");
    // From Feature Flags section
    assertThat(variableNames).contains("FEATURE_FLAG_VARIABLE");
    // ADMIN_WRITEABLE_VARIABLE is in TEST_SECTION top-level
    assertThat(variableNames).contains("ADMIN_WRITEABLE_VARIABLE");
  }

  @Test
  public void getAllAdminWriteableSettingDescriptions_filtersToWriteableOnly() {
    var descriptions = testManifest.getAllAdminWriteableSettingDescriptions();
    var variableNames =
        descriptions.stream()
            .map(SettingDescription::variableName)
            .collect(ImmutableList.toImmutableList());
    assertThat(variableNames).contains("ADMIN_WRITEABLE_VARIABLE");
    assertThat(variableNames).contains("FEATURE_FLAG_VARIABLE");
    assertThat(variableNames).doesNotContain("BOOL_VARIABLE");
    assertThat(variableNames).doesNotContain("STRING_VARIABLE");
  }
}
