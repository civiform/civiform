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
    System.out.println("Validating");
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

  private String getSettingValue(Locale locale, MapSettingType settingType) {
    Optional<LocalizedQuestionSetting> localizedQuestionSetting =
        getSettings(locale).stream()
            .filter(setting -> setting.settingType() == settingType)
            .findFirst();

    if (localizedQuestionSetting.isPresent()) {
      return localizedQuestionSetting.get().settingValue();
    }

    return "";
  }
}
