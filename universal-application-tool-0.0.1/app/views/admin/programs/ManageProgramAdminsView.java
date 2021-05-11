package views.admin.programs;

import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.Optional;
import javax.inject.Inject;
import play.mvc.Http;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;
import views.style.ReferenceClasses;
import views.style.Styles;

/**
 * Renders a form for adding and removing program admins via email for a given program.
 */
public class ManageProgramAdminsView extends BaseHtmlView {

  private final AdminLayout layout;

  @Inject
  public ManageProgramAdminsView(AdminLayout layout) {
    this.layout = layout;
  }

  public Content render(
      Http.Request request, ProgramDefinition program, ImmutableList<String> existingAdminEmails) {
    // Display a form with a list of inputs for adding and removing
    return layout.render(
        body(
            renderHeader("Manage Admins for Program: " + program.adminName()),
            adminEmailTemplate(),
            renderAdminForm(request, program.id(), existingAdminEmails)));
  }

  /** Render a form with inputs for program admin emails. If program admins exist, the input fields will be pre-populated with their email addresses. */
  private ContainerTag renderAdminForm(
      Http.Request request, long programId, ImmutableList<String> existingAdminEmails) {
    ContainerTag emailFields =
        div()
            .withId("program-admin-emails")
            .with(each(existingAdminEmails, email -> adminEmailInput(Optional.of(email))))
            .with(button("Add admin").withId("add-program-admin-button"));

    return form()
        .with(makeCsrfTokenInputTag(request))
        .withAction(routes.ProgramAdminManagementController.update(programId).url())
        .withMethod("POST")
        .with(emailFields)
        .with(submitButton("Save"));
  }

  private ContainerTag adminEmailInput(Optional<String> existing) {
    ContainerTag input =
        FieldWithLabel.email()
            .setFieldName("adminEmails[]")
            .setPlaceholderText("New admin email")
            .setValue(existing)
            .getContainer();
    Tag removeAdminButton = button("Remove").withClasses(ReferenceClasses.PROGRAM_ADMIN_REMOVE_BUTTON);

    return div().with(input, removeAdminButton);
  }

  /** A hidden template for adding and removing admins of a given program. */
  private ContainerTag adminEmailTemplate() {
    return adminEmailInput(Optional.empty())
        .withId("program-admin-email-template")
        .withClasses(Styles.HIDDEN);
  }
}
