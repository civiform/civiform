package forms;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import services.LocalizedStrings;
import services.question.QuestionSetting;
import services.question.types.MapQuestionDefinition;
import services.question.types.MapQuestionDefinition.MapValidationPredicates;
import services.question.types.QuestionDefinitionBuilder;
import services.question.types.QuestionType;

/** Form for {@link QuestionType#MAP} question configuration and settings. */
@Getter
public class MapQuestionForm extends QuestionForm {

  @Setter private String geoJsonEndpoint;
  private OptionalInt maxLocationSelections;
  @Setter private Setting locationName;
  @Setter private Setting locationAddress;
  @Setter private Setting locationDetailsUrl;
  @Setter private List<Setting> filters;

  public static final String LOCATION_NAME_DISPLAY = "Name";
  public static final String LOCATION_ADDRESS_DISPLAY = "Address";
  public static final String LOCATION_DETAILS_URL_DISPLAY = "URL";
  private final List<String> DEFAULT_MAP_QUESTION_KEYS =
      Arrays.asList(LOCATION_NAME_DISPLAY, LOCATION_ADDRESS_DISPLAY, LOCATION_DETAILS_URL_DISPLAY);

  /**
   * Simple class for MAP question settings. Used for form processing and gets converted to {@link
   * QuestionSetting} for database storage.
   */
  @Getter
  @Setter
  public static final class Setting {
    private String key;
    private String displayName;

    public Setting() {
      this.key = "";
      this.displayName = "";
    }

    public Setting(String key, String displayName) {
      this.key = key;
      this.displayName = displayName;
    }

    public static Setting emptySetting() {
      return new Setting("", "");
    }

    public static Setting emptyKeyWithDisplayName(String displayName) {
      return new Setting("", displayName);
    }

    public static List<Setting> emptyFilters() {
      return Arrays.asList(emptySetting(), emptySetting(), emptySetting());
    }
  }

  public MapQuestionForm() {
    super();
    this.geoJsonEndpoint = "";
    this.maxLocationSelections = OptionalInt.empty();
    setFormWithDefaultQuestionSettings();
  }

  public MapQuestionForm(MapQuestionDefinition qd) {
    super(qd);
    this.geoJsonEndpoint = qd.getMapValidationPredicates().geoJsonEndpoint();
    this.maxLocationSelections = qd.getMapValidationPredicates().maxLocationSelections();

    if (qd.getQuestionSettings().isPresent()) {
      setFormWithQuestionSettings(qd.getQuestionSettings().get());
    } else {
      setFormWithDefaultQuestionSettings();
    }
  }

  @Override
  public QuestionType getQuestionType() {
    return QuestionType.MAP;
  }

  /**
   * Sets the maximum number of locations an applicant can select.
   *
   * <p>We use a string parameter here so that if the field is empty (i.e., unset), we can correctly
   * set it to an empty {@code OptionalInt}.
   *
   * @param maxLocationSelectionsAsString the max number of locations an applicant can select
   */
  public void setMaxLocationSelections(String maxLocationSelectionsAsString) {
    this.maxLocationSelections =
        maxLocationSelectionsAsString.isEmpty()
            ? OptionalInt.empty()
            : OptionalInt.of(Integer.parseInt(maxLocationSelectionsAsString));
  }

  @Override
  public QuestionDefinitionBuilder getBuilder() {
    MapValidationPredicates.Builder predicateBuilder = MapValidationPredicates.builder();

    predicateBuilder.setGeoJsonEndpoint(getGeoJsonEndpoint());

    if (getMaxLocationSelections().isPresent()) {
      predicateBuilder.setMaxLocationSelections(getMaxLocationSelections());
    }

    return super.getBuilder()
        .setValidationPredicates(predicateBuilder.build())
        .setQuestionSettings(buildQuestionSettings());
  }

  private void setFormWithDefaultQuestionSettings() {
    this.locationName = Setting.emptySetting();
    this.locationAddress = Setting.emptySetting();
    this.locationDetailsUrl = Setting.emptySetting();
    this.filters = Setting.emptyFilters();
  }

  /** Converts persistent {@link QuestionSetting} back to form {@link Setting} for editing. */
  private void setFormWithQuestionSettings(ImmutableSet<QuestionSetting> settings) {
    this.locationName =
        settings.stream()
            .filter(
                setting ->
                    setting
                        .localizedSettingDisplayName()
                        .getDefault()
                        .equals(LOCATION_NAME_DISPLAY))
            .findFirst()
            .map(
                setting ->
                    new Setting(
                        setting.settingKey(), setting.localizedSettingDisplayName().getDefault()))
            .orElse(Setting.emptySetting());
    this.locationAddress =
        settings.stream()
            .filter(
                setting ->
                    setting
                        .localizedSettingDisplayName()
                        .getDefault()
                        .equals(LOCATION_ADDRESS_DISPLAY))
            .findFirst()
            .map(
                setting ->
                    new Setting(
                        setting.settingKey(), setting.localizedSettingDisplayName().getDefault()))
            .orElse(Setting.emptySetting());
    this.locationDetailsUrl =
        settings.stream()
            .filter(
                setting ->
                    setting
                        .localizedSettingDisplayName()
                        .getDefault()
                        .equals(LOCATION_DETAILS_URL_DISPLAY))
            .findFirst()
            .map(
                setting ->
                    new Setting(
                        setting.settingKey(), setting.localizedSettingDisplayName().getDefault()))
            .orElse(Setting.emptySetting());

    this.filters =
        settings.stream()
            .filter(
                setting ->
                    !DEFAULT_MAP_QUESTION_KEYS.contains(
                        setting.localizedSettingDisplayName().getDefault()))
            .map(
                setting ->
                    new Setting(
                        setting.settingKey(), setting.localizedSettingDisplayName().getDefault()))
            .collect(Collectors.toList());
  }

  /** Converts form {@link Setting} to persistent {@link QuestionSetting} for database storage. */
  private ImmutableSet<QuestionSetting> buildQuestionSettings() {
    ImmutableSet.Builder<QuestionSetting> builder = ImmutableSet.builder();

    builder.add(
        QuestionSetting.create(
            getLocationName().key, LocalizedStrings.withDefaultValue(LOCATION_NAME_DISPLAY)));

    builder.add(
        QuestionSetting.create(
            getLocationAddress().getKey(),
            LocalizedStrings.withDefaultValue(LOCATION_ADDRESS_DISPLAY)));

    builder.add(
        QuestionSetting.create(
            getLocationDetailsUrl().getKey(),
            LocalizedStrings.withDefaultValue(LOCATION_DETAILS_URL_DISPLAY)));

    for (Setting filter : getFilters()) {
      if (isValidSetting(filter)) {
        builder.add(
            QuestionSetting.create(
                filter.getKey(), LocalizedStrings.withDefaultValue(filter.getDisplayName())));
      }
    }
    return builder.build();
  }

  private boolean isValidSetting(Setting isValidSetting) {
    return isValidSetting.getKey() != null
        && !isValidSetting.getKey().isEmpty()
        && isValidSetting.getDisplayName() != null
        && !isValidSetting.getDisplayName().isEmpty();
  }
}
