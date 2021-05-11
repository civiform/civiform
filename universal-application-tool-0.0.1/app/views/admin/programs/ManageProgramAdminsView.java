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
import views.style.StyleUtils;
import views.style.Styles;

/** Renders a form for adding and removing program admins via email for a given program. */
public class ManageProgramAdminsView extends BaseHtmlView {

  private static final String EMAIL_FIELD_STYLES =
      StyleUtils.joinStyles(Styles.FLEX, Styles.FLEX_ROW);
  private static final String PAGE_TITLE = "Manage Admins for Program: ";
  private static final String ADD_ADMIN_BUTTON = "Add admin";
  private static final String SUBMIT_BUTTON = "Save";
  private static final String INPUT_PLACEHOLDER = "New admin email";
  private static final String REMOVE_BUTTON = "Remove";
  private static final String FORM_ID = "manage-program-admin-form";
  private static final String EMAIL_CONTAINER_DIV_ID = "program-admin-emails";
  private static final String ADD_BUTTON_ID = "add-program-admin-button";
  private static final String ADD_EMAIL_FIELD_NAME = "adminEmails[]";
  private static final String REMOVE_EMAIL_FIELD_NAME = "removeAdminEmails[]";
  private static final String EMAIL_INPUT_TEMPLATE_ID = "program-admin-email-template";

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
            renderHeader(PAGE_TITLE + program.adminName()),
            adminEmailTemplate(),
            renderAdminForm(request, program.id(), existingAdminEmails)));
  }

  /**
   * Render a form with inputs for program admin emails. If program admins exist, the input fields
   * will be pre-populated with their email addresses.
   */
  private ContainerTag renderAdminForm(
      Http.Request request, long programId, ImmutableList<String> existingAdminEmails) {
    ContainerTag emailFields =
        div()
            .withId(EMAIL_CONTAINER_DIV_ID)
            .withClasses(Styles.ML_4)
            .with(each(existingAdminEmails, email -> adminEmailInput(Optional.of(email))))
            .with(button(ADD_ADMIN_BUTTON).withId(ADD_BUTTON_ID).withClasses(Styles.MY_2));

    return form()
        .withId(FORM_ID)
        .with(makeCsrfTokenInputTag(request))
        .withAction(routes.ProgramAdminManagementController.update(programId).url())
        .withMethod("POST")
        .with(emailFields)
        .with(submitButton(SUBMIT_BUTTON).withClasses(Styles.MY_4));
  }

  private ContainerTag adminEmailInput(Optional<String> existing) {
    String inputFieldName = existing.isPresent() ? REMOVE_EMAIL_FIELD_NAME : ADD_EMAIL_FIELD_NAME;

    ContainerTag input =
        FieldWithLabel.email()
            .setFormId(FORM_ID)
            .setFieldName(inputFieldName)
            .setPlaceholderText(INPUT_PLACEHOLDER)
            .setValue(existing)
            // If there is an existing value, do not allow changes in the input field.
            .setDisabled(existing.isPresent())
            .getContainer()
            .withClasses(Styles.FLEX, Styles.M_2);

    Tag removeAdminButton =
        button(REMOVE_BUTTON)
            .withClasses(ReferenceClasses.PROGRAM_ADMIN_REMOVE_BUTTON, Styles.FLEX, Styles.M_2);

    return div().with(input, removeAdminButton).withClasses(EMAIL_FIELD_STYLES);
  }

  /** A hidden template for adding and removing admins of a given program. */
  private ContainerTag adminEmailTemplate() {
    return adminEmailInput(Optional.empty())
        .withId(EMAIL_INPUT_TEMPLATE_ID)
        .withClasses(Styles.HIDDEN, EMAIL_FIELD_STYLES);
  }
}
