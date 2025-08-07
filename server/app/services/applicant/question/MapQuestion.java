package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Locale;
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

  public ImmutableList<LocalizedQuestionSetting> getFilters() {
    return getFilters(applicantQuestion.getApplicant().getApplicantData().preferredLocale());
  }

  public ImmutableList<LocalizedQuestionSetting> getFilters(Locale locale) {
    return getQuestionDefinition().getFiltersForLocaleOrDefault(locale);
  }

  public ImmutableList<LocalizedQuestionSetting> getNameSetting() {
    return getFilters(applicantQuestion.getApplicant().getApplicantData().preferredLocale());
  }

  public ImmutableList<LocalizedQuestionSetting> getNameSetting(Locale locale) {
    return getQuestionDefinition().getFiltersForLocaleOrDefault(locale);
  }

  public ImmutableList<LocalizedQuestionSetting> getAddressSetting() {
    return getFilters(applicantQuestion.getApplicant().getApplicantData().preferredLocale());
  }

  public ImmutableList<LocalizedQuestionSetting> getAddressSetting(Locale locale) {
    return getQuestionDefinition().getFiltersForLocaleOrDefault(locale);
  }

  public ImmutableList<LocalizedQuestionSetting> getLocationDetailsUrlSetting() {
    return getFilters(applicantQuestion.getApplicant().getApplicantData().preferredLocale());
  }

  public ImmutableList<LocalizedQuestionSetting> getLocationDetailsUrlSetting(Locale locale) {
    return getQuestionDefinition().getFiltersForLocaleOrDefault(locale);
  }
}
