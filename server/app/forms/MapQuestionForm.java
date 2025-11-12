package forms;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import services.LocalizedStrings;
import services.question.MapSettingType;
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
  @Setter private ArrayList<Setting> filters;
  @Setter private Setting locationTag;

  /**
   * Simple class for MAP question settings. Used for form processing and gets converted to {@link
   * QuestionSetting} for database storage.
   */
  @Getter
  @Setter
  public static final class Setting {
    private String key;
    private String value;
    private String displayName;
    private String alertText;

    public Setting() {
      this.key = "";
      this.value = "";
      this.displayName = "";
      this.alertText = "";
    }

    public Setting(String key, String displayName) {
      this.key = key;
      this.value = "";
      this.displayName = displayName;
      this.alertText = "";
    }

    public Setting(String key, String value, String displayName, String alertText) {
      this.key = key;
      this.value = value;
      this.displayName = displayName;
      this.alertText = alertText;
    }

    public static Setting emptySetting() {
      return new Setting();
    }

    public static List<Setting> emptyFilters() {
      return new ArrayList<>();
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
    this.filters = new ArrayList<>(Setting.emptyFilters());
    this.locationTag = Setting.emptySetting();
  }

  private void setFormWithQuestionSettings(ImmutableSet<QuestionSetting> settings) {
    this.locationName =
        getSettingFromQuestionSettings(settings, MapSettingType.LOCATION_NAME_GEO_JSON_KEY);
    this.locationAddress =
        getSettingFromQuestionSettings(settings, MapSettingType.LOCATION_ADDRESS_GEO_JSON_KEY);
    this.locationDetailsUrl =
        getSettingFromQuestionSettings(settings, MapSettingType.LOCATION_DETAILS_URL_GEO_JSON_KEY);

    this.filters = new ArrayList<>(getFiltersFromQuestionSettings(settings));
    this.locationTag =
        getSettingFromQuestionSettings(settings, MapSettingType.LOCATION_TAG_GEO_JSON_KEY);
  }

  /** Converts {@link QuestionSetting} back to form {@link Setting} for editing. */
  private Setting getSettingFromQuestionSettings(
      ImmutableSet<QuestionSetting> settings, MapSettingType type) {
    return settings.stream()
        .filter(setting -> setting.settingType().equals(type))
        .map(
            setting ->
                new Setting(
                    setting.settingKey(),
                    setting.settingValue().orElse(""),
                    setting.localizedSettingDisplayName().isPresent()
                        ? setting.localizedSettingDisplayName().get().getDefault()
                        : "",
                    setting.localizedSettingText().isPresent()
                        ? setting.localizedSettingText().get().getDefault()
                        : ""))
        .findFirst()
        .orElse(new Setting());
  }

  /** Converts {@link QuestionSetting} back to form {@link Setting} list for editing filters. */
  private List<Setting> getFiltersFromQuestionSettings(ImmutableSet<QuestionSetting> settings) {
    return settings.stream()
        .filter(
            setting -> setting.settingType().equals(MapSettingType.LOCATION_FILTER_GEO_JSON_KEY))
        .map(
            setting ->
                new Setting(
                    setting.settingKey(),
                    setting.localizedSettingDisplayName().isPresent()
                        ? setting.localizedSettingDisplayName().get().getDefault()
                        : ""))
        .collect(Collectors.toCollection(ArrayList::new));
  }

  /** Converts form {@link Setting} to persistent {@link QuestionSetting} for database storage. */
  private ImmutableSet<QuestionSetting> buildQuestionSettings() {
    ImmutableSet.Builder<QuestionSetting> builder = ImmutableSet.builder();

    builder.add(
        QuestionSetting.create(
            getLocationName().getKey(), MapSettingType.LOCATION_NAME_GEO_JSON_KEY));
    builder.add(
        QuestionSetting.create(
            getLocationAddress().getKey(), MapSettingType.LOCATION_ADDRESS_GEO_JSON_KEY));
    builder.add(
        QuestionSetting.create(
            getLocationDetailsUrl().getKey(), MapSettingType.LOCATION_DETAILS_URL_GEO_JSON_KEY));

    if (getLocationTag() != null && !getLocationTag().getKey().isEmpty()) {
      builder.add(
          QuestionSetting.create(
              getLocationTag().getKey(),
              MapSettingType.LOCATION_TAG_GEO_JSON_KEY,
              Optional.of(LocalizedStrings.withDefaultValue(getLocationTag().getDisplayName())),
              Optional.of(getLocationTag().getValue()),
              Optional.of(LocalizedStrings.withDefaultValue(getLocationTag().getAlertText()))));
    }

    getFilters()
        .forEach(
            filter ->
                builder.add(
                    QuestionSetting.create(
                        filter.getKey(),
                        MapSettingType.LOCATION_FILTER_GEO_JSON_KEY,
                        Optional.of(LocalizedStrings.withDefaultValue(filter.getDisplayName())))));
    return builder.build();
  }
}
