package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.label;
import static j2html.TagCreator.p;

import auth.CiviFormProfile;
import com.typesafe.config.Config;
import controllers.admin.routes;
import j2html.tags.Tag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.LabelTag;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import play.twirl.api.Content;
import services.DateConverter;
import services.program.ActiveAndDraftPrograms;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.LinkElement;
import views.style.ReferenceClasses;
import views.style.StyleUtils;


/** Renders a page for program admins to view programs they administer. */
public class ProgramAdministratorProgramListView extends BaseHtmlView {

  private final AdminLayout layout;
  private final String baseUrl;
  private final DateConverter dateConverter;

  @Inject
  public ProgramAdministratorProgramListView(
      AdminLayoutFactory layoutFactory, Config config, DateConverter dateConverter) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
    this.baseUrl = checkNotNull(config).getString("base_url");
    this.dateConverter = checkNotNull(dateConverter);
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

    // TODO(#1238): Create a "Program Card" UI component that encapsulates
    // the styling of the CiviForm Admin equivalent of this view and reuse
    // it here.
    String title = "Your programs";
    DivTag contentDiv =
        div()
            .withClasses("px-20")
            .with(
                h1(title).withClasses("my-4"),
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

  public DivTag renderProgramListItem(
      Optional<ProgramDefinition> activeProgram, Optional<ProgramDefinition> draftProgram) {
    String programStatusText = extractProgramStatusText(draftProgram, activeProgram);
    String viewApplicationsLinkText = "Applications →";

    ProgramDefinition displayProgram = getDisplayProgram(draftProgram, activeProgram);

    String lastEditText =
        displayProgram.lastModifiedTime().isPresent()
            ? "Last updated: "
                + dateConverter.renderDateTime(displayProgram.lastModifiedTime().get())
            : "Could not find latest update time";
    String programTitleText = displayProgram.adminName();
    String programDescriptionText = displayProgram.adminDescription();
    String blockCountText = "Screens: " + displayProgram.getBlockCount();
    String questionCountText = "Questions: " + displayProgram.getQuestionCount();

    DivTag topContent =
        div(
                div(
                    p(programStatusText).withClasses("text-sm", "text-gray-700"),
                    div(programTitleText)
                        .withClasses(
                            "text-black", "font-bold", "text-xl", "mb-2")),
                p().withClasses("flex-grow"),
                div(p(blockCountText), p(questionCountText))
                    .withClasses(
                        "text-right",
                        "text-xs",
                        "text-gray-700",
                        "mr-2",
                        StyleUtils.applyUtilityClass(StyleUtils.RESPONSIVE_MD, "mr-4")))
            .withClasses("flex");

    DivTag midContent =
        div(programDescriptionText)
            .withClasses("text-gray-700", "text-base", "mb-8", "line-clamp-3");

    DivTag bottomContent =
        div(
                p(lastEditText).withClasses("text-gray-700", "italic"),
                p().withClasses("flex-grow"),
                maybeRenderViewApplicationsLink(viewApplicationsLinkText, activeProgram))
            .withClasses("flex", "text-sm", "w-full");

    LabelTag programDeepLink =
        label("Deep link, use this URL to link to this program from outside of CiviForm:")
            .withClasses("w-full")
            .with(
                input()
                    .withValue(
                        baseUrl
                            + controllers.applicant.routes.RedirectController.programByName(
                                    displayProgram.slug())
                                .url())
                    .isDisabled()
                    .isReadonly()
                    .withClasses("w-full", "mb-2")
                    .withType("text"));

    DivTag innerDiv =
        div(topContent, midContent, programDeepLink, bottomContent)
            .withClasses(
                "border", "border-gray-300", "bg-white", "rounded", "p-4");

    return div(innerDiv)
        .withClasses(
            ReferenceClasses.ADMIN_PROGRAM_CARD, "w-full", "shadow-lg", "mb-4");
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

  Tag<?> maybeRenderViewApplicationsLink(String text, Optional<ProgramDefinition> activeProgram) {
    if (activeProgram.isPresent()) {
      String viewApplicationsLink =
          routes.AdminApplicationController.index(
                  activeProgram.get().id(),
                  Optional.empty(),
                  Optional.empty(),
                  Optional.empty(),
                  Optional.empty())
              .url();

      return new LinkElement()
          .setId("program-view-apps-link-" + activeProgram.get().id())
          .setHref(viewApplicationsLink)
          .setText(text)
          .setStyles("mr-2")
          .asAnchorText();
    } else {
      return div();
    }
  }
}
