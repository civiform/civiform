package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.rawHtml;
import static views.applicant.AuthenticateUpsellCreator.createLoginPromptModal;
import static views.components.Modal.RepeatOpenBehavior.Group.PROGRAMS_INDEX_LOGIN_PROMPT;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import controllers.applicant.ApplicantRoutes;
import j2html.tags.DomContent;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.mvc.Http;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.program.ProgramDefinition;
import services.settings.SettingsManifest;
import views.HtmlBundle;
import views.ProgramImageUtils;
import views.TranslationUtils;
import views.components.Icons;
import views.components.Modal;
import views.components.TextFormatter;
import views.style.BaseStyles;

import views.style.ReferenceClasses;

/** A renderer to create an individual program information card. */
public final class ProgramCardViewRenderer {
  private final ApplicantRoutes applicantRoutes;
  private final ProfileUtils profileUtils;
  private final ProgramImageUtils programImageUtils;
  private final SettingsManifest settingsManifest;
  private final TemplateEngine templateEngine;
  private final ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory;

  @Inject
  public ProgramCardViewRenderer(
      ApplicantRoutes applicantRoutes,
      ProfileUtils profileUtils,
      ProgramImageUtils programImageUtils,
      SettingsManifest settingsManifest,
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory) {
    this.applicantRoutes = checkNotNull(applicantRoutes);
    this.profileUtils = checkNotNull(profileUtils);
    this.programImageUtils = checkNotNull(programImageUtils);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.templateEngine = checkNotNull(templateEngine);
    this.playThymeleafContextFactory = checkNotNull(playThymeleafContextFactory);
  }

  /**
   * Creates and returns DOM representing a program card using the information provided in {@code
   * cardData}.
   *
   * @param buttonTitle a message key for the text displayed on the action button on the bottom of
   *     the card.
   * @param buttonSrText a message key for the screen reader text for the action button on the
   *     bottom of the card.
   * @param nestedUnderSubheading true if this card appears under a heading (like "In Progress") and
   *     false otherwise.
   */
  public DomContent createProgramCard(
      Http.Request request,
      Messages messages,
      ApplicantPersonalInfo.ApplicantType applicantType,
      ApplicantService.ApplicantProgramData cardData,
      Long applicantId,
      Locale preferredLocale,
      MessageKey buttonTitle,
      MessageKey buttonSrText,
      boolean nestedUnderSubheading,
      HtmlBundle bundle,
      CiviFormProfile profile,
      ZoneId zoneId) {
    ProgramDefinition program = cardData.program();

    String baseId = ReferenceClasses.APPLICATION_CARD + "-" + program.id();

    Optional<DomContent> programImage =
        programImageUtils.createProgramImage(
            request, program, preferredLocale, /* isWithinProgramCard= */ true);
    ImmutableList<DomContent> descriptionContent =
        TextFormatter.formatText(
            program.localizedDescription().getOrDefault(preferredLocale),
            /* preserveEmptyLines= */ false,
            /* addRequiredIndicator= */ false);

    ThymeleafModule.PlayThymeleafContext context = playThymeleafContextFactory.create(request);
    context.setVariable("openInNewIcon", Icons.svg(templateEngine, context, Icons.OPEN_IN_NEW, ImmutableSet.of("mr-2", "w-5", "h-5")));
    context.setVariable("id", baseId);
    context.setVariable(
        "programLocalizedName", program.localizedName().getOrDefault(preferredLocale));
    context.setVariable("descriptionContent", descriptionContent);
    context.setVariable("nestedUnderSubheading", nestedUnderSubheading);
    // Use external link if it is present else use the default Program details page
    String programDetailsLink =
        program.externalLink().isEmpty()
            ? applicantRoutes.show(profile, applicantId, program.id()).url()
            : program.externalLink();
    context.setVariable("programDetailsLink", programDetailsLink);
    context.setVariable(
        "programDetailsText", messages.at(MessageKey.LINK_PROGRAM_DETAILS.getKeyName()));
    context.setVariable(
        "programDetailsAriaLabel",
        messages.at(
            MessageKey.LINK_PROGRAM_DETAILS_SR.getKeyName(),
            program.localizedName().getOrDefault(preferredLocale)));
    if (programImage.isPresent()) {
      context.setVariable("programImage", programImage.get().render());
    }
    boolean hasApplicationStatus = cardData.latestSubmittedApplicationStatus().isPresent();
    context.setVariable("hasApplicationStatus", hasApplicationStatus);
    if (hasApplicationStatus) {
      context.setVariable(
          "applicationStatusText",
          String.format(
              "%s: %s",
              messages.at(MessageKey.TITLE_STATUS.getKeyName()),
              cardData
                  .latestSubmittedApplicationStatus()
                  .get()
                  .localizedStatusText()
                  .getOrDefault(preferredLocale)));
    }
    boolean shouldShowEligibilityTag = shouldShowEligibilityTag(cardData);
    context.setVariable("shouldShowEligibilityTag", shouldShowEligibilityTag);
    if (shouldShowEligibilityTag) {
      CiviFormProfile submittingProfile = profileUtils.currentUserProfile(request).orElseThrow();
      boolean isTrustedIntermediary = submittingProfile.isTrustedIntermediary();
      MessageKey mayQualifyMessage =
          isTrustedIntermediary ? MessageKey.TAG_MAY_QUALIFY_TI : MessageKey.TAG_MAY_QUALIFY;
      MessageKey mayNotQualifyMessage =
          isTrustedIntermediary ? MessageKey.TAG_MAY_NOT_QUALIFY_TI : MessageKey.TAG_MAY_NOT_QUALIFY;
          boolean isEligible = cardData.isProgramMaybeEligible().get();
      Icons icon = isEligible ? Icons.CHECK_CIRCLE : Icons.INFO;
      String color = isEligible ? BaseStyles.BG_CIVIFORM_GREEN_LIGHT : "bg-gray-200";
      String textColor = isEligible ? BaseStyles.TEXT_CIVIFORM_GREEN : "text-black";
      String tagClass =
          isEligible ? ReferenceClasses.ELIGIBLE_TAG : ReferenceClasses.NOT_ELIGIBLE_TAG;
      String tagText =
          isEligible ? mayQualifyMessage.getKeyName() : mayNotQualifyMessage.getKeyName();
      context.setVariable("eligibilityIcon", Icons.svg(templateEngine, context, icon, ImmutableSet.of("inline-block", "h-4.5", "w-4.5", textColor)));
      context.setVariable("eligibilityText", messages.at(tagText));
      context.setVariable("eligibilityContainerClasses", color + " " + tagClass);
      context.setVariable("eligibilityTextClasses", textColor);
    }

    boolean showSubmittedDate = cardData.latestSubmittedApplicationTime().isPresent();
    context.setVariable("showSubmittedDate", showSubmittedDate);
    if (showSubmittedDate) {
      TranslationUtils.TranslatedStringSplitResult translateResult =
          TranslationUtils.splitTranslatedSingleArgString(messages, MessageKey.SUBMITTED_DATE);
      context.setVariable("beforeDateText", translateResult.beforeInterpretedContent());
      context.setVariable("afterDateText", translateResult.afterInterpretedContent());
      ZonedDateTime dateTime = cardData.latestSubmittedApplicationTime().get().atZone(zoneId);
      String formattedSubmitTime =
          DateTimeFormatter.ofLocalizedDate(
                  // SHORT will print dates as 1/2/2022.
                  FormatStyle.SHORT)
              .format(dateTime);
      context.setVariable("submittedDateText", formattedSubmitTime);
    }

        String actionUrl = applicantRoutes.review(profile, applicantId, program.id()).url();

        Modal loginPromptModal =
        createLoginPromptModal(
                messages,
                actionUrl,
                messages.at(
                    MessageKey.INITIAL_LOGIN_MODAL_PROMPT.getKeyName(),
                    settingsManifest.getApplicantPortalName(request).get()),
                MessageKey.BUTTON_CONTINUE_TO_APPLICATION)
            .setRepeatOpenBehavior(
                Modal.RepeatOpenBehavior.showOnlyOnce(PROGRAMS_INDEX_LOGIN_PROMPT, actionUrl))
            .build();
    bundle.addModals(loginPromptModal);

    return rawHtml(
        templateEngine.process(
            "applicant/ProgramCardFragment", ImmutableSet.of("program-card"), context));

    // // If the user is a guest, show the login prompt modal, which has a button
    // // to continue on to the application. Otherwise, show the button to go to the
    // // application directly.
    // ContainerTag content =
    //     applicantType == GUEST
    //         ? TagCreator.button().withId(loginPromptModal.getTriggerButtonId())
    //         : a().withHref(actionUrl).withId(baseId + "-apply");

    // content
    //     .withText(messages.at(buttonTitle.getKeyName()))
    //     .attr(
    //         "aria-label",
    //         messages.at(
    //             buttonSrText.getKeyName(),
    // program.localizedName().getOrDefault(preferredLocale)))
    //     .withClasses(ReferenceClasses.APPLY_BUTTON, ButtonStyles.SOLID_BLUE_TEXT_SM);


    // return cardListItem.with(programData).with(actionDiv);
  }

  /**
   * If eligibility is gating, the eligibility tag should always show when present. If eligibility
   * is non-gating, the eligibility tag should only show if the user may be eligible.
   */
  private static boolean shouldShowEligibilityTag(ApplicantService.ApplicantProgramData cardData) {
    if (!cardData.isProgramMaybeEligible().isPresent()) {
      return false;
    }

    return cardData.program().eligibilityIsGating() || cardData.isProgramMaybeEligible().get();
  }
}
