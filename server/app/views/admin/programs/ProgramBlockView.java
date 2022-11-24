package views.admin.programs;

import static j2html.TagCreator.div;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableList;
import forms.BlockForm;
import j2html.tags.Tag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import java.util.ArrayList;
import java.util.Arrays;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.question.types.QuestionDefinition;
import views.BaseHtmlView;
import views.ViewUtils;
import views.ViewUtils.BadgeStatus;
import views.components.Icons;
import views.style.AdminStyles;
import views.style.StyleUtils;

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
    asRedirectElement(
        editDetailsButton,
        getNavigationUrl(programDefinition));

    return div(
            ViewUtils.makeBadge(getBadgeStatus()),
            title,
            description,
            adminNote,
            editDetailsButton)
        .withClasses("bg-gray-100", "text-gray-800", "shadow-md", "p-8", "pt-4", "-mx-2");
  }

  /** Define the String that will be shown on the Edit button **/
  protected abstract String getEditButtonText();

  /** Define the navigation destination for the Edit button **/
  protected abstract String getNavigationUrl(ProgramDefinition programDefinition);

  protected abstract BadgeStatus getBadgeStatus();
}
