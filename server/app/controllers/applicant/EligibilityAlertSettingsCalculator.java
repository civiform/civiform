package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.FlashKey;
import java.util.Optional;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http;
import services.AlertSettings;
import services.AlertType;
import services.MessageKey;
import services.applicant.ReadOnlyApplicantProgramService;
import services.applicant.question.ApplicantQuestion;
import services.program.ProgramNotFoundException;
import services.program.ProgramService;

public final class EligibilityAlertSettingsCalculator {
  private final ProgramService programService;
  private final MessagesApi messagesApi;

  @Inject
  public EligibilityAlertSettingsCalculator(
      ProgramService programService, MessagesApi messagesApi) {
    this.programService = checkNotNull(programService);
    this.messagesApi = checkNotNull(messagesApi);
  }

  /**
   * Returns true if eligibility is enabled on the program and it is not a common intake form, false
   * otherwise.
   */
  public boolean canShowEligibilitySettings(long programId) {
    try {
      var programDefinition = programService.getFullProgramDefinition(programId);

      return programDefinition.hasEligibilityEnabled() && !programDefinition.isCommonIntakeForm();
    } catch (ProgramNotFoundException ex) {
      // Checked exceptions are the devil and we've already determined that this program exists by
      // this point
      throw new RuntimeException("Could not find program.", ex);
    }
  }

  /** Returns true if eligibility is gating and the application is ineligible, false otherwise. */
  public boolean shouldShowNotEligibleBanner(
      ReadOnlyApplicantProgramService roApplicantProgramService, long programId) {
    try {
      if (!programService.getFullProgramDefinition(programId).eligibilityIsGating()) {
        return false;
      }
    } catch (ProgramNotFoundException ex) {
      return false;
    }

    return roApplicantProgramService.isApplicationEligible();
  }

  /** Returns true if the eligibility banner should be hidden. */
  public boolean shouldHideEligibilityBanner(
      ReadOnlyApplicantProgramService roApplicantProgramService, long programId) {
    return !roApplicantProgramService.hasAnsweredEligibilityQuestions()
        || !shouldShowNotEligibleBanner(roApplicantProgramService, programId);
  }

  /**
   * questions: List of questions that the applicant answered that may make the applicant
   * ineligible. The list may be empty.
   */
  public AlertSettings calculate(
      Http.Request request,
      boolean isTI,
      boolean isApplicationEligible,
      boolean isNorthStarEnabled,
      boolean pageHasSupplementalInformation,
      long programId,
      ImmutableList<ApplicantQuestion> questions) {
    Messages messages = messagesApi.preferred(request);

    if (!canShowEligibilitySettings(programId)) {
      return AlertSettings.empty();
    }

    boolean isApplicationFastForwarded =
        request.flash().get(FlashKey.SHOW_FAST_FORWARDED_MESSAGE).isPresent();

    Triple triple =
        isTI
            ? getTi(
                isApplicationFastForwarded,
                isApplicationEligible,
                isNorthStarEnabled,
                pageHasSupplementalInformation)
            : getApplicant(
                isApplicationFastForwarded,
                isApplicationEligible,
                isNorthStarEnabled,
                pageHasSupplementalInformation);

    ImmutableList<String> formattedQuestions =
        questions.stream()
            .map(ApplicantQuestion::getQuestionText)
            .collect(ImmutableList.toImmutableList());

    return new AlertSettings(
        true,
        Optional.of(messages.at(triple.titleKey.getKeyName())),
        messages.at(triple.textKey.getKeyName()),
        triple.alertType,
        formattedQuestions);
  }

  private Triple getTi(
      boolean isApplicationFastForwarded,
      boolean isApplicationEligible,
      boolean isNorthStarEnabled,
      boolean pageHasSupplementalInformation) {
    if (isApplicationFastForwarded == true && isApplicationEligible == true) {
      return new Triple(
          AlertType.SUCCESS,
          MessageKey.ALERT_ELIGIBILITY_TI_FASTFORWARDED_ELIGIBLE_TITLE,
          MessageKey.ALERT_ELIGIBILITY_TI_FASTFORWARDED_ELIGIBLE_TEXT);
    }

    if (isApplicationFastForwarded == true && isApplicationEligible == false) {
      return new Triple(
          AlertType.WARNING,
          MessageKey.ALERT_ELIGIBILITY_TI_FASTFORWARDED_NOT_ELIGIBLE_TITLE,
          MessageKey.ALERT_ELIGIBILITY_TI_FASTFORWARDED_NOT_ELIGIBLE_TEXT);
    }

    if (isApplicationFastForwarded == false && isApplicationEligible == true) {
      return new Triple(
          AlertType.SUCCESS,
          MessageKey.ALERT_ELIGIBILITY_TI_ELIGIBLE_TITLE,
          MessageKey.ALERT_ELIGIBILITY_TI_ELIGIBLE_TEXT);
    }

    if (isNorthStarEnabled == true && pageHasSupplementalInformation == true) {
      return new Triple(
          AlertType.WARNING,
          MessageKey.ALERT_ELIGIBILITY_TI_NOT_ELIGIBLE_TITLE,
          MessageKey.ALERT_ELIGIBILITY_TI_NOT_ELIGIBLE_TEXT_SHORT);
    }

    // The default case: isApplicationFastForwarded == false && isApplicationEligible == false
    return new Triple(
        AlertType.WARNING,
        MessageKey.ALERT_ELIGIBILITY_TI_NOT_ELIGIBLE_TITLE,
        MessageKey.ALERT_ELIGIBILITY_TI_NOT_ELIGIBLE_TEXT);
  }

  private Triple getApplicant(
      boolean isApplicationFastForwarded,
      boolean isApplicationEligible,
      boolean isNorthStarEnabled,
      boolean pageHasSupplementalInformation) {
    if (isApplicationFastForwarded == true && isApplicationEligible == true) {
      return new Triple(
          AlertType.SUCCESS,
          MessageKey.ALERT_ELIGIBILITY_APPLICANT_FASTFORWARDED_ELIGIBLE_TITLE,
          MessageKey.ALERT_ELIGIBILITY_APPLICANT_FASTFORWARDED_ELIGIBLE_TEXT);
    }

    if (isApplicationFastForwarded == true && isApplicationEligible == false) {
      return new Triple(
          AlertType.WARNING,
          MessageKey.ALERT_ELIGIBILITY_APPLICANT_FASTFORWARDED_NOT_ELIGIBLE_TITLE,
          MessageKey.ALERT_ELIGIBILITY_APPLICANT_FASTFORWARDED_NOT_ELIGIBLE_TEXT);
    }

    if (isApplicationFastForwarded == false && isApplicationEligible == true) {
      return new Triple(
          AlertType.SUCCESS,
          MessageKey.ALERT_ELIGIBILITY_APPLICANT_ELIGIBLE_TITLE,
          MessageKey.ALERT_ELIGIBILITY_APPLICANT_ELIGIBLE_TEXT);
    }

    if (pageHasSupplementalInformation == true && isNorthStarEnabled == true) {
      return new Triple(
          AlertType.WARNING,
          MessageKey.ALERT_ELIGIBILITY_APPLICANT_NOT_ELIGIBLE_TITLE,
          MessageKey.ALERT_ELIGIBILITY_APPLICANT_NOT_ELIGIBLE_TEXT_SHORT);
    }

    // The default case: isApplicationFastForwarded == false && isApplicationEligible == false
    return new Triple(
        AlertType.WARNING,
        MessageKey.ALERT_ELIGIBILITY_APPLICANT_NOT_ELIGIBLE_TITLE,
        MessageKey.ALERT_ELIGIBILITY_APPLICANT_NOT_ELIGIBLE_TEXT);
  }

  private record Triple(AlertType alertType, MessageKey titleKey, MessageKey textKey) {}
}
