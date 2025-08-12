package services.applicant.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import forms.MapQuestionForm;
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

  private ImmutableSet<LocalizedQuestionSetting> getSettings(Locale locale) {
    return getQuestionDefinition().getSettingsForLocaleOrDefault(locale).orElse(ImmutableSet.of());
  }

  public ImmutableList<LocalizedQuestionSetting> getFilters() {
    return getFilters(applicantQuestion.getApplicant().getApplicantData().preferredLocale());
  }

  // In a MAP question, any setting that is not in the DEFAULT_MAP_QUESTION_KEYS is used as a
  // filter, and the admin is limited to submitting 3 filters

  /**
   * In a MAP question, filters include any setting that is not in {@link
   * MapQuestionForm#DEFAULT_MAP_QUESTION_KEYS}, and will be used as filters in the question. The
   * admin is limited to submitting 3 filters when creating the question.
   *
   * @param locale the {@link Locale} of the applicant
   * @return Question Settings excluding {@link MapQuestionForm#DEFAULT_MAP_QUESTION_KEYS}
   */
  public ImmutableList<LocalizedQuestionSetting> getFilters(Locale locale) {
    return getSettings(locale).stream()
        .filter(
            setting ->
                !MapQuestionForm.DEFAULT_MAP_QUESTION_KEYS.contains(setting.settingDisplayName()))
        .collect(ImmutableList.toImmutableList());
  }
}
