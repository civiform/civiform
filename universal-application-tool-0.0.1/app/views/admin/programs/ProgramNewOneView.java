package views.admin.programs;

import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.form;

import com.google.inject.Inject;
import forms.ProgramForm;
import j2html.tags.ContainerTag;
import play.mvc.Http.Request;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;
import views.components.ToastMessage;

public final class ProgramNewOneView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public ProgramNewOneView(AdminLayout layout) {
    this.layout = layout;
  }

  public Content render(Request request) {
    return layout.render(
        body(
            renderHeader("New program"),
            div(
                buildProgramForm(new ProgramForm())
                    .with(makeCsrfTokenInputTag(request))
                    .withAction(controllers.admin.routes.AdminProgramController.create().url()))));
  }

  public Content render(Request request, ProgramForm programForm, String message) {
    ContainerTag bodyContent =
        body(
            renderHeader("New program"),
            div(
                buildProgramForm(programForm)
                    .with(makeCsrfTokenInputTag(request))
                    .withAction(controllers.admin.routes.AdminProgramController.create().url())));
    if (message.length() > 0) {
      bodyContent.with(ToastMessage.error(message).setDismissible(false).getContainerTag());
    }
    return layout.render(bodyContent);
  }

  private ContainerTag buildProgramForm(ProgramForm programForm) {
    ContainerTag formTag = form().withMethod("POST");
    formTag.with(
        FieldWithLabel.input()
            .setId("program-name-input")
            .setFieldName("name")
            .setLabelText("Program name")
            .setPlaceholderText("The name of the program")
            .setValue(programForm.getName())
            .getContainer(),
        FieldWithLabel.textArea()
            .setId("program-description-textarea")
            .setFieldName("description")
            .setLabelText("Program description")
            .setPlaceholderText("The description of the program")
            .setValue(programForm.getDescription())
            .getContainer(),
        submitButton("Create").withId("program-create-button"));
    return formTag;
  }
}
