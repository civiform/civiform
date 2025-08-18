package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.li;
import static j2html.TagCreator.ol;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static services.applicant.ApplicantPersonalInfo.ApplicantType.GUEST;
import static views.applicant.AuthenticateUpsellCreator.createLoginPromptModal;
import static views.components.Modal.RepeatOpenBehavior.Group.PROGRAMS_INDEX_LOGIN_PROMPT;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import controllers.applicant.ApplicantRoutes;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.ImgTag;
import j2html.tags.specialized.LiTag;
import j2html.tags.specialized.PTag;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.program.ProgramDefinition;
import services.settings.SettingsManifest;
import views.HtmlBundle;
import views.ProgramImageUtils;
import views.components.ButtonStyles;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.components.TextFormatter;
import views.style.ApplicantStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** A renderer to display program information cards individually and in groups. */
public final class ProgramCardViewRenderer {
  private final ApplicantRoutes applicantRoutes;
  private final ProfileUtils profileUtils;
  private final ProgramImageUtils programImageUtils;
  private final SettingsManifest settingsManifest;
  private final ZoneId zoneId;

  @Inject
  public ProgramCardViewRenderer(
      ApplicantRoutes applicantRoutes,
      ProfileUtils profileUtils,
      ProgramImageUtils programImageUtils,
      SettingsManifest settingsManifest,
      ZoneId zoneId) {
    this.applicantRoutes = checkNotNull(applicantRoutes);
    this.profileUtils = checkNotNull(profileUtils);
    this.programImageUtils = checkNotNull(programImageUtils);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.zoneId = checkNotNull(zoneId);
  }

  /**
   * Generates a list of style classes with responsive column counts. The number of columns should
   * not exceed the number of programs, or the program card container will not be centered.
   *
   * @param numPrograms the number of programs to be displayed in the collection
   */
  static String programCardsContainerStyles(int numPrograms) {
    return StyleUtils.joinStyles(
        ApplicantStyles.PROGRAM_CARDS_CONTAINER_BASE,
        numPrograms >= 2 ? StyleUtils.responsiveMedium("grid-cols-2") : "",
        numPrograms >= 3 ? StyleUtils.responsiveLarge("grid-cols-3") : "",
        numPrograms >= 4 ? StyleUtils.responsiveXLarge("grid-cols-4") : "",
        numPrograms >= 5 ? StyleUtils.responsive2XLarge("grid-cols-5") : "");
  }

  /**
   * Renders a collection of programs displayed as a group of cards, delegates to {@code
   * createProgramCard} for individual program cards.
   */
  public DivTag programCardsSection(
      Http.Request request,
      Messages messages,
      ApplicantPersonalInfo personalInfo,
      Optional<MessageKey> sectionTitle,
      String cardContainerStyles,
      Optional<Long> applicantId,
      Locale preferredLocale,
      ImmutableList<ApplicantService.ApplicantProgramData> cards,
      MessageKey buttonTitle,
      MessageKey buttonSrText,
      HtmlBundle bundle,
      Optional<CiviFormProfile> profile,
      boolean isMyApplicationsSection) {
    String sectionHeaderId = Modal.randomModalId();
    DivTag div = div().withClass(ReferenceClasses.APPLICATION_PROGRAM_SECTION);
    if (sectionTitle.isPresent()) {
      div.with(
          h2().withId(sectionHeaderId)
              .withText(messages.at(sectionTitle.get().getKeyName(), cards.size()))
              .withClasses("mb-4", "px-4", "text-xl", "font-semibold"));
    }
    return div.with(
        ol().condAttr(sectionTitle.isPresent(), "aria-labelledby", sectionHeaderId)
            .withClasses(cardContainerStyles)
            .with(
                each(
                    cards,
                    (card) ->
                        createProgramCard(
                            request,
                            messages,
                            personalInfo.getType(),
                            card,
                            applicantId,
                            preferredLocale,
                            buttonTitle,
                            buttonSrText,
                            bundle,
                            profile,
                            zoneId,
                            isMyApplicationsSection))));
  }

  /**
   * Creates and returns DOM representing a program card using the information provided in {@code
   * cardData}.
   *
   * @param buttonTitle a message key for the text displayed on the action button on the bottom of
   *     the card.
   * @param buttonSrText a message key for the screen reader text for the action button on the
   *     bottom of the card.
   */
  public LiTag createProgramCard(
      Http.Request request,
      Messages messages,
      ApplicantPersonalInfo.ApplicantType applicantType,
      ApplicantService.ApplicantProgramData cardData,
      Optional<Long> applicantId,
      Locale preferredLocale,
      MessageKey buttonTitle,
      MessageKey buttonSrText,
      HtmlBundle bundle,
      Optional<CiviFormProfile> profile,
      ZoneId zoneId,
      boolean isInMyApplicationsSection) {
    ProgramDefinition program = cardData.program();

    String baseId = ReferenceClasses.APPLICATION_CARD + "-" + program.id();

    ContainerTag title =
        h3().withId(baseId + "-title")
            .withClasses(ReferenceClasses.APPLICATION_CARD_TITLE, "text-lg", "font-semibold")
            .withText(program.localizedName().getOrDefault(preferredLocale));
    ImmutableList<DomContent> descriptionContent =
        TextFormatter.formatText(
            program.localizedDescription().getOrDefault(preferredLocale),
            /* preserveEmptyLines= */ false,
            /* addRequiredIndicator= */ false,
            messages.at(MessageKey.LINK_OPENS_NEW_TAB_SR.getKeyName()));
    DivTag description =
        div()
            .withId(baseId + "-description")
            .withClasses(
                ReferenceClasses.APPLICATION_CARD_DESCRIPTION, "text-xs", "my-2", "line-clamp-5")
            .with(descriptionContent);

    DivTag programData =
        div()
            .withId(baseId + "-data")
            .withClasses("w-full", "px-4", "pt-4", "h-56", "overflow-auto");

    programData.with(title, description);

    // Create the "Program details" link if an external link if it is present
    String programDetailsLink = program.externalLink();
    if (!programDetailsLink.isEmpty()) {
      ATag infoLink =
          new LinkElement()
              .setId(baseId + "-info-link")
              .setStyles("mb-2", "text-sm", "underline")
              .setText(messages.at(MessageKey.LINK_PROGRAM_DETAILS.getKeyName()))
              .setHref(programDetailsLink)
              .opensInNewTab()
              .setIcon(Icons.OPEN_IN_NEW, LinkElement.IconPosition.END)
              .asAnchorText()
              .attr(
                  "aria-label",
                  messages.at(
                      MessageKey.LINK_PROGRAM_DETAILS_SR.getKeyName(),
                      program.localizedName().getOrDefault(preferredLocale)));
      programData.with(div(infoLink));
    }

    if (isInMyApplicationsSection) {
      programData.with(programCardApplicationStatus(messages, preferredLocale, cardData));
    }

    if (shouldShowEligibilityTag(cardData)) {
      programData.with(
          eligibilityTag(request, messages, cardData.isProgramMaybeEligible().get(), profileUtils));
    }

    String actionUrl =
        profile.isPresent() && applicantId.isPresent()
            ? applicantRoutes
                .review(profile.get(), applicantId.get(), cardData.currentApplicationProgramId())
                .url()
            : applicantRoutes.review(cardData.currentApplicationProgramId()).url();

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

    // If the user is a guest, show the login prompt modal, which has a button
    // to continue on to the application. Otherwise, show the button to go to the
    // application directly.
    ContainerTag content =
        applicantType == GUEST
            ? TagCreator.button().withId(loginPromptModal.getTriggerButtonId())
            : a().withHref(actionUrl).withId(baseId + "-apply");

    content
        .withText(messages.at(buttonTitle.getKeyName()))
        .attr(
            "aria-label",
            messages.at(
                buttonSrText.getKeyName(), program.localizedName().getOrDefault(preferredLocale)))
        .withClasses(ReferenceClasses.APPLY_BUTTON, ButtonStyles.SOLID_BLUE_TEXT_SM);

    DivTag categoriesDiv =
        !isInMyApplicationsSection
            ? div()
                .withClasses("flex", "flex-wrap", "gap-2", "mx-4", "mt-4")
                .with(
                    each(
                        program.categories(),
                        (category) ->
                            div()
                                .withClasses("text-xs", "bg-gray-200", "px-2", "py-1")
                                .withText(
                                    category.getLocalizedName().getOrDefault(preferredLocale))))
            : div();

    DivTag actionDiv = div(content).withClasses("mt-2", "mb-6", "mx-4", "flex");
    LiTag cardListItem =
        li().withId(baseId)
            .withClasses(ReferenceClasses.APPLICATION_CARD, ApplicantStyles.PROGRAM_CARD);

    Optional<ImgTag> programImage =
        isInMyApplicationsSection
            ? Optional.empty()
            : programImageUtils.createProgramImage(
                program, preferredLocale, /* isWithinProgramCard= */ true);

    programImage.ifPresent(cardListItem::with);

    return cardListItem.with(categoriesDiv).with(programData).with(actionDiv);
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

  private PTag programCardApplicationStatus(
      Messages messages, Locale preferredLocale, ApplicantService.ApplicantProgramData cardData) {
    boolean isSubmitted = cardData.latestSubmittedApplicationTime().isPresent();
    boolean hasStatus = cardData.latestSubmittedApplicationStatus().isPresent();
    String badgeText = messages.at(MessageKey.TITLE_PROGRAMS_IN_PROGRESS_UPDATED.getKeyName());

    if (isSubmitted) {
      String submitDate =
          messages.at(
              MessageKey.SUBMITTED_DATE.getKeyName(),
              getFormattedSubmitDate(cardData.latestSubmittedApplicationTime().get(), zoneId));
      badgeText =
          hasStatus
              ? String.format(
                  "%s (%s)",
                  cardData
                      .latestSubmittedApplicationStatus()
                      .get()
                      .localizedStatusText()
                      .getOrDefault(preferredLocale),
                  submitDate)
              : submitDate;
    }

    return p().withClasses("border", "px-1", "mt-2", "flex", "items-center", "w-fit", "bg-blue-100")
        .with(
            Icons.svg(Icons.INFO)
                // 4.5 is 18px as defined in tailwind.config.js
                .withClasses("inline-block", "h-4.5", "w-4.5", BaseStyles.TEXT_CIVIFORM_BLUE),
            span(badgeText)
                .withClasses(
                    "p-1",
                    "text-xs",
                    "font-medium",
                    "w-11/12",
                    BaseStyles.TEXT_CIVIFORM_BLUE,
                    ReferenceClasses.BT_DATE));
  }

  private PTag eligibilityTag(
      Http.Request request, Messages messages, boolean isEligible, ProfileUtils profileUtils) {
    CiviFormProfile submittingProfile = profileUtils.currentUserProfile(request);
    boolean isTrustedIntermediary = submittingProfile.isTrustedIntermediary();
    MessageKey mayQualifyMessage =
        isTrustedIntermediary ? MessageKey.TAG_MAY_QUALIFY_TI : MessageKey.TAG_MAY_QUALIFY;
    MessageKey mayNotQualifyMessage =
        isTrustedIntermediary ? MessageKey.TAG_MAY_NOT_QUALIFY_TI : MessageKey.TAG_MAY_NOT_QUALIFY;
    Icons icon = isEligible ? Icons.CHECK_CIRCLE : Icons.INFO;
    String color = isEligible ? BaseStyles.BG_CIVIFORM_GREEN_LIGHT : "bg-gray-200";
    String textColor = isEligible ? BaseStyles.TEXT_CIVIFORM_GREEN : "text-black";
    String tagClass =
        isEligible ? ReferenceClasses.ELIGIBLE_TAG : ReferenceClasses.NOT_ELIGIBLE_TAG;
    String tagText =
        isEligible ? mayQualifyMessage.getKeyName() : mayNotQualifyMessage.getKeyName();

    return p().withClasses(
            tagClass, "border", "px-1", "mt-2", "flex", "items-center", "w-fit", color)
        .with(
            Icons.svg(icon)
                // 4.5 is 18px as defined in tailwind.config.js
                .withClasses("inline-block", "h-4.5", "w-4.5", textColor),
            span(messages.at(tagText)).withClasses("p-1", "text-xs", "font-medium", textColor));
  }

  private String getFormattedSubmitDate(Instant submittedDate, ZoneId zoneId) {
    ZonedDateTime dateTime = submittedDate.atZone(zoneId);
    String formattedSubmitTime =
        DateTimeFormatter.ofLocalizedDate(
                // SHORT will print dates as 1/2/2022.
                FormatStyle.SHORT)
            .format(dateTime);
    return formattedSubmitTime;
  }
}
