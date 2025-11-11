package services.applicant.question;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import services.MessageKey;
import services.ObjectMapperSingleton;
import services.Path;
import services.applicant.ApplicantData;
import services.applicant.ValidationErrorMessage;
import services.question.LocalizedQuestionSetting;
import services.question.MapSettingType;
import services.question.types.MapQuestionDefinition;

// TODO(#11003): Build out map question.
public final class MapQuestion extends AbstractQuestion {
  private ApplicantData applicantData;

  MapQuestion(ApplicantQuestion applicantQuestion) {
    super(applicantQuestion);
    applicantData = applicantQuestion.getApplicant().getApplicantData();
  }

  @Override
  protected ImmutableMap<Path, ImmutableSet<ValidationErrorMessage>> getValidationErrorsInternal() {
    return ImmutableMap.of(applicantQuestion.getContextualizedPath(), validateSelections());
  }

  private ImmutableSet<ValidationErrorMessage> validateSelections() {
    int numberOfSelections = getSelectedLocations().size();
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
    ImmutableList<String> selectedLocationNames = getSelectedLocationNames();
    return selectedLocationNames.isEmpty() ? "-" : String.join(", ", selectedLocationNames);
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getSelectionPath());
  }

  public MapQuestionDefinition getQuestionDefinition() {
    return (MapQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  public ImmutableList<LocalizedQuestionSetting> getFilters() {
    return getFilters(applicantData.preferredLocale());
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
    return getSettingKey(
        applicantData.preferredLocale(), MapSettingType.LOCATION_NAME_GEO_JSON_KEY);
  }

  public String getAddressValue() {
    return getSettingKey(
        applicantData.preferredLocale(), MapSettingType.LOCATION_ADDRESS_GEO_JSON_KEY);
  }

  public String getDetailsUrlValue() {
    return getSettingKey(
        applicantData.preferredLocale(), MapSettingType.LOCATION_DETAILS_URL_GEO_JSON_KEY);
  }

  public LocalizedQuestionSetting getTagSetting() {
    return getSetting(applicantData.preferredLocale(), MapSettingType.LOCATION_TAG_GEO_JSON_KEY)
        .orElse(null);
  }

  public boolean hasTagSetting() {
    return getTagSetting() != null;
  }

  public boolean hasTagText() {
    return !getTagText().isBlank();
  }

  public String getTagKey() {
    LocalizedQuestionSetting tag = getTagSetting();
    return tag != null ? tag.settingKey() : "";
  }

  public String getTagValue() {
    LocalizedQuestionSetting tag = getTagSetting();
    return tag != null ? tag.settingValue() : "";
  }

  public String getTagDisplayName() {
    LocalizedQuestionSetting tag = getTagSetting();
    return tag != null ? tag.settingDisplayName() : "";
  }

  public String getTagText() {
    LocalizedQuestionSetting tag = getTagSetting();
    return tag != null ? tag.settingText() : "";
  }

  public Path getSelectionPath() {
    return applicantQuestion.getContextualizedPath().join(Scalar.SELECTIONS);
  }

  public String getSelectionPathAsArray() {
    return getSelectionPath().toString() + Path.ARRAY_SUFFIX;
  }

  public ImmutableList<String> getSelectedLocationNames() {
    return getSelectedLocations().stream()
        .map(location -> location.get("locationName"))
        .collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<String> getSelectedLocationIds() {
    return getSelectedLocations().stream()
        .map(location -> location.get("featureId"))
        .collect(ImmutableList.toImmutableList());
  }

  private ImmutableList<Map<String, String>> getSelectedLocations() {
    Optional<ImmutableList<String>> selectedLocationsString =
        applicantData.readStringList(getSelectionPath());

    return selectedLocationsString
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
      //  Creating an instance of ObjectMapper is a heavy-ish operation so it should be done in the
      // constructor but that's not possible here, so using the same settings as the global
      // ObjectMapper for consistency
      TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {};
      return Optional.of(ObjectMapperSingleton.instance().readValue(jsonString, typeRef));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public boolean locationIsSelected(String locationId) {
    ImmutableList<String> selectedIds = getSelectedLocationIds();
    return selectedIds.contains(locationId);
  }

  public String createLocationJson(String featureId, String locationName) {
    if (locationName == null) {
      locationName = "Unknown Location";
    }
    MapSelection selection = MapSelection.create(featureId, locationName);
    try {
      return ObjectMapperSingleton.instance().writeValueAsString(selection);
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize MapSelection to JSON", e);
    }
  }

  private ImmutableSet<LocalizedQuestionSetting> getSettings(Locale locale) {
    return getQuestionDefinition().getSettingsForLocaleOrDefault(locale).orElse(ImmutableSet.of());
  }

  private String getSettingKey(Locale locale, MapSettingType settingType) {
    return getSettings(locale).stream()
        .filter(setting -> setting.settingType() == settingType)
        .findFirst()
        .map(LocalizedQuestionSetting::settingKey)
        .orElse("");
  }

  private Optional<LocalizedQuestionSetting> getSetting(Locale locale, MapSettingType settingType) {
    return getSettings(locale).stream()
        .filter(setting -> setting.settingType() == settingType)
        .findFirst();
  }
}
