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
import views.style.Styles;

abstract class ProgramBlockView extends BaseHtmlView {
  /** Renders a div with internal/admin program information. */
  protected final DivTag renderProgramInfo(ProgramDefinition programDefinition) {
    DivTag programTitle =
        div(programDefinition.adminName())
            .withId("program-title")
            .withClasses(Styles.TEXT_3XL, Styles.PB_3);
    DivTag programDescription =
        div(programDefinition.adminDescription()).withClasses(Styles.TEXT_SM);

    ButtonTag editDetailsButton =
        ViewUtils.makeSvgTextButton("Edit program details", Icons.EDIT)
            .withId("edit-program-details")
            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, Styles.MY_5);
    asRedirectElement(
        editDetailsButton,
        controllers.admin.routes.AdminProgramController.edit(programDefinition.id()).url());

    return div(
            ViewUtils.makeBadge(BadgeStatus.DRAFT),
            programTitle,
            programDescription,
            editDetailsButton)
        .withClasses(
            Styles.BG_GRAY_100,
            Styles.TEXT_GRAY_800,
            Styles.SHADOW_MD,
            Styles.P_8,
            Styles.PT_4,
            Styles._MX_2);
  }
}
