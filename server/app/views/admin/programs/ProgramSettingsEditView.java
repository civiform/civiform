package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.input;
import static j2html.TagCreator.p;

import com.google.inject.Inject;
import j2html.TagCreator;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import play.mvc.Http.HttpVerbs;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.style.StyleUtils;

/** Renders a page for editing program-level settings. */
public final class ProgramSettingsEditView extends BaseHtmlView {
  private static final String ELIGIBILITY_TOGGLE_ID = "eligibility-toggle";
  private static final String ELIGIBILITY_IS_GATING_DESCRIPTION =
      "When using eligibility conditions, you can set the experience to gating or non-gating. The"
          + " default is a gated experience that won’t allow an applicant to submit an application"
          + " if they do not meet eligibility criteria. Whereas a non-gated experience can be"
          + " turned on to allow application/form submission regardless of whether or not they’ve"
          + " met eligibility.";

  private final AdminLayout layout;

  @Inject
  public ProgramSettingsEditView(AdminLayoutFactory layoutFactory) {
    super();
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
  }

  public Content render(Request request, ProgramDefinition program) {
    boolean eligibilityIsGating = program.eligibilityIsGating();

    ButtonTag eligibilityIsGatingToggle =
        TagCreator.button()
            .withId(ELIGIBILITY_TOGGLE_ID)
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
            .with(p("Non-gating e;ligibility").withClasses("hover-group:text-white", "ml-1"));
    String toggleAction =
        controllers.admin.routes.AdminProgramController.setEligibilityIsGating(program.id()).url();
    FormTag formTag =
        form(makeCsrfTokenInputTag(request))
            .withMethod(HttpVerbs.POST)
            .withAction(toggleAction)
            .with(
                input()
                    .isHidden()
                    .withName("eligibilityIsGating")
                    .withValue(eligibilityIsGating ? "false" : "true"))
            .with(eligibilityIsGatingToggle)
            .with(
                p(ELIGIBILITY_IS_GATING_DESCRIPTION)
                    .withClasses("text-md", "max-w-prose", "mt-6", "text-gray-700"));

    String title = program.localizedName().getDefault() + " settings";
    DivTag contentDiv =
        div()
            .withClasses("px-12")
            .with(div().withClasses("mt-12").with(h1(title)))
            .with(div(formTag));

    return layout.renderCentered(layout.getBundle().setTitle(title).addMainContent(contentDiv));
  }
}
