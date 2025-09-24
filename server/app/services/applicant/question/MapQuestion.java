package services.applicant.question;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import services.MessageKey;
import services.Path;
import services.applicant.ValidationErrorMessage;
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
        applicantQuestion.getApplicantData().readStringList(getSelectionPath());

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
      ObjectMapper mapper =
          new ObjectMapper()
              // Play defaults
              .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
              .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
              // This adds support for Optional
              .registerModule(new Jdk8Module())
              // This adds support for ImmutableList, ImmutableSet, etc.
              .registerModule(new GuavaModule())
              // This adds support for Instant, LocalDateTime, java.time classes, etc.
              .registerModule(new JavaTimeModule());

      TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {};
      return Optional.of(mapper.readValue(jsonString, typeRef));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public boolean locationIsSelected(String locationId) {
    ImmutableList<String> selectedIds = getSelectedLocationIds();
    return selectedIds.contains(locationId);
  }

  public String createLocationJson(String featureId, String locationName) {
    MapSelection selection = MapSelection.create(featureId, locationName);
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(selection);
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize MapSelection to JSON", e);
    }
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
