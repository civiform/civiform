package services.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static services.settings.SettingsService.CIVIFORM_SETTINGS_ATTRIBUTE_KEY;
import static support.FakeRequestBuilder.fakeRequest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.Optional;
import org.junit.Test;
import play.libs.typedmap.TypedMap;
import play.mvc.Http;

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

  private static Http.Request REQUEST =
      fakeRequest()
          .withAttrs(
              TypedMap.empty()
                  .put(CIVIFORM_SETTINGS_ATTRIBUTE_KEY, ImmutableMap.of("BOOL_VARIABLE", "true")));

  private static Http.Request REQUEST_WITH_OVERRIDES =
      fakeRequest()
          .withAttrs(
              TypedMap.empty()
                  .put(
                      CIVIFORM_SETTINGS_ATTRIBUTE_KEY,
                      ImmutableMap.of(
                          "BOOL_VARIABLE", "true",
                          "STRING_VARIABLE", "overridden-var",
                          "INT_VARIABLE", "42",
                          "LIST_OF_STRINGS_VARIABLE", "a,b,c")));

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

  @Test
  public void gettingSections() {
    assertThat(testManifest.getSections()).isEqualTo(SECTIONS);
  }

  @Test
  public void getSettingDisplayValue() {
    assertThat(testManifest.getSettingDisplayValue(REQUEST, STRING_VARIABLE))
        .isEqualTo(Optional.of("my-var"));
    assertThat(testManifest.getSettingDisplayValue(REQUEST, INT_VARIABLE))
        .isEqualTo(Optional.of("11"));
    assertThat(testManifest.getSettingDisplayValue(REQUEST, LIST_OF_STRINGS_VARIABLE))
        .isEqualTo(Optional.of("one, two, three"));
    assertThat(testManifest.getSettingDisplayValue(REQUEST, BOOL_VARIABLE))
        .isEqualTo(Optional.of("TRUE"));
    assertThat(testManifest.getSettingDisplayValue(REQUEST, ENUM_VARIABLE))
        .isEqualTo(Optional.of("foo"));
    assertThat(testManifest.getSettingDisplayValue(REQUEST, UNSET_STRING_VARIABLE))
        .isEqualTo(Optional.empty());
  }

  @Test
  public void getBool_noAttrsInRequest_returnsHoconValue() {
    assertThat(testManifest.getBool("BOOL_VARIABLE", fakeRequest())).isFalse();
  }

  @Test
  public void getBool_withWritableSettingsOverride_returnsOverriddenValue() {
    assertThat(testManifest.getBool("BOOL_VARIABLE", REQUEST)).isTrue();
  }

  @Test
  public void getBool_withNoRequest_returnsHoconValue() {
    assertThat(testManifest.getBool("BOOL_VARIABLE")).isFalse();
  }

  @Test
  public void getBool_missingKey_returnsFalse() {
    assertThat(testManifest.getBool("NONEXISTENT_VARIABLE")).isFalse();
  }

  @Test
  public void getString_noAttrsInRequest_returnsHoconValue() {
    assertThat(testManifest.getSettingDisplayValue(fakeRequest(), STRING_VARIABLE))
        .isEqualTo(Optional.of("my-var"));
  }

  @Test
  public void getString_withWritableSettingsOverride_returnsOverriddenValue() {
    assertThat(testManifest.getSettingDisplayValue(REQUEST_WITH_OVERRIDES, STRING_VARIABLE))
        .isEqualTo(Optional.of("overridden-var"));
  }

  @Test
  public void getInt_noAttrsInRequest_returnsHoconValue() {
    assertThat(testManifest.getSettingDisplayValue(fakeRequest(), INT_VARIABLE))
        .isEqualTo(Optional.of("11"));
  }

  @Test
  public void getInt_withWritableSettingsOverride_returnsOverriddenValue() {
    assertThat(testManifest.getSettingDisplayValue(REQUEST_WITH_OVERRIDES, INT_VARIABLE))
        .isEqualTo(Optional.of("42"));
  }

  @Test
  public void getListOfStrings_noAttrsInRequest_returnsHoconValue() {
    assertThat(testManifest.getSettingDisplayValue(fakeRequest(), LIST_OF_STRINGS_VARIABLE))
        .isEqualTo(Optional.of("one, two, three"));
  }

  @Test
  public void getListOfStrings_withWritableSettingsOverride_returnsOverriddenValue() {
    assertThat(
            testManifest.getSettingDisplayValue(REQUEST_WITH_OVERRIDES, LIST_OF_STRINGS_VARIABLE))
        .isEqualTo(Optional.of("a, b, c"));
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
    var flags = testManifest.getAllFeatureFlagsSorted(REQUEST);
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
                    "Fake section.",
                    ImmutableList.of(),
                    ImmutableList.of(BOOL_VARIABLE))),
            CONFIG);
    assertThat(manifestWithoutFlags.getAllFeatureFlagsSorted(REQUEST)).isEmpty();
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
