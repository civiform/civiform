package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.p;

import com.google.inject.Inject;
import j2html.TagCreator;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import java.util.Optional;
import play.mvc.Http.HttpVerbs;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.Icons;
import views.components.LinkElement;
import views.components.LinkElement.IconPosition;
import views.style.StyleUtils;

/** Renders a page for editing program-level settings. */
public final class ProgramSettingsEditView extends BaseHtmlView {
  public static final String NAVIGATION_SOURCE_SESSION_KEY = "programSettingsnavigationSource";
  public static final String NAVIGATION_SOURCE_PROGRAM_INDEX_SESSION_VALUE = "programIndex";
  public static final String NAVIGATION_SOURCE_PROGRAM_BLOCKS_SESSION_VALUE = "programBlocks";

  private static final String ELIGIBILITY_TOGGLE_ID = "eligibility-toggle";
  private static final String ELIGIBILITY_IS_GATING_LABEL =
      "Eligibility criteria does not block submission";
  private static final String ELIGIBILITY_IS_GATING_DESCRIPTION =
      "When enabled, applicants can submit applications even if the eligibility criteria are not"
          + " met. When disabled, applications must meet all eligibility criteria in order to"
          + " submit an application.";

  private final AdminLayout layout;

  @Inject
  public ProgramSettingsEditView(AdminLayoutFactory layoutFactory) {
    super();
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
  }

  public Content render(Request request, ProgramDefinition program) {
    String formId = "program-settings-form";
    boolean eligibilityIsGating = program.eligibilityIsGating();
    String toggleAction =
        controllers.admin.routes.AdminProgramController.setEligibilityIsGating(program.id()).url();
    ButtonTag eligibilityIsGatingToggle =
        TagCreator.button()
            .withId(ELIGIBILITY_TOGGLE_ID)
            .attr("hx-post", toggleAction)
            .attr("hx-select-oob", String.format("#%s", formId))
            .withClasses(
                "flex",
                "p-0",
                "gap-2",
                "items-center",
                "text-black",
                "font-md",
                "text-lg",
                "bg-transparent",
                "rounded-full",
                "mt-12",
                StyleUtils.hover("bg-transparent", "text-gray-300"))
            .withType("submit")
            .with(
                div()
                    .withClasses("relative")
                    .with(
                        div()
                            .withClasses(
                                eligibilityIsGating ? "bg-gray-400" : "bg-blue-600",
                                "w-11",
                                "h-6",
                                "rounded-full"))
                    .with(
                        div()
                            .withClasses(
                                "absolute",
                                "bg-white",
                                eligibilityIsGating ? "left-1" : "right-1",
                                "right-1",
                                "top-1",
                                "w-4",
                                "h-4",
                                "rounded-full")))
            .with(p(ELIGIBILITY_IS_GATING_LABEL).withClasses("hover-group:text-white", "ml-1"));

    String title = program.localizedName().getDefault() + " settings";
    DivTag contentDiv =
        div()
            .withClasses("px-12")
            .with(getBackButton(request, program))
            .with(div().withClasses("mt-4").with(h1(title)))
            .with(
                form(makeCsrfTokenInputTag(request))
                    .withId(formId)
                    .with(
                        input()
                            .isHidden()
                            .withName("eligibilityIsGating")
                            .withValue(eligibilityIsGating ? "false" : "true"))
                    .with(eligibilityIsGatingToggle))
            .with(
                p(ELIGIBILITY_IS_GATING_DESCRIPTION)
                    .withClasses("text-md", "max-w-prose", "mt-6", "text-gray-700"));

    return layout.renderCentered(layout.getBundle().setTitle(title).addMainContent(contentDiv));
  }

  private ATag getBackButton(Request request, ProgramDefinition program) {
    String backTarget = request.header("referer").orElse(controllers.admin.routes.AdminProgramController.index().url());
    return new LinkElement()
        .setHref(backTarget)
        .setIcon(Icons.ARROW_LEFT, IconPosition.START)
        .setText("Back")
        .setStyles("mt-6")
        .asAnchorText();
  }
}
