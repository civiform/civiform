package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import forms.MapQuestionForm;
import java.util.Locale;
import java.util.Optional;
import services.Path;
import services.applicant.ValidationErrorMessage;
import services.question.LocalizedQuestionSetting;
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

  public ImmutableSet<LocalizedQuestionSetting> getFilters() {
    return getFilters(applicantQuestion.getApplicant().getApplicantData().preferredLocale());
  }

  public ImmutableSet<LocalizedQuestionSetting> getFilters(Locale locale) {
    return getSettings(locale).stream()
        .filter(
            setting ->
                !MapQuestionForm.DEFAULT_MAP_QUESTION_KEYS.contains(setting.settingDisplayName()))
        .collect(ImmutableSet.toImmutableSet());
  }

  public Optional<LocalizedQuestionSetting> getNameSetting() {
    return getNameSetting(applicantQuestion.getApplicant().getApplicantData().preferredLocale());
  }

  public Optional<LocalizedQuestionSetting> getNameSetting(Locale locale) {
    return getSettings(locale).stream()
        .filter(
            setting -> MapQuestionForm.LOCATION_NAME_DISPLAY.equals(setting.settingDisplayName()))
        .findFirst();
  }

  public Optional<LocalizedQuestionSetting> getAddressSetting() {
    return getAddressSetting(applicantQuestion.getApplicant().getApplicantData().preferredLocale());
  }

  public Optional<LocalizedQuestionSetting> getAddressSetting(Locale locale) {
    return getSettings(locale).stream()
        .filter(
            setting ->
                MapQuestionForm.LOCATION_ADDRESS_DISPLAY.equals(setting.settingDisplayName()))
        .findFirst();
  }

  public Optional<LocalizedQuestionSetting> getLocationDetailsUrlSetting() {
    return getLocationDetailsUrlSetting(
        applicantQuestion.getApplicant().getApplicantData().preferredLocale());
  }

  public Optional<LocalizedQuestionSetting> getLocationDetailsUrlSetting(Locale locale) {
    return getSettings(locale).stream()
        .filter(
            setting ->
                MapQuestionForm.LOCATION_DETAILS_URL_DISPLAY.equals(setting.settingDisplayName()))
        .findFirst();
  }
}
