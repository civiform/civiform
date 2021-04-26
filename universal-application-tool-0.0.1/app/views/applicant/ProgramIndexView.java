package views.applicant;

import static j2html.TagCreator.a;
import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.attributes.Attr.HREF;

import com.google.common.collect.ImmutableList;
import j2html.tags.ContainerTag;
import java.util.Locale;
import java.util.Optional;
import javax.inject.Inject;
import play.i18n.Messages;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.components.ToastMessage;
import views.style.ApplicantStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Returns a list of programs that an applicant can browse, with buttons for applying. */
public class ProgramIndexView extends BaseHtmlView {

  private final ApplicantLayout layout;

  @Inject
  public ProgramIndexView(ApplicantLayout layout) {
    this.layout = layout;
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
      long applicantId,
      ImmutableList<ProgramDefinition> programs,
      Optional<String> banner) {
    ContainerTag body =
        body().withClasses(Styles.RELATIVE, Styles.PX_8, ApplicantStyles.BODY_BACKGROUND);
    if (banner.isPresent()) {
      body.with(ToastMessage.alert(banner.get()).getContainerTag());
    }
    body.with(
        topContent(messages.at("content.benefits"), messages.at("content.description")),
        mainContent(
            programs, applicantId, messages.lang().toLocale(), messages.at("button.apply")));

    return layout.render(body);
  }

  private ContainerTag topContent(String titleText, String infoText) {
    ContainerTag floatTitle =
        div()
            .withId("float-title")
            .withText(titleText)
            .withClasses(
                Styles.RELATIVE, Styles.W_0, Styles.TEXT_6XL, Styles.FONT_SERIF, Styles.FONT_THIN);
    ContainerTag floatText =
        div()
            .withId("float-text")
            .withText(infoText)
            .withClasses(Styles.MY_4, Styles.TEXT_SM, Styles.W_FULL);

    return div()
        .withId("top-content")
        .withClasses(
            Styles.RELATIVE,
            Styles.W_FULL,
            Styles.MB_10,
            StyleUtils.responsiveMedium(Styles.GRID, Styles.GRID_COLS_2))
        .with(floatTitle, floatText);
  }

  private ContainerTag mainContent(
      ImmutableList<ProgramDefinition> programs,
      long applicantId,
      Locale preferredLocale,
      String applyText) {
    return div()
        .withId("main-content")
        .withClasses(Styles.RELATIVE, Styles.W_FULL, Styles.FLEX, Styles.FLEX_WRAP, Styles.PB_8)
        .with(
            each(
                programs,
                program -> programCard(program, applicantId, preferredLocale, applyText)));
  }

  private ContainerTag programCard(
      ProgramDefinition program, Long applicantId, Locale preferredLocale, String applyText) {
    String baseId = ReferenceClasses.APPLICATION_CARD + "-" + program.id();
    ContainerTag category =
        div()
            .withId(baseId + "-category")
            .withClasses(Styles.TEXT_XS, Styles.PB_2)
            .with(
                div()
                    .withClasses(
                        Styles.BG_TEAL_400,
                        Styles.H_3,
                        Styles.W_3,
                        Styles.ROUNDED_FULL,
                        Styles.INLINE_BLOCK,
                        Styles.ALIGN_MIDDLE),
                div("No Category")
                    .withClasses(
                        Styles.ML_2,
                        Styles.INLINE,
                        Styles.ALIGN_BOTTOM,
                        Styles.ALIGN_TEXT_BOTTOM,
                        Styles.LEADING_3));

    ContainerTag title =
        div()
            .withId(baseId + "-title")
            .withClasses(Styles.TEXT_LG, Styles.FONT_SEMIBOLD)
            .withText(program.getLocalizedNameOrDefault(preferredLocale));
    ContainerTag description =
        div()
            .withId(baseId + "-description")
            .withClasses(Styles.TEXT_XS, Styles.MY_2)
            .withText(program.getLocalizedDescriptionOrDefault(preferredLocale));

    ContainerTag externalLink =
        div()
            .withId(baseId + "-external-link")
            .withClasses(Styles.TEXT_XS, Styles.UNDERLINE)
            .withText("Program details");
    ContainerTag programData =
        div()
            .withId(baseId + "-data")
            .withClasses(Styles.PX_4)
            .with(category, title, description, externalLink);

    String applyUrl =
        controllers.applicant.routes.ApplicantProgramsController.edit(applicantId, program.id())
            .url();
    ContainerTag applyButton =
        a().attr(HREF, applyUrl)
            .withText(applyText)
            .withId(baseId + "-apply")
            .withClasses(
                "apply-button",
                Styles.BLOCK,
                Styles.UPPERCASE,
                Styles.ROUNDED_3XL,
                Styles.PY_2,
                Styles.PX_6,
                Styles.W_MIN,
                Styles.MX_AUTO,
                Styles.BG_GRAY_200,
                StyleUtils.hover(Styles.BG_GRAY_300));

    ContainerTag applyDiv =
        div(applyButton).withClasses(Styles.ABSOLUTE, Styles.BOTTOM_6, Styles.W_FULL);
    return div()
        .withId(baseId)
        .withClasses(
            ReferenceClasses.APPLICATION_CARD,
            Styles.RELATIVE,
            Styles.INLINE_BLOCK,
            Styles.MR_4,
            Styles.MB_4,
            Styles.W_64,
            Styles.H_72,
            Styles.BG_WHITE,
            Styles.ROUNDED_XL,
            Styles.SHADOW_SM)
        .with(
            div()
                .withClasses(
                    Styles.BG_TEAL_400,
                    Styles.BG_OPACITY_60,
                    Styles.H_3,
                    Styles.ROUNDED_T_XL,
                    Styles.MB_4))
        .with(programData)
        .with(applyDiv);
  }
}
