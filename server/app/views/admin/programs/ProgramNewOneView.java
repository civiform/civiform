package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;

import com.google.inject.Inject;
import com.typesafe.config.Config;
import featureflags.FeatureFlags;
import forms.ProgramForm;
import j2html.tags.specialized.DivTag;
import java.util.Optional;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.ToastMessage;

/** Renders a page for adding a new program. */
public final class ProgramNewOneView extends ProgramFormBuilder {
  private final AdminLayout layout;

  @Inject
  public ProgramNewOneView(
      AdminLayoutFactory layoutFactory, Config configuration, FeatureFlags featureFlags) {
    super(configuration, featureFlags);
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
  }

  public Content render(Request request) {
    return render(request, new ProgramForm(), /* message= */ Optional.empty());
  }

  public Content render(Request request, ProgramForm programForm, Optional<ToastMessage> message) {
    String title = "New program information";

    DivTag contentDiv =
        div(
            buildProgramForm(request, programForm, /* editExistingProgram = */ false)
                .with(makeCsrfTokenInputTag(request))
                .withAction(controllers.admin.routes.AdminProgramController.create().url()));

    HtmlBundle htmlBundle =
        layout.getBundle().setTitle(title).addMainContent(renderHeader(title), contentDiv);

    message.ifPresent(htmlBundle::addToastMessages);

    return layout.renderCentered(htmlBundle);
  }
}
