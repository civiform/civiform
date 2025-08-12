package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
import java.util.Optional;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.LocalizedQuestionSetting;
import services.question.SettingType;
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
    // TODO(#11003) Create answer string
    return "";
  }

  @Override
  public ImmutableList<Path> getAllPaths() {
    // @TODO(#11003): Return a real value for the map question
    return ImmutableList.of();
  }

  public MapQuestionDefinition getQuestionDefinition() {
    return (MapQuestionDefinition) applicantQuestion.getQuestionDefinition();
  }

  private ImmutableSet<LocalizedQuestionSetting> getSettings(Locale locale) {
    return getQuestionDefinition().getSettingsForLocaleOrDefault(locale).orElse(ImmutableSet.of());
  }

  public ImmutableList<LocalizedQuestionSetting> getFilters() {
    return getFilters(applicantQuestion.getApplicant().getApplicantData().preferredLocale());
  }

  /**
   * In a MAP question, filters include any setting that has {@link SettingType#FILTER} type. The
   * admin is limited to submitting 3 filters when creating the question.
   *
   * @param locale the {@link Locale} of the applicant
   * @return Question Settings with {@link SettingType#FILTER} type
   */
  public ImmutableList<LocalizedQuestionSetting> getFilters(Locale locale) {
    return getSettings(locale).stream()
        .filter(setting -> setting.settingId() == SettingType.FILTER)
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Get the GeoJSON field name that contains the location name.
   *
   * @param locale the {@link Locale} of the applicant
   * @return the GeoJSON field name for location name, if configured
   */
  public Optional<String> getNameFieldKey(Locale locale) {
    return getSettings(locale).stream()
        .filter(setting -> setting.settingId() == SettingType.NAME)
        .map(LocalizedQuestionSetting::settingKey)
        .findFirst();
  }

  /**
   * Get the GeoJSON field name that contains the location address.
   *
   * @param locale the {@link Locale} of the applicant
   * @return the GeoJSON field name for location address, if configured
   */
  public Optional<String> getAddressFieldKey(Locale locale) {
    return getSettings(locale).stream()
        .filter(setting -> setting.settingId() == SettingType.ADDRESS)
        .map(LocalizedQuestionSetting::settingKey)
        .findFirst();
  }

  /**
   * Get the GeoJSON field name that contains the location details URL.
   *
   * @param locale the {@link Locale} of the applicant
   * @return the GeoJSON field name for location URL, if configured
   */
  public Optional<String> getUrlFieldKey(Locale locale) {
    return getSettings(locale).stream()
        .filter(setting -> setting.settingId() == SettingType.URL)
        .map(LocalizedQuestionSetting::settingKey)
        .findFirst();
  }

  /** Get the GeoJSON field name for location name using the applicant's preferred locale. */
  public Optional<String> getNameFieldKey() {
    return getNameFieldKey(applicantQuestion.getApplicant().getApplicantData().preferredLocale());
  }

  /** Get the GeoJSON field name for location address using the applicant's preferred locale. */
  public Optional<String> getAddressFieldKey() {
    return getAddressFieldKey(
        applicantQuestion.getApplicant().getApplicantData().preferredLocale());
  }

  /** Get the GeoJSON field name for location URL using the applicant's preferred locale. */
  public Optional<String> getUrlFieldKey() {
    return getUrlFieldKey(applicantQuestion.getApplicant().getApplicantData().preferredLocale());
  }
}
