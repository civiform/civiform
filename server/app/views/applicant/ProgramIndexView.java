package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.h4;
import static j2html.TagCreator.li;
import static j2html.TagCreator.ol;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;
import static services.applicant.ApplicantPersonalInfo.ApplicantType.GUEST;
import static views.applicant.AuthenticateUpsellCreator.createLoginPromptModal;
import static views.components.Modal.RepeatOpenBehavior.Group.PROGRAMS_INDEX_LOGIN_PROMPT;

import annotations.BindingAnnotations;
import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import controllers.routes;
import j2html.TagCreator;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.H1Tag;
import j2html.tags.specialized.H2Tag;
import j2html.tags.specialized.LiTag;
import j2html.tags.specialized.PTag;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import models.LifecycleStage;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.program.ProgramDefinition;
import services.program.StatusDefinitions;
import services.settings.SettingsManifest;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.TranslationUtils;
import views.components.ButtonStyles;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.components.Modal.RepeatOpenBehavior;
import views.components.TextFormatter;
import views.components.ToastMessage;
import views.style.ApplicantStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Returns a list of programs that an applicant can browse, with buttons for applying. */
public final class ProgramIndexView extends BaseHtmlView {

  private final ApplicantLayout layout;
  private final SettingsManifest settingsManifest;
  private final ProfileUtils profileUtils;
  private final String authProviderName;
  private final ZoneId zoneId;

  @Inject
  public ProgramIndexView(
      ApplicantLayout layout,
      ZoneId zoneId,
      SettingsManifest settingsManifest,
      ProfileUtils profileUtils,
      @BindingAnnotations.ApplicantAuthProviderName String authProviderName) {
    this.layout = checkNotNull(layout);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.profileUtils = checkNotNull(profileUtils);
    this.authProviderName = checkNotNull(authProviderName);
    this.zoneId = checkNotNull(zoneId);
  }

  /**
   * For each program in the list, render the program information along with an "Apply" button that
   * redirects the user to that program's application.
   *
   * @param messages the localized {@link Messages} for the current applicant
   * @param applicantId the ID of the current applicant
   * @param applicationPrograms an {@link ImmutableList} of programs (with attached application)
   *     information that should be displayed in the list
   * @return HTML content for rendering the list of available programs
   */
  public Content render(
      Messages messages,
      Http.Request request,
      long applicantId,
      ApplicantPersonalInfo personalInfo,
      ApplicantService.ApplicationPrograms applicationPrograms,
      Optional<ToastMessage> bannerMessage) {
    HtmlBundle bundle = layout.getBundle(request);
    bundle.setTitle(messages.at(MessageKey.CONTENT_GET_BENEFITS.getKeyName()));
    bannerMessage.ifPresent(bundle::addToastMessages);

    String sessionEndedMessage = messages.at(MessageKey.TOAST_SESSION_ENDED.getKeyName());
    bundle.addToastMessages(
        ToastMessage.success(sessionEndedMessage)
            .setCondOnStorageKey("session_just_ended")
            .setDuration(5000));

    bundle.addMainContent(
        topContent(request, messages, personalInfo),
        mainContent(
            request,
            messages,
            personalInfo,
            applicationPrograms,
            applicantId,
            messages.lang().toLocale(),
            bundle));

    return layout.renderWithNav(
        request, personalInfo, messages, bundle, /*includeAdminLogin=*/ true, applicantId);
  }

  private DivTag topContent(
      Http.Request request, Messages messages, ApplicantPersonalInfo personalInfo) {

    String h1Text, infoDivText, widthClass;

    if (personalInfo.getType() == GUEST) {
      // "Save time when applying for benefits"
      h1Text = messages.at(MessageKey.CONTENT_SAVE_TIME.getKeyName());
      infoDivText =
          messages.at(MessageKey.CONTENT_GUEST_DESCRIPTION.getKeyName(), authProviderName);
      widthClass = "w-8/12";
    } else { // Logged in.
      // "Get benefits"
      h1Text = messages.at(MessageKey.CONTENT_GET_BENEFITS.getKeyName());
      infoDivText =
          messages.at(
              MessageKey.CONTENT_CIVIFORM_DESCRIPTION.getKeyName(),
              settingsManifest.getWhitelabelCivicEntityShortName(request).get());
      widthClass = "w-5/12";
    }

    H1Tag programIndexH1 =
        h1().withText(h1Text)
            .withClasses(
                "text-4xl",
                StyleUtils.responsiveSmall("text-5xl"),
                "font-semibold",
                "mt-10",
                "px-6",
                StyleUtils.responsiveSmall("mb-6"));

    DivTag infoDiv =
        div()
            .withText(infoDivText)
            .withClasses(
                "text-sm", "px-6", widthClass, "pb-6", StyleUtils.responsiveSmall("text-base"));

    return div()
        .withId("top-content")
        .withClasses(
            ApplicantStyles.PROGRAM_INDEX_TOP_CONTENT,
            "relative",
            "flex",
            "flex-col",
            "items-center")
        .with(programIndexH1, infoDiv)
        .condWith(
            personalInfo.getType() == GUEST,
            // Log in and Create account buttons if user is a guest.
            div()
                .with(
                    redirectButton(
                            "login-button",
                            messages.at(MessageKey.BUTTON_LOGIN.getKeyName()),
                            routes.LoginController.applicantLogin(Optional.empty()).url())
                        .withClasses(ButtonStyles.SOLID_WHITE, "basis-60"))
                .with(
                    redirectButton(
                            "create-account",
                            messages.at(MessageKey.BUTTON_CREATE_ACCOUNT.getKeyName()),
                            routes.LoginController.register().url())
                        .withClasses(ButtonStyles.SOLID_WHITE, "basis-60"))
                .withClasses(
                    "flex",
                    "flex-row",
                    "gap-x-8",
                    "pb-6",
                    "px-4",
                    "w-screen",
                    "place-content-center"));
  }

  private H2Tag programSectionTitle(String title) {
    return h2().withText(title).withClasses("mb-4", "px-4", "text-xl", "font-semibold");
  }

  private DivTag mainContent(
      Http.Request request,
      Messages messages,
      ApplicantPersonalInfo personalInfo,
      ApplicantService.ApplicationPrograms relevantPrograms,
      long applicantId,
      Locale preferredLocale,
      HtmlBundle bundle) {
    DivTag content =
        div()
            .withId("main-content")
            .withClasses("mx-auto", "my-4", StyleUtils.responsiveSmall("m-10"));

    // The different program card containers should have the same styling, by using the program
    // count of the larger set of programs
    String cardContainerStyles =
        programCardsContainerStyles(
            Math.max(
                Math.max(relevantPrograms.unapplied().size(), relevantPrograms.submitted().size()),
                relevantPrograms.inProgress().size()));

    if (settingsManifest.getIntakeFormEnabled(request)
        && relevantPrograms.commonIntakeForm().isPresent()) {
      content.with(
          findServicesSection(
              request,
              messages,
              personalInfo,
              relevantPrograms,
              cardContainerStyles,
              applicantId,
              preferredLocale,
              bundle),
          div().withClass("mb-12"),
          programSectionTitle(
              messages.at(
                  MessageKey.TITLE_ALL_PROGRAMS_SECTION.getKeyName(),
                  relevantPrograms.inProgress().size()
                      + relevantPrograms.submitted().size()
                      + relevantPrograms.unapplied().size())));
    } else {
      content.with(programSectionTitle(messages.at(MessageKey.TITLE_PROGRAMS.getKeyName())));
    }

    if (!relevantPrograms.inProgress().isEmpty()) {
      content.with(
          programCardsSection(
              request,
              messages,
              personalInfo,
              Optional.of(MessageKey.TITLE_PROGRAMS_IN_PROGRESS_UPDATED),
              cardContainerStyles,
              applicantId,
              preferredLocale,
              relevantPrograms.inProgress(),
              MessageKey.BUTTON_CONTINUE,
              MessageKey.BUTTON_CONTINUE_SR,
              bundle));
    }
    if (!relevantPrograms.submitted().isEmpty()) {
      content.with(
          programCardsSection(
              request,
              messages,
              personalInfo,
              Optional.of(MessageKey.TITLE_PROGRAMS_SUBMITTED),
              cardContainerStyles,
              applicantId,
              preferredLocale,
              relevantPrograms.submitted(),
              MessageKey.BUTTON_EDIT,
              MessageKey.BUTTON_EDIT_SR,
              bundle));
    }
    if (!relevantPrograms.unapplied().isEmpty()) {
      content.with(
          programCardsSection(
              request,
              messages,
              personalInfo,
              Optional.of(MessageKey.TITLE_PROGRAMS_ACTIVE_UPDATED),
              cardContainerStyles,
              applicantId,
              preferredLocale,
              relevantPrograms.unapplied(),
              MessageKey.BUTTON_APPLY,
              MessageKey.BUTTON_APPLY_SR,
              bundle));
    }

    return div().withClasses("flex", "flex-col", "place-items-center").with(content);
  }

  private DivTag findServicesSection(
      Http.Request request,
      Messages messages,
      ApplicantPersonalInfo personalInfo,
      ApplicantService.ApplicationPrograms relevantPrograms,
      String cardContainerStyles,
      long applicantId,
      Locale preferredLocale,
      HtmlBundle bundle) {
    Optional<LifecycleStage> commonIntakeFormApplicationStatus =
        relevantPrograms.commonIntakeForm().get().latestApplicationLifecycleStage();
    MessageKey buttonText = MessageKey.BUTTON_START_HERE;
    MessageKey buttonScreenReaderText = MessageKey.BUTTON_START_HERE_COMMON_INTAKE_SR;
    if (commonIntakeFormApplicationStatus.isPresent()) {
      switch (commonIntakeFormApplicationStatus.get()) {
        case ACTIVE:
          buttonText = MessageKey.BUTTON_EDIT;
          buttonScreenReaderText = MessageKey.BUTTON_EDIT_COMMON_INTAKE_SR;
          break;
        case DRAFT:
          buttonText = MessageKey.BUTTON_CONTINUE;
          buttonScreenReaderText = MessageKey.BUTTON_CONTINUE_COMMON_INTAKE_SR;
          break;
        default:
          // Leave button text as is.
      }
    }
    return div()
        .withClass(ReferenceClasses.APPLICATION_PROGRAM_SECTION)
        .with(programSectionTitle(messages.at(MessageKey.TITLE_FIND_SERVICES_SECTION.getKeyName())))
        .with(
            programCardsSection(
                request,
                messages,
                personalInfo,
                Optional.empty(),
                cardContainerStyles,
                applicantId,
                preferredLocale,
                ImmutableList.of(relevantPrograms.commonIntakeForm().get()),
                buttonText,
                buttonScreenReaderText,
                bundle));
  }

  /**
   * This method generates a list of style classes with responsive column counts. The number of
   * columns should not exceed the number of programs, or the program card container will not be
   * centered.
   */
  private String programCardsContainerStyles(int numPrograms) {
    return StyleUtils.joinStyles(
        ApplicantStyles.PROGRAM_CARDS_CONTAINER_BASE,
        numPrograms >= 2 ? StyleUtils.responsiveMedium("grid-cols-2") : "",
        numPrograms >= 3 ? StyleUtils.responsiveLarge("grid-cols-3") : "",
        numPrograms >= 4 ? StyleUtils.responsiveXLarge("grid-cols-4") : "",
        numPrograms >= 5 ? StyleUtils.responsive2XLarge("grid-cols-5") : "");
  }

  private DivTag programCardsSection(
      Http.Request request,
      Messages messages,
      ApplicantPersonalInfo personalInfo,
      Optional<MessageKey> sectionTitle,
      String cardContainerStyles,
      long applicantId,
      Locale preferredLocale,
      ImmutableList<ApplicantService.ApplicantProgramData> cards,
      MessageKey buttonTitle,
      MessageKey buttonSrText,
      HtmlBundle bundle) {
    String sectionHeaderId = Modal.randomModalId();
    DivTag div = div().withClass(ReferenceClasses.APPLICATION_PROGRAM_SECTION);
    if (sectionTitle.isPresent()) {
      div.with(
          h3().withId(sectionHeaderId)
              .withText(messages.at(sectionTitle.get().getKeyName()))
              .withClasses(ApplicantStyles.PROGRAM_CARDS_SUBTITLE));
    }
    return div.with(
        ol().attr("aria-labelledby", sectionHeaderId)
            .withClasses(cardContainerStyles)
            .with(
                each(
                    cards,
                    (card) ->
                        programCard(
                            request,
                            messages,
                            personalInfo,
                            card,
                            applicantId,
                            preferredLocale,
                            buttonTitle,
                            buttonSrText,
                            sectionTitle.isPresent(),
                            bundle))));
  }

  private LiTag programCard(
      Http.Request request,
      Messages messages,
      ApplicantPersonalInfo personalInfo,
      ApplicantService.ApplicantProgramData cardData,
      Long applicantId,
      Locale preferredLocale,
      MessageKey buttonTitle,
      MessageKey buttonSrText,
      boolean nestedUnderSubheading,
      HtmlBundle bundle) {
    ProgramDefinition program = cardData.program();

    String baseId = ReferenceClasses.APPLICATION_CARD + "-" + program.id();

    ContainerTag title =
        nestedUnderSubheading
            ? h4().withId(baseId + "-title")
                .withClasses(ReferenceClasses.APPLICATION_CARD_TITLE, "text-lg", "font-semibold")
                .withText(program.localizedName().getOrDefault(preferredLocale))
            : h3().withId(baseId + "-title")
                .withClasses(ReferenceClasses.APPLICATION_CARD_TITLE, "text-lg", "font-semibold")
                .withText(program.localizedName().getOrDefault(preferredLocale));
    ImmutableList<DomContent> descriptionContent =
        TextFormatter.formatText(
            program.localizedDescription().getOrDefault(preferredLocale),
            /*preserveEmptyLines= */ false,
            /*addRequiredIndicator= */ false, messages);
    DivTag description =
        div()
            .withId(baseId + "-description")
            .withClasses(
                ReferenceClasses.APPLICATION_CARD_DESCRIPTION, "text-xs", "my-2", "line-clamp-5")
            .with(descriptionContent);

    DivTag programData =
        div().withId(baseId + "-data").withClasses("w-full", "px-4", "overflow-auto");
    if (cardData.latestSubmittedApplicationStatus().isPresent()) {
      programData.with(
          programCardApplicationStatus(
              messages, preferredLocale, cardData.latestSubmittedApplicationStatus().get()));
    }
    if (shouldShowEligibilityTag(cardData)) {
      programData.with(eligibilityTag(request, messages, cardData.isProgramMaybeEligible().get()));
    }
    programData.with(title, description);
    // Use external link if it is present else use the default Program details page
    String programDetailsLink =
        program.externalLink().isEmpty()
            ? controllers.applicant.routes.ApplicantProgramsController.view(
                    applicantId, program.id())
                .url()
            : program.externalLink();
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

    if (cardData.latestSubmittedApplicationTime().isPresent()) {
      programData.with(
          programCardSubmittedDate(messages, cardData.latestSubmittedApplicationTime().get()));
    }

    String actionUrl =
        controllers.applicant.routes.ApplicantProgramReviewController.review(
                applicantId, program.id())
            .url();

    Modal loginPromptModal =
        createLoginPromptModal(
                messages,
                actionUrl,
                messages.at(
                    MessageKey.INITIAL_LOGIN_MODAL_PROMPT.getKeyName(),
                    settingsManifest.getApplicantPortalName(request).get()),
                MessageKey.BUTTON_CONTINUE_TO_APPLICATION)
            .setRepeatOpenBehavior(
                RepeatOpenBehavior.showOnlyOnce(PROGRAMS_INDEX_LOGIN_PROMPT, actionUrl))
            .build();
    bundle.addModals(loginPromptModal);

    // If the user is a guest, show the login prompt modal, which has a button
    // to continue on to the application. Otherwise, show the button to go to the
    // application directly.
    ContainerTag content =
        personalInfo.getType() == GUEST
            ? TagCreator.button().withId(loginPromptModal.getTriggerButtonId())
            : a().withHref(actionUrl).withId(baseId + "-apply");

    content
        .withText(messages.at(buttonTitle.getKeyName()))
        .attr(
            "aria-label",
            messages.at(
                buttonSrText.getKeyName(), program.localizedName().getOrDefault(preferredLocale)))
        .withClasses(ReferenceClasses.APPLY_BUTTON, ButtonStyles.SOLID_BLUE_TEXT_SM, "mx-auto");

    DivTag actionDiv = div(content).withClasses("w-full", "mb-6", "flex-grow", "flex", "items-end");
    return li().withId(baseId)
        .withClasses(ReferenceClasses.APPLICATION_CARD, ApplicantStyles.PROGRAM_CARD)
        .with(
            // The visual bar at the top of each program card.
            div()
                .withClasses(
                    "block", "shrink-0", BaseStyles.BG_SEATTLE_BLUE, "rounded-t-xl", "h-3"))
        .with(programData)
        .with(actionDiv);
  }

  /**
   * If eligibility is gating, the eligibility tag should always show when present. If eligibility
   * is non-gating, the eligibility tag should only show if the user may be eligible.
   */
  private boolean shouldShowEligibilityTag(ApplicantService.ApplicantProgramData cardData) {
    if (!cardData.isProgramMaybeEligible().isPresent()) {
      return false;
    }

    return cardData.program().eligibilityIsGating() || cardData.isProgramMaybeEligible().get();
  }

  private PTag programCardApplicationStatus(
      Messages messages, Locale preferredLocale, StatusDefinitions.Status status) {
    return p().withClasses(
            "border",
            "rounded-full",
            "px-2",
            "py-1",
            "mb-4",
            "gap-x-2",
            "inline-block",
            "w-auto",
            "bg-blue-100")
        .with(
            Icons.svg(Icons.INFO)
                // 4.5 is 18px as defined in tailwind.config.js
                .withClasses("inline-block", "h-4.5", "w-4.5", BaseStyles.TEXT_SEATTLE_BLUE),
            span(String.format(
                    "%s: %s",
                    messages.at(MessageKey.TITLE_STATUS.getKeyName()),
                    status.localizedStatusText().getOrDefault(preferredLocale)))
                .withClasses("p-2", "text-xs", "font-medium", BaseStyles.TEXT_SEATTLE_BLUE));
  }

  private PTag eligibilityTag(Http.Request request, Messages messages, boolean isEligible) {
    CiviFormProfile submittingProfile = profileUtils.currentUserProfile(request).orElseThrow();
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
            tagClass,
            "border",
            "rounded-full",
            "px-2",
            "py-1",
            "mb-4",
            "gap-x-2",
            "inline-block",
            "w-auto",
            color)
        .with(
            Icons.svg(icon)
                // 4.5 is 18px as defined in tailwind.config.js
                .withClasses("inline-block", "h-4.5", "w-4.5", textColor),
            span(messages.at(tagText)).withClasses("p-2", "text-xs", "font-medium", textColor));
  }

  private DivTag programCardSubmittedDate(Messages messages, Instant submittedDate) {
    TranslationUtils.TranslatedStringSplitResult translateResult =
        TranslationUtils.splitTranslatedSingleArgString(messages, MessageKey.SUBMITTED_DATE);
    String beforeContent = translateResult.beforeInterpretedContent();
    String afterContent = translateResult.afterInterpretedContent();

    List<DomContent> submittedComponents = Lists.newArrayList();
    if (!beforeContent.isEmpty()) {
      submittedComponents.add(text(beforeContent));
    }

    ZonedDateTime dateTime = submittedDate.atZone(zoneId);
    String formattedSubmitTime =
        DateTimeFormatter.ofLocalizedDate(
                // SHORT will print dates as 1/2/2022.
                FormatStyle.SHORT)
            .format(dateTime);
    submittedComponents.add(
        span(formattedSubmitTime).withClasses(ReferenceClasses.BT_DATE, "font-semibold"));

    if (!afterContent.isEmpty()) {
      submittedComponents.add(text(afterContent));
    }

    return div().withClasses("text-xs", "text-gray-700").with(submittedComponents);
  }
}
