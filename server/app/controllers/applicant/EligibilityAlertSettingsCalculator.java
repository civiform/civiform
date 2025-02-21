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
   * Calculates the alert settings for the given request. This method contains a String param
   * representing eligibility message.
   *
   * @param request The HTTP request.
   * @param isTI True if the request is from a tax advisor.
   * @param isApplicationEligible True if the application is eligible for the program.
   * @param isNorthStarEnabled True if NorthStar is enabled.
   * @param pageHasSupplementalInformation True if the page has supplemental information.
   * @param programId The program ID.
   * @param eligibilityMsg The eligibility message.
   * @param questions The list of applicant questions that the applicant answered that may make the
   *     applicant ineligible. The list may be empty.
   * @return The alert settings.
   */
  public AlertSettings calculate(
      Http.Request request,
      boolean isTI,
      boolean isApplicationEligible,
      boolean isNorthStarEnabled,
      boolean pageHasSupplementalInformation,
      long programId,
      String eligibilityMsg,
      ImmutableList<ApplicantQuestion> questions) {
    return calculateCommon(
        request,
        isTI,
        isApplicationEligible,
        isNorthStarEnabled,
        pageHasSupplementalInformation,
        programId,
        eligibilityMsg,
        questions);
  }

  /**
   * Calculates the alert settings for the given request.
   *
   * @param request The HTTP request.
   * @param isTI True if the request is from a tax advisor.
   * @param isApplicationEligible True if the application is eligible for the program.
   * @param isNorthStarEnabled True if NorthStar is enabled.
   * @param pageHasSupplementalInformation True if the page has supplemental information.
   * @param programId The program ID.
   * @param questions The list of applicant questions that the applicant answered that may make the
   *     applicant ineligible. The list may be empty.
   * @return The alert settings.
   */
  public AlertSettings calculate(
      Http.Request request,
      boolean isTI,
      boolean isApplicationEligible,
      boolean isNorthStarEnabled,
      boolean pageHasSupplementalInformation,
      long programId,
      ImmutableList<ApplicantQuestion> questions) {
    return calculateCommon(
        request,
        isTI,
        isApplicationEligible,
        isNorthStarEnabled,
        pageHasSupplementalInformation,
        programId,
        "",
        questions);
  }

  private AlertSettings calculateCommon(
      Http.Request request,
      boolean isTI,
      boolean isApplicationEligible,
      boolean isNorthStarEnabled,
      boolean pageHasSupplementalInformation,
      long programId,
      String eligibilityMsg,
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

    String text = messages.at(triple.textKey.getKeyName());
    ImmutableList<String> formattedQuestions =
        questions.stream()
            .map(ApplicantQuestion::getQuestionText)
            .collect(ImmutableList.toImmutableList());
    Optional<String> customMessage =
        eligibilityMsg.isEmpty() ? Optional.empty() : Optional.of(eligibilityMsg);
    Optional<String> title = Optional.of(messages.at(triple.titleKey.getKeyName()));
    Optional<String> helpText =
        title.isPresent()
            ? Optional.of(AlertSettings.getTitleHelpText(messages, triple.alertType, title.get()))
            : Optional.empty();

    return new AlertSettings(
        true,
        Optional.of(messages.at(triple.titleKey.getKeyName())),
        text,
        triple.alertType,
        formattedQuestions,
        customMessage,
        helpText,
        /* isSlim= */ false);
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

  /**
   * Returns true if eligibility is enabled on the program and it is not a common intake form, false
   * otherwise.
   */
  private boolean canShowEligibilitySettings(long programId) {
    try {
      var programDefinition = programService.getFullProgramDefinition(programId);

      return !programDefinition.isCommonIntakeForm() && programDefinition.hasEligibilityEnabled();
    } catch (ProgramNotFoundException ex) {
      // Checked exceptions are the devil and we've already determined that this program exists by
      // this point
      throw new RuntimeException("Could not find program.", ex);
    }
  }
}
