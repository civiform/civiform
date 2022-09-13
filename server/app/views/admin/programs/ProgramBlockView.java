package views.admin.programs;

import static j2html.TagCreator.div;

import j2html.tags.specialized.DivTag;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.ViewUtils;
import views.ViewUtils.BadgeStatus;
import views.style.Styles;

abstract class ProgramBlockView extends BaseHtmlView {
  /** Renders a div with internal/admin program information. */
  protected final DivTag renderProgramInfo(ProgramDefinition programDefinition) {
    DivTag programTitle =
        div(programDefinition.adminName())
            .withId("program-title")
            .withClasses(Styles.TEXT_3XL, Styles.PB_3, Styles.MT_5);
    DivTag programDescription =
        div(programDefinition.adminDescription()).withClasses(Styles.TEXT_SM);

    return div(ViewUtils.makeBadge(BadgeStatus.DRAFT), programTitle, programDescription)
        .withClasses(
            Styles.BG_GRAY_100,
            Styles.TEXT_GRAY_800,
            Styles.SHADOW_MD,
            Styles.P_8,
            Styles.PT_4,
            Styles._MX_2);
  }
}
