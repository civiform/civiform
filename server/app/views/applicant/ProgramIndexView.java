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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import j2html.tags.DomContent;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.H1Tag;
import j2html.tags.specialized.H4Tag;
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
  private final Optional<String> maybeLogoUrl;
  private final String civicEntityFullName;
  private final ZoneId zoneId;

  @Inject
  public ProgramIndexView(ApplicantLayout layout, Config config, ZoneId zoneId) {
    this.layout = checkNotNull(layout);
    this.maybeLogoUrl =
        checkNotNull(config).hasPath("whitelabel.logo_with_name_url")
            ? Optional.of(config.getString("whitelabel.logo_with_name_url"))
            : Optional.empty();
    this.civicEntityFullName = checkNotNull(config).getString("whitelabel.civic_entity_full_name");
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
        mainContent(messages, applicationPrograms, applicantId, messages.lang().toLocale()));

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
                "mb-2",
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

    ImgTag logoImg =
        maybeLogoUrl.isPresent()
            ? img().withSrc(maybeLogoUrl.get())
            : this.layout.viewUtils.makeLocalImageTag("Seattle-logo_horizontal_blue-white_small");

    DivTag logoDiv =
        div()
            .with(
                logoImg
                    .withAlt(civicEntityFullName + " logo")
                    .attr("aria-hidden", "true")
                    .withStyle("max-width: 155px; max-height: 40px;"))
            .withClasses("pt-6", "px-6");
    return div()
        .withId("top-content")
        .withClasses(ApplicantStyles.PROGRAM_INDEX_TOP_CONTENT, "relative")
        .with(logoDiv, programIndexH1, infoLine1Div, infoLine2Div);
  }

  private DivTag mainContent(
      Messages messages,
      ApplicantService.ApplicationPrograms relevantPrograms,
      long applicantId,
      Locale preferredLocale) {
    DivTag content =
        div()
            .withId("main-content")
            .withClasses("mx-auto", "my-4", StyleUtils.responsiveSmall("m-10"))
            .with(
                h2().withText(messages.at(MessageKey.TITLE_PROGRAMS.getKeyName()))
                    .withClasses("mb-4", "px-4", "text-xl", "font-semibold"));

    // The different program card containers should have the same styling, by using the program
    // count of the larger set of programs
    String cardContainerStyles =
        programCardsContainerStyles(
            Math.max(
                Math.max(relevantPrograms.unapplied().size(), relevantPrograms.submitted().size()),
                relevantPrograms.inProgress().size()));

    if (!relevantPrograms.inProgress().isEmpty()) {
      content.with(
          programCardsSection(
              messages,
              MessageKey.TITLE_PROGRAMS_IN_PROGRESS_UPDATED,
              cardContainerStyles,
              applicantId,
              preferredLocale,
              relevantPrograms.inProgress(),
              MessageKey.BUTTON_CONTINUE,
              // TODO(#3577): Once button.continueSr translations are available, switch to using
              // those.
              MessageKey.BUTTON_APPLY_SR));
    }
    if (!relevantPrograms.submitted().isEmpty()) {
      content.with(
          programCardsSection(
              messages,
              MessageKey.TITLE_PROGRAMS_SUBMITTED,
              cardContainerStyles,
              applicantId,
              preferredLocale,
              relevantPrograms.submitted(),
              MessageKey.BUTTON_EDIT,
              // TODO(#3577): Once button.editSr translations are available, switch to using
              // those.
              MessageKey.BUTTON_APPLY_SR));
    }
    if (!relevantPrograms.unapplied().isEmpty()) {
      content.with(
          programCardsSection(
              messages,
              MessageKey.TITLE_PROGRAMS_ACTIVE_UPDATED,
              cardContainerStyles,
              applicantId,
              preferredLocale,
              relevantPrograms.unapplied(),
              MessageKey.BUTTON_APPLY,
              MessageKey.BUTTON_APPLY_SR));
    }

    return div().withClasses("flex", "flex-col", "place-items-center").with(content);
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
      Messages messages,
      MessageKey sectionTitle,
      String cardContainerStyles,
      long applicantId,
      Locale preferredLocale,
      ImmutableList<ApplicantService.ApplicantProgramData> cards,
      MessageKey buttonTitle,
      MessageKey buttonSrText) {
    String sectionHeaderId = Modal.randomModalId();
    return div()
        .withClass(ReferenceClasses.APPLICATION_PROGRAM_SECTION)
        .with(
            h3().withId(sectionHeaderId)
                .withText(messages.at(sectionTitle.getKeyName()))
                .withClasses(ApplicantStyles.PROGRAM_CARDS_SUBTITLE))
        .with(
            ol().attr("aria-labelledby", sectionHeaderId)
                .withClasses(cardContainerStyles)
                .with(
                    each(
                        cards,
                        (card) ->
                            programCard(
                                messages,
                                card,
                                applicantId,
                                preferredLocale,
                                buttonTitle,
                                buttonSrText))));
  }

  private LiTag programCard(
      Messages messages,
      ApplicantService.ApplicantProgramData cardData,
      Long applicantId,
      Locale preferredLocale,
      MessageKey buttonTitle,
      MessageKey buttonSrText) {
    ProgramDefinition program = cardData.program();
    String baseId = ReferenceClasses.APPLICATION_CARD + "-" + program.id();

    H4Tag title =
        h4().withId(baseId + "-title")
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
    programData.with(title, description);

    // Add info link.
    String infoUrl =
        controllers.applicant.routes.ApplicantProgramsController.view(applicantId, program.id())
            .url();
    ATag infoLink =
        new LinkElement()
            .setId(baseId + "-info-link")
            .setStyles("mb-2", "text-sm", "underline")
            .setText(messages.at(MessageKey.LINK_PROGRAM_DETAILS.getKeyName()))
            .setHref(infoUrl)
            .asAnchorText()
            .attr(
                "aria-label",
                messages.at(
                    MessageKey.LINK_PROGRAM_DETAILS_SR.getKeyName(),
                    program.localizedName().getOrDefault(preferredLocale)));
    programData.with(div(infoLink));

    // Add external link if it is set.
    if (!program.externalLink().isEmpty()) {
      ATag externalLink =
          new LinkElement()
              .setId(baseId + "-external-link")
              .setStyles("mb-2", "text-sm", "underline")
              .setText(messages.at(MessageKey.EXTERNAL_LINK.getKeyName()))
              .setHref(program.externalLink())
              .opensInNewTab()
              .setIcon(Icons.OPEN_IN_NEW, LinkElement.IconPosition.END)
              .asAnchorText()
              .attr(
                  "aria-label",
                  messages.at(MessageKey.EXTERNAL_LINK_OPENS_IN_NEW_TAB.getKeyName()));

      programData.with(div(externalLink));
    }

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

  private PTag programCardApplicationStatus(
      Locale preferredLocale, StatusDefinitions.Status status) {
    return p().withClasses("border", "rounded-lg", "px-2", "py-1", "mb-4", "bg-blue-100")
        .with(
            span(status.localizedStatusText().getOrDefault(preferredLocale))
                .withClasses("text-xs", "font-medium"));
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
