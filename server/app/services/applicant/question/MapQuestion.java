package services.applicant.question;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.geojson.FeatureCollection;
import services.question.LocalizedQuestionSetting;
import services.question.MapSettingType;
import services.question.types.MapQuestionDefinition;

// TODO(#11003): Build out map question.
public final class MapQuestion extends AbstractQuestion {

  MapQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
  }

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    return ImmutableMap.of(applicantQuestion.getContextualizedPath(), validateSelections());
  }

  private ImmutableSet<ValidationErrorMessage> validateSelections() {
    int numberOfSelections = getSelectedLocationIds().size();
    ImmutableSet.Builder<ValidationErrorMessage> errors = ImmutableSet.builder();

    if (getQuestionDefinition().getMapValidationPredicates().maxLocationSelections().isPresent()) {
      int maxLocationSelections =
          getQuestionDefinition().getMapValidationPredicates().maxLocationSelections().getAsInt();
      if (numberOfSelections > maxLocationSelections) {
        errors.add(
            ValidationErrorMessage.create(
                MessageKey.MAP_VALIDATION_TOO_MANY, maxLocationSelections));
      }
    }
    return errors.build();
  }

  @Override
  public String getAnswerString() {
    ImmutableList<String> selectedLocationIds = getSelectedLocationIds();
    return selectedLocationIds.isEmpty() ? "-" : String.join(", ", selectedLocationIds);
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getSelectionPath());
  }

  public MapQuestionDefinition getQuestionDefinition() {
    return (MapQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public ImmutableList<LocalizedQuestionSetting> getFilters() {
    return getFilters(applicantQuestion.getApplicant().getApplicantData().preferredLocale());
  }

  /**
   * In a MAP question, filters include any setting that has {@link
   * MapSettingType#LOCATION_FILTER_GEO_JSON_KEY} type. The admin is limited to submitting 3 filters
   * when creating the question.
   *
   * @param locale the {@link Locale} of the applicant
   * @return Question Settings with {@link MapSettingType#LOCATION_FILTER_GEO_JSON_KEY} type
   */
  public ImmutableList<LocalizedQuestionSetting> getFilters(Locale locale) {
    return getSettings(locale).stream()
        .filter(setting -> setting.settingType() == MapSettingType.LOCATION_FILTER_GEO_JSON_KEY)
        .collect(ImmutableList.toImmutableList());
  }

  public String getNameValue() {
    return getSettingValue(
        applicantQuestion.getApplicant().getApplicantData().preferredLocale(),
        MapSettingType.LOCATION_NAME_GEO_JSON_KEY);
  }

  public String getAddressValue() {
    return getSettingValue(
        applicantQuestion.getApplicant().getApplicantData().preferredLocale(),
        MapSettingType.LOCATION_ADDRESS_GEO_JSON_KEY);
  }

  public String getDetailsUrlValue() {
    return getSettingValue(
        applicantQuestion.getApplicant().getApplicantData().preferredLocale(),
        MapSettingType.LOCATION_DETAILS_URL_GEO_JSON_KEY);
  }

  public Path getSelectionPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.SELECTIONS);
  }

  public String getSelectionPathAsArray() {
    return getSelectionPath().toString() + Path.ARRAY_SUFFIX;
  }

  public ImmutableList<Map<String, String>> getSelectedLocations() {
    Optional<ImmutableList<String>> rawData =
        applicantQuestion.getApplicantData().readStringList(getSelectionPath());

    return rawData
        .map(
            strings ->
                strings.stream()
                    .map(this::parseLocationJson)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(ImmutableList.toImmutableList()))
        .orElseGet(ImmutableList::of);
  }

  private Optional<Map<String, String>> parseLocationJson(String jsonString) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {};
      return Optional.of(mapper.readValue(jsonString, typeRef));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public ImmutableList<String> getSelectedLocationIds() {
    return getSelectedLocations().stream()
        .map(location -> location.get("featureId"))
        .collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<String> getSelectedLocationNames() {
    return getSelectedLocations().stream()
        .map(location -> location.get("locationName"))
        .collect(ImmutableList.toImmutableList());
  }

  public boolean locationIsSelected(String featureId) {
    ImmutableList<String> selectedItems = getSelectedLocationIds();
    return selectedItems.contains(featureId);
  }

  public String createLocationJson(String featureId, String locationName) {
    return String.format("{\"featureId\":\"%s\",\"locationName\":\"%s\"}", featureId, locationName);
  }

  public String getLocationNameById(String featureId, FeatureCollection geoJson) {
    String nameKey = getNameValue();

    return geoJson.features().stream()
        .filter(feature -> featureId.equals(feature.id()))
        .findFirst()
        .map(feature -> feature.properties().getOrDefault(nameKey, featureId))
        .orElse(featureId); // Fallback to ID if not found
  }

  private ImmutableSet<LocalizedQuestionSetting> getSettings(Locale locale) {
    return getQuestionDefinition().getSettingsForLocaleOrDefault(locale).orElse(ImmutableSet.of());
  }

  private String getSettingValue(Locale locale, MapSettingType settingType) {
    return getSettings(locale).stream()
        .filter(setting -> setting.settingType() == settingType)
        .findFirst()
        .map(LocalizedQuestionSetting::settingValue)
        .orElse("");
  }
}
