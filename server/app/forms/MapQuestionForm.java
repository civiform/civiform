package forms;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.Setter;
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
  @Setter private QuestionSetting locationName;
  @Setter private QuestionSetting locationAddress;
  @Setter private QuestionSetting locationDetailsUrl;
  @Setter private List<QuestionSetting> filters;

  public static final String LOCATION_NAME_DISPLAY = "Name";
  public static final String LOCATION_ADDRESS_DISPLAY = "Address";
  public static final String LOCATION_DETAILS_URL_DISPLAY = "URL";
  private static final List<String> DEFAULT_MAP_QUESTION_KEYS = Arrays.asList(LOCATION_NAME_DISPLAY, LOCATION_ADDRESS_DISPLAY, LOCATION_DETAILS_URL_DISPLAY);


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
    this.locationName = QuestionSettingUtils.emptySetting();
    this.locationAddress = QuestionSettingUtils.emptySetting();
    this.locationDetailsUrl = QuestionSettingUtils.emptySetting();
    this.filters = emptyFilters();
  }

  public static List<QuestionSetting> emptyFilters() {
    return ImmutableList.of(
        QuestionSettingUtils.emptySetting(),
        QuestionSettingUtils.emptySetting(),
        QuestionSettingUtils.emptySetting());
  }

  /** Populates specific map question form fields from persistent {@link QuestionSetting} objects. */
  private void setFormWithQuestionSettings(ImmutableSet<QuestionSetting> settings) {
    this.locationName = QuestionSettingUtils.findByDisplayName(settings, LOCATION_NAME_DISPLAY);
    this.locationAddress = QuestionSettingUtils.findByDisplayName(settings, LOCATION_ADDRESS_DISPLAY);
    this.locationDetailsUrl = QuestionSettingUtils.findByDisplayName(settings, LOCATION_DETAILS_URL_DISPLAY);

    this.filters =
        settings.stream()
            .filter(
                setting ->
                    !DEFAULT_MAP_QUESTION_KEYS.contains(setting.localizedSettingDisplayName().getDefault()))
            .collect(Collectors.toList());
  }

  /** Returns the form {@link QuestionSetting} objects for database storage. */
  private ImmutableSet<QuestionSetting> buildQuestionSettings() {
    ImmutableSet.Builder<QuestionSetting> builder = ImmutableSet.builder();

    builder.add(getLocationName());
    builder.add(getLocationAddress());
    builder.add(getLocationDetailsUrl());

    for (QuestionSetting filter : getFilters()) {
      if (QuestionSettingUtils.isValidSetting(filter)) {
        builder.add(filter);
      }
    }
    return builder.build();
  }

}
