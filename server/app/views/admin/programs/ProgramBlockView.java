package views.admin.programs;

import static j2html.TagCreator.div;

import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.ViewUtils;
import views.ViewUtils.BadgeStatus;
import views.components.Icons;
import views.style.AdminStyles;


abstract class ProgramBlockView extends BaseHtmlView {
  /** Renders a div with internal/admin program information. */
  protected final DivTag renderProgramInfo(ProgramDefinition programDefinition) {
    DivTag programTitle =
        div(programDefinition.adminName())
            .withId("program-title")
            .withClasses("text-3xl", "pb-3");
    DivTag programDescription =
        div(programDefinition.adminDescription()).withClasses("text-sm");

    ButtonTag editDetailsButton =
        ViewUtils.makeSvgTextButton("Edit program details", Icons.EDIT)
            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, "my-5");
    asRedirectElement(
        editDetailsButton,
        controllers.admin.routes.AdminProgramController.edit(programDefinition.id()).url());

    return div(
            ViewUtils.makeBadge(BadgeStatus.DRAFT),
            programTitle,
            programDescription,
            editDetailsButton)
        .withClasses(
            "bg-gray-100",
            "text-gray-800",
            "shadow-md",
            "p-8",
            "pt-4",
            "-mx-2");
  }
}
