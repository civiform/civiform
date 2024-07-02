package controllers.applicant;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.inject.Inject;
import controllers.FlashKey;
import java.util.Optional;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http;
import services.AlertSettings;
import services.AlertType;
import services.MessageKey;
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

  /** Returns true if eligibility is gating and the application is ineligible, false otherwise. */
  private boolean isEligibilityGating(long programId) {
    try {
      return programService.getFullProgramDefinition(programId).eligibilityIsGating();
    } catch (ProgramNotFoundException ex) {
      // Checked exceptions are the devil and we've already determined that this program exists by
      // this point
      throw new RuntimeException("Could not find program.", ex);
    }
  }

  public AlertSettings calculate(
      Http.Request request, boolean isTI, boolean isApplicationEligible, long programId) {
    Messages messages = messagesApi.preferred(request);

    boolean isEligibilityGating = isEligibilityGating(programId);

    if (!isEligibilityGating) {
      return AlertSettings.empty();
    }

    boolean isApplicationFastForwarded =
        request.flash().get(FlashKey.SHOW_FAST_FORWARDED_MESSAGE).isPresent();

    Triple triple =
        isTI
            ? getTi(isApplicationFastForwarded, isApplicationEligible)
            : getApplicant(isApplicationFastForwarded, isApplicationEligible);

    return new AlertSettings(
        isEligibilityGating,
        Optional.of(messages.at(triple.titleKey.getKeyName())),
        messages.at(triple.textKey.getKeyName()),
        triple.alertType);
  }

  private Triple getTi(Boolean isApplicationFastForwarded, Boolean isApplicationEligible) {
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

    // The default case: isApplicationFastForwarded == false && isApplicationEligible == false
    return new Triple(
        AlertType.WARNING,
        MessageKey.ALERT_ELIGIBILITY_TI_NOT_ELIGIBLE_TITLE,
        MessageKey.ALERT_ELIGIBILITY_TI_NOT_ELIGIBLE_TEXT);
  }

  private Triple getApplicant(Boolean isApplicationFastForwarded, Boolean isApplicationEligible) {
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

    // The default case: isApplicationFastForwarded == false && isApplicationEligible == false
    return new Triple(
        AlertType.WARNING,
        MessageKey.ALERT_ELIGIBILITY_APPLICANT_NOT_ELIGIBLE_TITLE,
        MessageKey.ALERT_ELIGIBILITY_APPLICANT_NOT_ELIGIBLE_TEXT);
  }

  private record Triple(AlertType alertType, MessageKey titleKey, MessageKey textKey) {}
}
