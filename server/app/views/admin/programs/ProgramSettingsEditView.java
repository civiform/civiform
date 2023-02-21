package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.TagCreator.p;

import com.google.inject.Inject;
import featureflags.FeatureFlags;
import j2html.TagCreator;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import java.util.Optional;
import play.mvc.Http.HttpVerbs;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.ToastMessage;
import views.style.StyleUtils;

/** Renders a page for adding a new program. */
public final class ProgramSettingsEditView extends ProgramSettingsFormBuilder {
  private final AdminLayout layout;

  @Inject
  public ProgramSettingsEditView(AdminLayoutFactory layoutFactory, FeatureFlags featureFlags) {
    super(featureFlags);
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
  }

  public Content render(Request request, ProgramDefinition program) {
    return render(request, program, /* message= */ Optional.empty());
  }

  public Content render(
      Request request, ProgramDefinition program, Optional<ToastMessage> message) {
    String title = program.adminName() + " settings";

    ButtonTag eligibilityIsGatingButton =
        TagCreator.button()
            .withClasses(
                "flex",
                "gap-2",
                "items-center",
                // isOptional ? "text-black" : "text-gray-400",
                "text-black",
                "font-medium",
                "bg-transparent",
                "rounded-full",
                StyleUtils.hover("bg-gray-400", "text-gray-300"))
            .withType("submit")
            .with(p("Non-gated eligibility").withClasses("hover-group:text-white"))
            .with(
                div()
                    .withClasses("relative")
                    .with(
                        div()
                            .withClasses(
                                // isOptional ? "bg-blue-600" : "bg-gray-600",
                                "bg-blue-600", "w-14", "h-8", "rounded-full"))
                    .with(
                        div()
                            .withClasses(
                                "absolute",
                                "bg-white",
                                // isOptional ? "right-1" : "left-1",
                                "right-1",
                                "top-1",
                                "w-6",
                                "h-6",
                                "rounded-full")));
    /*
    String toggleOptionalAction =
        controllers.admin.routes.AdminProgramBlockQuestionsController.setOptional(
                programDefinitionId, blockDefinitionId, questionDefinition.getId())
            .url();
                */
    FormTag formTag =
        form(makeCsrfTokenInputTag(request))
            .withMethod(HttpVerbs.POST)
            // .withAction(toggleOptionalAction)
            .with(
                input()
                    .isHidden()
                    .withName("optional")
                    .withValue(
                        // isOptional ? "false" : "true"))
                        "false"))
            .with(eligibilityIsGatingButton);

    DivTag contentDiv = div(formTag);
    // .withAction(controllers.admin.routes.AdminProgramController.().url()));

    HtmlBundle htmlBundle =
        layout.getBundle().setTitle(title).addMainContent(renderHeader(title), contentDiv);

    message.ifPresent(htmlBundle::addToastMessages);

    return layout.renderCentered(htmlBundle);
  }
}
