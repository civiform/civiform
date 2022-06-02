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
import static j2html.attributes.Attr.HREF;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.Tag;
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

  private ContainerTag topContent(String titleText, String infoTextLine1, String infoTextLine2) {
    // "Get benefits"
    ContainerTag programIndexH1 =
        h1().withText(titleText)
            .withClasses(
                Styles.TEXT_4XL,
                StyleUtils.responsiveSmall(Styles.TEXT_5XL),
                Styles.FONT_SEMIBOLD,
                Styles.MB_2,
                Styles.PX_6,
                StyleUtils.responsiveSmall(Styles.MB_6));

    ContainerTag infoLine1Div =
        div()
            .withText(infoTextLine1)
            .withClasses(Styles.TEXT_SM, Styles.PX_6, StyleUtils.responsiveSmall(Styles.TEXT_BASE));

    ContainerTag infoLine2Div =
        div()
            .withText(infoTextLine2)
            .withClasses(
                Styles.TEXT_SM,
                Styles.PX_6,
                Styles.PB_6,
                StyleUtils.responsiveSmall(Styles.TEXT_BASE));

    Tag logoImg =
        maybeLogoUrl.isPresent()
            ? img().withSrc(maybeLogoUrl.get())
            : this.layout.viewUtils.makeLocalImageTag("Seattle-logo_horizontal_blue-white_small");

    ContainerTag logoDiv =
        div()
            .with(
                logoImg
                    .withAlt(civicEntityFullName + " logo")
                    .attr("aria-hidden", "true")
                    .attr("width", 175)
                    .attr("height", 70))
            .withClasses(Styles.TOP_2, Styles.LEFT_2);

    return div()
        .withId("top-content")
        .withClasses(ApplicantStyles.PROGRAM_INDEX_TOP_CONTENT, Styles.RELATIVE)
        .with(logoDiv, programIndexH1, infoLine1Div, infoLine2Div);
  }

  private ContainerTag mainContent(
      Messages messages,
      ImmutableList<ProgramDefinition> draftPrograms,
      ImmutableList<ProgramDefinition> activePrograms,
      long applicantId,
      Locale preferredLocale) {
    ContainerTag content =
        div()
            .withId("main-content")
            .withClasses(Styles.MX_AUTO, Styles.MY_4, StyleUtils.responsiveSmall(Styles.M_10))
            .with(
                h2().withText(messages.at(MessageKey.TITLE_PROGRAMS.getKeyName()))
                    .withClasses(Styles.MB_4, Styles.TEXT_XL, Styles.FONT_SEMIBOLD));

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
      content.with(hr().withClass(Styles.MY_16));
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

    return div().withClasses(Styles.FLEX, Styles.FLEX_COL, Styles.PLACE_ITEMS_CENTER).with(content);
  }

  /**
   * This method generates a list of style classes with responsive column counts. The number of
   * columns should not exceed the number of programs, or the program card container will not be
   * centered.
   */
  private String programCardsContainerStyles(int numPrograms) {
    return StyleUtils.joinStyles(
        ApplicantStyles.PROGRAM_CARDS_CONTAINER_BASE,
        numPrograms >= 2 ? StyleUtils.responsiveMedium(Styles.GRID_COLS_2) : "",
        numPrograms >= 3 ? StyleUtils.responsiveLarge(Styles.GRID_COLS_3) : "",
        numPrograms >= 4 ? StyleUtils.responsiveXLarge(Styles.GRID_COLS_4) : "",
        numPrograms >= 5 ? StyleUtils.responsive2XLarge(Styles.GRID_COLS_5) : "");
  }

  private ContainerTag programCard(
      Messages messages,
      ProgramDefinition program,
      int programIndex,
      int totalProgramCount,
      Long applicantId,
      Locale preferredLocale,
      boolean isDraft) {
    String baseId = ReferenceClasses.APPLICATION_CARD + "-" + program.id();

    ContainerTag title =
        div()
            .withId(baseId + "-title")
            .withClasses(Styles.TEXT_LG, Styles.FONT_SEMIBOLD)
            .withText(program.localizedName().getOrDefault(preferredLocale));
    ImmutableList<DomContent> descriptionContent =
        TextFormatter.createLinksAndEscapeText(
            program.localizedDescription().getOrDefault(preferredLocale),
            TextFormatter.UrlOpenAction.NewTab);
    ContainerTag description =
        div()
            .withId(baseId + "-description")
            .withClasses(
                ReferenceClasses.APPLICATION_CARD_DESCRIPTION,
                Styles.TEXT_XS,
                Styles.MY_2,
                Styles.LINE_CLAMP_5)
            .with(descriptionContent);

    ContainerTag programData =
        div()
            .withId(baseId + "-data")
            .withClasses(Styles.W_FULL, Styles.PX_4, Styles.OVERFLOW_AUTO)
            .with(title, description);

    // Add info link.
    String infoUrl =
        controllers.applicant.routes.ApplicantProgramsController.view(applicantId, program.id())
            .url();
    ContainerTag infoLink =
        new LinkElement()
            .setId(baseId + "-info-link")
            .setStyles(Styles.BLOCK, Styles.MY_2, Styles.TEXT_XS, Styles.UNDERLINE)
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
      ContainerTag externalLink =
          new LinkElement()
              .setId(baseId + "-external-link")
              .setStyles(Styles.BLOCK, Styles.MY_2, Styles.TEXT_XS, Styles.UNDERLINE)
              .setText(messages.at(MessageKey.EXTERNAL_LINK.getKeyName()))
              .setHref(program.externalLink())
              .asAnchorText()
              .withTarget("_blank")
              .with(Icons.svg(Icons.OPEN_IN_NEW_PATH, 24, 24)
                  .withClasses(Styles.FLEX_SHRINK_0, Styles.H_5, Styles.W_AUTO, Styles.INLINE));;

      programData.with(externalLink);
    }

    String applyUrl =
        controllers.applicant.routes.ApplicantProgramReviewController.preview(
                applicantId, program.id())
            .url();
    ContainerTag applyButton =
        a().attr(HREF, applyUrl)
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

    ContainerTag applyDiv =
        div(applyButton)
            .withClasses(
                Styles.W_FULL, Styles.MB_6, Styles.FLEX_GROW, Styles.FLEX, Styles.ITEMS_END);
    String srProgramCardTitle =
        messages.at(
            MessageKey.TITLE_PROGRAM_CARD.getKeyName(),
            programIndex + 1,
            totalProgramCount,
            program.localizedName().getOrDefault(preferredLocale));
    return div()
        .withId(baseId)
        .withClasses(ReferenceClasses.APPLICATION_CARD, ApplicantStyles.PROGRAM_CARD)
        .with(h4(srProgramCardTitle).withClass(Styles.SR_ONLY))
        .with(
            // The visual bar at the top of each program card.
            div()
                .withClasses(
                    Styles.BLOCK,
                    Styles.FLEX_SHRINK_0,
                    BaseStyles.BG_SEATTLE_BLUE,
                    Styles.ROUNDED_T_XL,
                    Styles.H_3))
        .with(programData)
        .with(applyDiv);
  }
}
