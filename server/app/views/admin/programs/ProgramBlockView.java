package views.admin.programs;

import static j2html.TagCreator.div;
import static j2html.TagCreator.span;

import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.ViewUtils;
import views.ViewUtils.ProgramDisplayType;
import views.components.Icons;
import views.style.AdminStyles;

abstract class ProgramBlockView extends BaseHtmlView {
  /** Renders a div with internal/admin program information. */
  protected final DivTag renderProgramInfo(ProgramDefinition programDefinition) {
    DivTag title =
        div(programDefinition.localizedName().getDefault())
            .withId("program-title")
            .withClasses("text-3xl", "pb-3");
    DivTag description =
        div(programDefinition.localizedDescription().getDefault()).withClasses("text-sm");
    DivTag adminNote =
        div()
            .withClasses("text-sm")
            .with(span("Admin note: ").withClasses("font-semibold"))
            .with(span(programDefinition.adminDescription()));

    ButtonTag editDetailsButton =
        ViewUtils.makeSvgTextButton(getEditButtonText(), Icons.EDIT)
            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, "my-5");
    asRedirectElement(editDetailsButton, getEditButtonUrl(programDefinition));

    return div(
            ViewUtils.makeBadge(getProgramDisplayStatus()),
            title,
            description,
            adminNote,
            editDetailsButton)
        .withClasses("bg-gray-100", "text-gray-800", "shadow-md", "p-8", "pt-4", "-mx-2");
  }

  /** Returns the string that will be shown on the Edit button */
  protected abstract String getEditButtonText();

  /** Returns the navigation destination for the Edit button */
  protected abstract String getEditButtonUrl(ProgramDefinition programDefinition);

  /**
   * Returns the badge which represents the status of the program. It will be shown at the top of
   * the page.
   */
  protected abstract ProgramDisplayType getProgramDisplayStatus();
}
