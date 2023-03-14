package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.h4;
import static j2html.TagCreator.img;
import static j2html.TagCreator.li;
import static j2html.TagCreator.ol;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;
import static j2html.TagCreator.text;

import auth.CiviFormProfile;
import auth.ProfileUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import featureflags.FeatureFlags;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.H1Tag;
import j2html.tags.specialized.H2Tag;
import j2html.tags.specialized.ImgTag;
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
import services.applicant.ApplicantService;
import services.program.ProgramDefinition;
import services.program.StatusDefinitions;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.TranslationUtils;
import views.components.Icons;
import views.components.LinkElement;
import views.components.Modal;
import views.components.TextFormatter;
import views.components.ToastMessage;
import views.style.ApplicantStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Returns a list of programs that an applicant can browse, with buttons for applying. */
public final class ProgramIndexView extends BaseHtmlView {

  private final ApplicantLayout layout;
  private final FeatureFlags featureFlags;
  private final ProfileUtils profileUtils;
  private final ZoneId zoneId;

  @Inject
  public ProgramIndexView(
      ApplicantLayout layout,
      ZoneId zoneId,
      FeatureFlags featureFlags,
      ProfileUtils profileUtils) {
    this.layout = checkNotNull(layout);
    this.featureFlags = checkNotNull(featureFlags);
    this.profileUtils = checkNotNull(profileUtils);
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
      Optional<String> userName,
      ApplicantService.ApplicationPrograms applicationPrograms,
      Optional<ToastMessage> bannerMessage) {
    HtmlBundle bundle = layout.getBundle();
    bundle.setTitle(messages.at(MessageKey.CONTENT_GET_BENEFITS.getKeyName()));
    bannerMessage.ifPresent(bundle::addToastMessages);
    bundle.addMainContent(
        topContent(
            messages.at(MessageKey.CONTENT_GET_BENEFITS.getKeyName()),
            messages.at(MessageKey.CONTENT_CIVIFORM_DESCRIPTION_1.getKeyName()),
            messages.at(MessageKey.CONTENT_CIVIFORM_DESCRIPTION_2.getKeyName())),
        mainContent(
            request, messages, applicationPrograms, applicantId, messages.lang().toLocale()));

    return layout.renderWithNav(request, userName, messages, bundle);
  }

  private DivTag topContent(String titleText, String infoTextLine1, String infoTextLine2) {
    // "Get benefits"
    H1Tag programIndexH1 =
        h1().withText(titleText)
            .withClasses(
                "text-4xl",
                StyleUtils.responsiveSmall("text-5xl"),
                "font-semibold",
                "mt-10",
                "px-6",

                StyleUtils.responsiveSmall("mb-6"));

    DivTag infoLine1Div =
        div()
            .withText(infoTextLine1)
            .withClasses("text-sm", "px-6", StyleUtils.responsiveSmall("text-base"));

    DivTag infoLine2Div =
        div()
            .withText(infoTextLine2)
            .withClasses("text-sm", "px-6", "pb-6", StyleUtils.responsiveSmall("text-base"));

    return div()
        .withId("top-content")
        .withClasses(ApplicantStyles.PROGRAM_INDEX_TOP_CONTENT, "relative", "flex",
          "flex-col")
        .with(programIndexH1, infoLine1Div, infoLine2Div);
  }

  private H2Tag programSectionTitle(String title) {
    return h2().withText(title).withClasses("mb-4", "px-4", "text-xl", "font-semibold");
  }

  private DivTag mainContent(
      Http.Request request,
      Messages messages,
      ApplicantService.ApplicationPrograms relevantPrograms,
      long applicantId,
      Locale preferredLocale) {
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

    if (featureFlags.isIntakeFormEnabled(request)
        && relevantPrograms.commonIntakeForm().isPresent()) {
      content.with(
          findServicesSection(
              request,
              messages,
              relevantPrograms,
              cardContainerStyles,
              applicantId,
              preferredLocale),
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
              Optional.of(MessageKey.TITLE_PROGRAMS_IN_PROGRESS_UPDATED),
              cardContainerStyles,
              applicantId,
              preferredLocale,
              relevantPrograms.inProgress(),
              MessageKey.BUTTON_CONTINUE,
              MessageKey.BUTTON_CONTINUE_SR));
    }
    if (!relevantPrograms.submitted().isEmpty()) {
      content.with(
          programCardsSection(
              request,
              messages,
              Optional.of(MessageKey.TITLE_PROGRAMS_SUBMITTED),
              cardContainerStyles,
              applicantId,
              preferredLocale,
              relevantPrograms.submitted(),
              MessageKey.BUTTON_EDIT,
              MessageKey.BUTTON_EDIT_SR));
    }
    if (!relevantPrograms.unapplied().isEmpty()) {
      content.with(
          programCardsSection(
              request,
              messages,
              Optional.of(MessageKey.TITLE_PROGRAMS_ACTIVE_UPDATED),
              cardContainerStyles,
              applicantId,
              preferredLocale,
              relevantPrograms.unapplied(),
              MessageKey.BUTTON_APPLY,
              MessageKey.BUTTON_APPLY_SR));
    }

    return div().withClasses("flex", "flex-col", "place-items-center").with(content);
  }

  private DivTag findServicesSection(
      Http.Request request,
      Messages messages,
      ApplicantService.ApplicationPrograms relevantPrograms,
      String cardContainerStyles,
      long applicantId,
      Locale preferredLocale) {
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
                Optional.empty(),
                cardContainerStyles,
                applicantId,
                preferredLocale,
                ImmutableList.of(relevantPrograms.commonIntakeForm().get()),
                buttonText,
                buttonScreenReaderText));
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
      Optional<MessageKey> sectionTitle,
      String cardContainerStyles,
      long applicantId,
      Locale preferredLocale,
      ImmutableList<ApplicantService.ApplicantProgramData> cards,
      MessageKey buttonTitle,
      MessageKey buttonSrText) {
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
                            card,
                            applicantId,
                            preferredLocale,
                            buttonTitle,
                            buttonSrText,
                            sectionTitle.isPresent()))));
  }

  private LiTag programCard(
      Http.Request request,
      Messages messages,
      ApplicantService.ApplicantProgramData cardData,
      Long applicantId,
      Locale preferredLocale,
      MessageKey buttonTitle,
      MessageKey buttonSrText,
      boolean nestedUnderSubheading) {
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
        TextFormatter.createLinksAndEscapeText(
            program.localizedDescription().getOrDefault(preferredLocale),
            TextFormatter.UrlOpenAction.NewTab,
            /* addRequiredIndicator= */ false);
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
              preferredLocale, cardData.latestSubmittedApplicationStatus().get()));
    }
    if (shouldShowEligibilityTag(request, cardData)) {
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
    ATag actionButton =
        a().withHref(actionUrl)
            .attr(
                "aria-label",
                messages.at(
                    buttonSrText.getKeyName(),
                    program.localizedName().getOrDefault(preferredLocale)))
            .withText(messages.at(buttonTitle.getKeyName()))
            .withId(baseId + "-apply")
            .withClasses(ReferenceClasses.APPLY_BUTTON, ApplicantStyles.BUTTON_PROGRAM_APPLY);

    DivTag actionDiv =
        div(actionButton).withClasses("w-full", "mb-6", "flex-grow", "flex", "items-end");
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
  private boolean shouldShowEligibilityTag(
      Http.Request request, ApplicantService.ApplicantProgramData cardData) {
    if (!featureFlags.isProgramEligibilityConditionsEnabled(request)) {
      return false;
    }

    if (!cardData.isProgramMaybeEligible().isPresent()) {
      return false;
    }

    return !featureFlags.isNongatedEligibilityEnabled(request)
        || cardData.program().eligibilityIsGating()
        || cardData.isProgramMaybeEligible().get();
  }

  private PTag programCardApplicationStatus(
      Locale preferredLocale, StatusDefinitions.Status status) {
    return p().withClasses("border", "rounded-lg", "px-2", "py-1", "mb-4", "bg-blue-100")
        .with(
            span(status.localizedStatusText().getOrDefault(preferredLocale))
                .withClasses("text-xs", "font-medium"));
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
                .withClasses("inline-block")
                // Can't set 18px using Tailwind CSS classes.
                .withStyle("width: 18px; height: 18px;"),
            span(messages.at(tagText)).withClasses("p-2", "text-xs", "font-medium"));
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
