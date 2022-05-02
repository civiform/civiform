package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.p;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.typesafe.config.Config;

import auth.CiviFormProfile;
import controllers.admin.routes;
import java.time.Clock;
import j2html.tags.Tag;
import play.twirl.api.Content;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.components.LinkElement;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/** Renders a page for program admins to view programs they administer. */
public class ProgramAdministratorProgramListView extends BaseHtmlView {

  private final AdminLayout layout;
  private final String baseUrl;
  private final ZoneId zoneId;

  @Inject
  public ProgramAdministratorProgramListView(AdminLayout layout, Config config, Clock clock) {
    this.layout = checkNotNull(layout);
    this.baseUrl = checkNotNull(config).getString("base_url");
    this.zoneId = checkNotNull(clock).getZone();
  }

  public Content render(
      ActiveAndDraftPrograms programs,
      List<String> authorizedPrograms,
      Optional<CiviFormProfile> civiformProfile) {
    if (civiformProfile.isPresent()
        && civiformProfile.get().isProgramAdmin()
        && !civiformProfile.get().isCiviFormAdmin()) {
      layout.setOnlyProgramAdminType();
    }

    String title = "Your programs";
    Tag contentDiv =
        div()
            .withClasses(Styles.PX_20)
            .with(
                h1(title).withClasses(Styles.MY_4),
                each(
                    programs.getProgramNames().stream()
                        .filter(programName -> authorizedPrograms.contains(programName))
                        .map(
                            name ->
                                this.renderProgramListItem(
                                    programs.getActiveProgramDefinition(name),
                                    programs.getDraftProgramDefinition(name)))));

    HtmlBundle htmlBundle = layout.getBundle().setTitle(title).addMainContent(contentDiv);

    return layout.renderCentered(htmlBundle);
  }

  public ProgramDefinition getDisplayProgram(
      Optional<ProgramDefinition> draftProgram, Optional<ProgramDefinition> activeProgram) {
    if (draftProgram.isPresent()) {
      return draftProgram.get();
    }
    return activeProgram.get();
  }

  public Tag renderProgramListItem(
      Optional<ProgramDefinition> activeProgram, Optional<ProgramDefinition> draftProgram) {
    String programStatusText = extractProgramStatusText(draftProgram, activeProgram);
    String viewApplicationsLinkText = "Applications →";

    ProgramDefinition displayProgram = getDisplayProgram(draftProgram, activeProgram);

    String lastEditText =
        displayProgram.lastModifiedTime().isPresent()
            ? "Last updated: " + renderDateTime(displayProgram.lastModifiedTime().get(), zoneId)
            : "Could not find latest update time";
    String programTitleText = displayProgram.adminName();
    String programDescriptionText = displayProgram.adminDescription();
    String blockCountText = "Screens: " + displayProgram.getBlockCount();
    String questionCountText = "Questions: " + displayProgram.getQuestionCount();

    Tag topContent =
        div(
                div(
                    p(programStatusText).withClasses(Styles.TEXT_SM, Styles.TEXT_GRAY_700),
                    div(programTitleText)
                        .withClasses(
                            Styles.TEXT_BLACK, Styles.FONT_BOLD, Styles.TEXT_XL, Styles.MB_2)),
                p().withClasses(Styles.FLEX_GROW),
                div(p(blockCountText), p(questionCountText))
                    .withClasses(
                        Styles.TEXT_RIGHT,
                        Styles.TEXT_XS,
                        Styles.TEXT_GRAY_700,
                        Styles.MR_2,
                        StyleUtils.applyUtilityClass(StyleUtils.RESPONSIVE_MD, Styles.MR_4)))
            .withClasses(Styles.FLEX);

    Tag midContent =
        div(programDescriptionText)
            .withClasses(Styles.TEXT_GRAY_700, Styles.TEXT_BASE, Styles.MB_8, Styles.LINE_CLAMP_3);

    Tag bottomContent =
        div(
                p(lastEditText).withClasses(Styles.TEXT_GRAY_700, Styles.ITALIC),
                p().withClasses(Styles.FLEX_GROW),
                maybeRenderViewApplicationsLink(viewApplicationsLinkText, activeProgram))
            .withClasses(Styles.FLEX, Styles.TEXT_SM, Styles.W_FULL);

    Tag programDeepLink =
        label("Deep link, use this URL to link to this program from outside of CiviForm:")
            .withClasses(Styles.W_FULL)
            .with(
                input()
                    .withValue(
                        baseUrl
                            + controllers.applicant.routes.RedirectController.programByName(
                                    displayProgram.slug())
                                .url())
                    .attr("disabled", "readonly")
                    .withClasses(Styles.W_FULL, Styles.MB_2)
                    .withType("text"));

    Tag innerDiv =
        div(topContent, midContent, programDeepLink, bottomContent)
            .withClasses(
                Styles.BORDER, Styles.BORDER_GRAY_300, Styles.BG_WHITE, Styles.ROUNDED, Styles.P_4);

    return div(innerDiv)
        .withClasses(
            ReferenceClasses.ADMIN_PROGRAM_CARD, Styles.W_FULL, Styles.SHADOW_LG, Styles.MB_4);
  }

  private String extractProgramStatusText(
      Optional<ProgramDefinition> draftProgram, Optional<ProgramDefinition> activeProgram) {
    if (draftProgram.isPresent() && activeProgram.isPresent()) {
      return "Active, with draft";
    } else if (draftProgram.isPresent()) {
      return "Draft";
    } else if (activeProgram.isPresent()) {
      return "Active";
    }
    throw new IllegalArgumentException("Program neither active nor draft.");
  }

  Tag maybeRenderViewApplicationsLink(String text, Optional<ProgramDefinition> activeProgram) {
    if (activeProgram.isPresent()) {
      String viewApplicationsLink =
          routes.AdminApplicationController.index(
                  activeProgram.get().id(), Optional.empty(), Optional.empty())
              .url();

      return new LinkElement()
          .setId("program-view-apps-link-" + activeProgram.get().id())
          .setHref(viewApplicationsLink)
          .setText(text)
          .setStyles(Styles.MR_2)
          .asAnchorText();
    } else {
      return div();
    }
  }
}
