package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.h3;
import static j2html.TagCreator.h4;
import static j2html.TagCreator.hr;
import static j2html.TagCreator.img;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import j2html.tags.DomContent;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.H1Tag;
import j2html.tags.specialized.ImgTag;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.components.Icons;
import views.components.LinkElement;
import views.components.TextFormatter;
import views.components.ToastMessage;
import views.style.ApplicantStyles;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Returns a list of programs that an applicant can browse, with buttons for applying. */
public class ProgramIndexView extends BaseHtmlView {

  private final ApplicantLayout layout;
  private final Optional<String> maybeLogoUrl;
  private final String civicEntityFullName;

  @Inject
  public ProgramIndexView(ApplicantLayout layout, Config config) {
    this.layout = checkNotNull(layout);
    this.maybeLogoUrl =
        checkNotNull(config).hasPath("whitelabel.logo_with_name_url")
            ? Optional.of(config.getString("whitelabel.logo_with_name_url"))
            : Optional.empty();
    this.civicEntityFullName = checkNotNull(config).getString("whitelabel.civic_entity_full_name");
  }

  /**
   * For each program in the list, render the program information along with an "Apply" button that
   * redirects the user to that program's application.
   *
   * @param messages the localized {@link Messages} for the current applicant
   * @param applicantId the ID of the current applicant
   * @param draftPrograms an {@link ImmutableList} of {@link ProgramDefinition}s which the applicant
   *     has draft applications of
   * @param activePrograms an {@link ImmutableList} of {@link ProgramDefinition}s with the most
   *     recent published versions
   * @return HTML content for rendering the list of available programs
   */
  public Content render(
      Messages messages,
      Http.Request request,
      long applicantId,
      Optional<String> userName,
      ImmutableList<ProgramDefinition> draftPrograms,
      ImmutableList<ProgramDefinition> activePrograms,
      Optional<String> banner) {
    HtmlBundle bundle = layout.getBundle();
    bundle.setTitle(messages.at(MessageKey.CONTENT_GET_BENEFITS.getKeyName()));
    if (banner.isPresent()) {
      bundle.addToastMessages(ToastMessage.alert(banner.get()));
    }
    bundle.addMainContent(
        topContent(
            messages.at(MessageKey.CONTENT_GET_BENEFITS.getKeyName()),
            messages.at(MessageKey.CONTENT_CIVIFORM_DESCRIPTION_1.getKeyName()),
            messages.at(MessageKey.CONTENT_CIVIFORM_DESCRIPTION_2.getKeyName())),
        mainContent(
            messages, draftPrograms, activePrograms, applicantId, messages.lang().toLocale()));

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
            .withClasses(
                "text-sm",
                "px-6",
                "pb-6",
                StyleUtils.responsiveSmall("text-base"));

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
                    .withWidth("175")
                    .withHeight("70"))
            .withClasses("top-2", "left-2");

    return div()
        .withId("top-content")
        .withClasses(ApplicantStyles.PROGRAM_INDEX_TOP_CONTENT, "relative")
        .with(logoDiv, programIndexH1, infoLine1Div, infoLine2Div);
  }

  private DivTag mainContent(
      Messages messages,
      ImmutableList<ProgramDefinition> draftPrograms,
      ImmutableList<ProgramDefinition> activePrograms,
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
        programCardsContainerStyles(Math.max(draftPrograms.size(), activePrograms.size()));

    if (!draftPrograms.isEmpty()) {
      content
          .with(
              h3().withText(messages.at(MessageKey.TITLE_PROGRAMS_IN_PROGRESS.getKeyName()))
                  .withClasses(ApplicantStyles.PROGRAM_CARDS_SUBTITLE))
          .with(
              div()
                  .withClasses(cardContainerStyles)
                  .with(
                      each(
                          IntStream.range(0, draftPrograms.size())
                              .boxed()
                              .collect(Collectors.toList()),
                          index ->
                              programCard(
                                  messages,
                                  draftPrograms.get(index),
                                  index,
                                  draftPrograms.size(),
                                  applicantId,
                                  preferredLocale,
                                  true))));
    }
    if (!draftPrograms.isEmpty() && !activePrograms.isEmpty()) {
      content.with(hr().withClass("my-16"));
    }
    if (!activePrograms.isEmpty()) {
      content
          .with(
              h3().withText(messages.at(MessageKey.TITLE_PROGRAMS_ACTIVE.getKeyName()))
                  .withClasses(ApplicantStyles.PROGRAM_CARDS_SUBTITLE))
          .with(
              div()
                  .withClasses(cardContainerStyles)
                  .with(
                      each(
                          IntStream.range(0, activePrograms.size())
                              .boxed()
                              .collect(Collectors.toList()),
                          index ->
                              programCard(
                                  messages,
                                  activePrograms.get(index),
                                  index,
                                  activePrograms.size(),
                                  applicantId,
                                  preferredLocale,
                                  false))));
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

  private DivTag programCard(
      Messages messages,
      ProgramDefinition program,
      int programIndex,
      int totalProgramCount,
      Long applicantId,
      Locale preferredLocale,
      boolean isDraft) {
    String baseId = ReferenceClasses.APPLICATION_CARD + "-" + program.id();

    DivTag title =
        div()
            .withId(baseId + "-title")
            .withClasses("text-lg", "font-semibold")
            .withText(program.localizedName().getOrDefault(preferredLocale));
    ImmutableList<DomContent> descriptionContent =
        TextFormatter.createLinksAndEscapeText(
            program.localizedDescription().getOrDefault(preferredLocale),
            TextFormatter.UrlOpenAction.NewTab);
    DivTag description =
        div()
            .withId(baseId + "-description")
            .withClasses(
                ReferenceClasses.APPLICATION_CARD_DESCRIPTION,
                "text-xs",
                "my-2",
                "line-clamp-5")
            .with(descriptionContent);

    DivTag programData =
        div()
            .withId(baseId + "-data")
            .withClasses("w-full", "px-4", "overflow-auto")
            .with(title, description);

    // Add info link.
    String infoUrl =
        controllers.applicant.routes.ApplicantProgramsController.view(applicantId, program.id())
            .url();
    ATag infoLink =
        new LinkElement()
            .setId(baseId + "-info-link")
            .setStyles("block", "my-2", "text-sm", "underline")
            .setText(messages.at(MessageKey.LINK_PROGRAM_DETAILS.getKeyName()))
            .setHref(infoUrl)
            .asAnchorText()
            .attr(
                "aria-label",
                messages.at(
                    MessageKey.LINK_PROGRAM_DETAILS_SR.getKeyName(),
                    program.localizedName().getOrDefault(preferredLocale)));
    programData.with(infoLink);

    // Add external link if it is set.
    if (!program.externalLink().isEmpty()) {
      ATag externalLink =
          new LinkElement()
              .setId(baseId + "-external-link")
              .setStyles("block", "my-2", "text-sm", "underline")
              .setText(messages.at(MessageKey.EXTERNAL_LINK.getKeyName()))
              .setHref(program.externalLink())
              .opensInNewTab()
              .asAnchorText()
              .with(
                  Icons.svg(Icons.OPEN_IN_NEW, 24, 24)
                      .withClasses(
                          "flex-shrink-0",
                          "h-5",
                          "w-auto",
                          "inline",
                          "ml-1",
                          "align-text-top"));

      programData.with(externalLink);
    }

    String applyUrl =
        controllers.applicant.routes.ApplicantProgramReviewController.preview(
                applicantId, program.id())
            .url();
    ATag applyButton =
        a().withHref(applyUrl)
            .attr(
                "aria-label",
                messages.at(
                    MessageKey.BUTTON_APPLY_SR.getKeyName(),
                    program.localizedName().getOrDefault(preferredLocale)))
            .withText(
                isDraft
                    ? messages.at(MessageKey.BUTTON_CONTINUE.getKeyName())
                    : messages.at(MessageKey.BUTTON_APPLY.getKeyName()))
            .withId(baseId + "-apply")
            .withClasses(ReferenceClasses.APPLY_BUTTON, ApplicantStyles.BUTTON_PROGRAM_APPLY);

    DivTag applyDiv =
        div(applyButton)
            .withClasses(
                "w-full", "mb-6", "flex-grow", "flex", "items-end");
    String srProgramCardTitle =
        messages.at(
            MessageKey.TITLE_PROGRAM_CARD.getKeyName(),
            programIndex + 1,
            totalProgramCount,
            program.localizedName().getOrDefault(preferredLocale));
    return div()
        .withId(baseId)
        .withClasses(ReferenceClasses.APPLICATION_CARD, ApplicantStyles.PROGRAM_CARD)
        .with(h4(srProgramCardTitle).withClass("sr-only"))
        .with(
            // The visual bar at the top of each program card.
            div()
                .withClasses(
                    "block",
                    "flex-shrink-0",
                    BaseStyles.BG_SEATTLE_BLUE,
                    "rounded-t-xl",
                    "h-3"))
        .with(programData)
        .with(applyDiv);
  }
}
