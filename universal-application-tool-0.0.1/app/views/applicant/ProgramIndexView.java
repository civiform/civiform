package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.a;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.attributes.Attr.HREF;

import com.google.common.collect.ImmutableList;
import controllers.applicant.routes;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Messages;
import play.mvc.Http;
import play.twirl.api.Content;
import services.MessageKey;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
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

  @Inject
  public ProgramIndexView(ApplicantLayout layout) {
    this.layout = checkNotNull(layout);
  }

  /**
   * For each program in the list, render the program information along with an "Apply" button that
   * redirects the user to that program's application.
   *
   * @param messages the localized {@link Messages} for the current applicant
   * @param applicantId the ID of the current applicant
   * @param programs an {@link ImmutableList} of {@link ProgramDefinition}s with the most recent
   *     published versions
   * @return HTML content for rendering the list of available programs
   */
  public Content render(
      Messages messages,
      Http.Request request,
      long applicantId,
      String userName,
      ImmutableList<ProgramDefinition> programs,
      Optional<String> banner) {
    HtmlBundle bundle = layout.getBundle();
    if (banner.isPresent()) {
      bundle.addToastMessages(ToastMessage.alert(banner.get()));
    }
    bundle.addMainContent(
        topContent(
            messages.at(MessageKey.CONTENT_GET_BENEFITS.getKeyName()),
            messages.at(MessageKey.CONTENT_CIVIFORM_DESCRIPTION_1.getKeyName()),
            messages.at(MessageKey.CONTENT_CIVIFORM_DESCRIPTION_2.getKeyName())),
        mainContent(messages, programs, applicantId, messages.lang().toLocale()));

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
                StyleUtils.responsiveSmall(Styles.MB_6));
    ContainerTag infoLine1Div =
        div()
            .withText(infoTextLine1)
            .withClasses(Styles.TEXT_SM, StyleUtils.responsiveSmall(Styles.TEXT_BASE));
    ContainerTag infoLine2Div =
        div()
            .withText(infoTextLine2)
            .withClasses(Styles.TEXT_SM, StyleUtils.responsiveSmall(Styles.TEXT_BASE));

    return div()
        .withId("top-content")
        .withClasses(ApplicantStyles.PROGRAM_INDEX_TOP_CONTENT)
        .with(programIndexH1, infoLine1Div, infoLine2Div);
  }

  private ContainerTag mainContent(
      Messages messages,
      ImmutableList<ProgramDefinition> programs,
      long applicantId,
      Locale preferredLocale) {
    return div()
        .withId("main-content")
        .withClasses(Styles.MX_6, Styles.MY_4, StyleUtils.responsiveSmall(Styles.M_10))
        .with(
            h2().withText(messages.at(MessageKey.TITLE_PROGRAMS.getKeyName()))
                .withClasses(Styles.BLOCK, Styles.MB_4, Styles.TEXT_LG, Styles.FONT_SEMIBOLD))
        .with(
            div()
                .withClasses(ApplicantStyles.PROGRAM_CARDS_CONTAINER)
                .with(
                    each(
                        programs,
                        program -> programCard(messages, program, applicantId, preferredLocale))));
  }

  private ContainerTag programCard(
      Messages messages, ProgramDefinition program, Long applicantId, Locale preferredLocale) {
    String baseId = ReferenceClasses.APPLICATION_CARD + "-" + program.id();

    ContainerTag title =
        div()
            .withId(baseId + "-title")
            .withClasses(Styles.TEXT_LG, Styles.FONT_SEMIBOLD)
            .withText(program.localizedName().getOrDefault(preferredLocale));
    ImmutableList<DomContent> descriptionContent =
        TextFormatter.createLinksAndEscapeText(program.localizedDescription().getOrDefault(preferredLocale));
    ContainerTag description =
        div()
            .withId(baseId + "-description")
            .withClasses(
                ReferenceClasses.APPLICATION_CARD_DESCRIPTION,
                Styles.TEXT_XS,
                Styles.MY_2,
                Styles.LINE_CLAMP_5)
            .with(descriptionContent);

    ContainerTag externalLink =
        new LinkElement()
            .setId(baseId + "-external-link")
            .setStyles(Styles.TEXT_XS, Styles.UNDERLINE)
            .setText(messages.at(MessageKey.LINK_PROGRAM_DETAILS.getKeyName()))
            .setHref(routes.RedirectController.programByName(program.slug()).url())
            .asAnchorText();
    ContainerTag programData =
        div()
            .withId(baseId + "-data")
            .withClasses(Styles.W_FULL, Styles.PX_4, Styles.OVERFLOW_AUTO)
            .with(title, description, externalLink);

    String applyUrl =
        controllers.applicant.routes.ApplicantProgramsController.view(applicantId, program.id())
            .url();
    ContainerTag applyButton =
        a().attr(HREF, applyUrl)
            .withText(messages.at(MessageKey.BUTTON_APPLY.getKeyName()))
            .withId(baseId + "-apply")
            .withClasses(ReferenceClasses.APPLY_BUTTON, ApplicantStyles.BUTTON_PROGRAM_APPLY);

    ContainerTag applyDiv =
        div(applyButton)
            .withClasses(
                Styles.W_FULL, Styles.MB_6, Styles.FLEX_GROW, Styles.FLEX, Styles.ITEMS_END);
    return div()
        .withId(baseId)
        .withClasses(ReferenceClasses.APPLICATION_CARD, ApplicantStyles.PROGRAM_CARD)
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
