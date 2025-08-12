package forms;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import services.LocalizedStrings;
import services.question.QuestionSetting;
import services.question.SettingType;
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
      return new Setting();
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
    this.locationAddress =
        settings.stream()
            .filter(setting -> setting.settingType().equals(SettingType.NAME))
            .map(setting -> new Setting(setting.settingKey(), ""))
            .findFirst()
            .orElse(new Setting());
    this.locationAddress =
        settings.stream()
            .filter(setting -> setting.settingType().equals(SettingType.ADDRESS))
            .map(setting -> new Setting(setting.settingKey(), ""))
            .findFirst()
            .orElse(new Setting());
    this.locationDetailsUrl =
        settings.stream()
            .filter(setting -> setting.settingType().equals(SettingType.URL))
            .map(setting -> new Setting(setting.settingKey(), ""))
            .findFirst()
            .orElse(new Setting());

    this.filters =
        settings.stream()
            .filter(setting -> setting.settingType().equals(SettingType.FILTER))
            .map(
                setting ->
                    new Setting(
                        setting.settingKey(),
                        setting.localizedSettingDisplayName().isPresent()
                            ? setting.localizedSettingDisplayName().get().getDefault()
                            : ""))
            .collect(Collectors.toList());
  }

  /** Converts form {@link Setting} to persistent {@link QuestionSetting} for database storage. */
  private ImmutableSet<QuestionSetting> buildQuestionSettings() {
    ImmutableSet.Builder<QuestionSetting> builder = ImmutableSet.builder();

    builder.add(
        QuestionSetting.create(getLocationName().getKey(), SettingType.NAME, Optional.empty()));

    builder.add(
        QuestionSetting.create(
            getLocationAddress().getKey(), SettingType.ADDRESS, Optional.empty()));

    builder.add(
        QuestionSetting.create(
            getLocationDetailsUrl().getKey(), SettingType.URL, Optional.empty()));

    for (Setting filter : getFilters()) {
      if (isValidSetting(filter)) {
        builder.add(
            QuestionSetting.create(
                filter.getKey(),
                SettingType.FILTER,
                Optional.of(LocalizedStrings.withDefaultValue(filter.getDisplayName()))));
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
