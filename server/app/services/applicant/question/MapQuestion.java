package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
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
    int numberOfSelections = getSelectedLocationIds().map(ImmutableList::size).orElse(0);
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
    Optional<ImmutableList<String>> selectedLocationIds = getSelectedLocationIds();
    return selectedLocationIds.map(strings -> String.join(", ", strings)).orElse("-");
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

  public Optional<ImmutableList<String>> getSelectedLocationIds() {
    return applicantQuestion.getApplicantData().readStringList(getSelectionPath());
  }

  public boolean locationIsSelected(String featureId) {
    Optional<ImmutableList<String>> selectedItems = getSelectedLocationIds();
    return selectedItems.isPresent() && selectedItems.get().contains(featureId);
  }

  public String getLocationNameById(String featureId, FeatureCollection geoJson) {
    if (geoJson == null) {
      return featureId; // Fallback to ID if no GeoJSON
    }

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
