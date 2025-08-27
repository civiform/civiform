package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
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
    // TODO(#11002): Add map question validation.
    return ImmutableMap.of();
  }

  @Override
  public String getAnswerString() {
    Optional<ImmutableList<String>> selectedLocationNames = getSelectedLocationNames();
    return selectedLocationNames.map(strings -> String.join(", ", strings)).orElse("-");
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    return ImmutableList.of(getSelectionPath());
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

  public Optional<ImmutableList<String>> getSelectedLocationNames() {
    return applicantQuestion.getApplicantData().readStringList(getSelectionPath());
  }

  public boolean locationIsSelected(String locationName) {
    Optional<ImmutableList<String>> selectedNames = getSelectedLocationNames();
    return selectedNames.isPresent() && selectedNames.get().contains(locationName);
  }

  private MapQuestionDefinition getQuestionDefinition() {
    return (MapQuestionDefinition) applicantQuestion.getQuestionDefinition();
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
