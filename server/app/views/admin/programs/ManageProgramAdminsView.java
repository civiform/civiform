package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import java.util.Optional;
import javax.inject.Inject;
import play.mvc.Http;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.ViewUtils;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.ButtonStyles;
import views.components.FieldWithLabel;
import views.components.Icons;
import views.components.ToastMessage;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Renders a form for adding and removing program admins via email for a given program. */
public class ManageProgramAdminsView extends BaseHtmlView {

  private static final String EMAIL_FIELD_STYLES = StyleUtils.joinStyles("flex", "flex-row");
  private static final String PAGE_TITLE = "Manage Admins for Program: ";
  private static final String EMAIL_CONTAINER_DIV_ID = "program-admin-emails";
  private static final String ADD_BUTTON_ID = "add-program-admin-button";
  private static final String ADD_EMAIL_FIELD_NAME = "adminEmails[]";
  private static final String REMOVE_EMAIL_FIELD_NAME = "removeAdminEmails[]";
  private static final String EMAIL_INPUT_TEMPLATE_ID = "program-admin-email-template";

  private final AdminLayout layout;

  @Inject
  public ManageProgramAdminsView(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
  }

  /**
   * Display a form with a list of inputs for adding and removing admins. Adds a toast error message
   * if the message parameter is provided.
   */
  public Content render(
      Http.Request request,
      ProgramDefinition program,
      ImmutableList<String> existingAdminEmails,
      Optional<ToastMessage> message) {

    String fullTitle = PAGE_TITLE + program.adminName();

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(fullTitle)
            .addMainContent(
                renderHeader(fullTitle),
                adminEmailTemplate(),
                renderAdminForm(request, program.id(), existingAdminEmails));

    message.map(m -> m.setDuration(6000)).ifPresent(htmlBundle::addToastMessages);

    return layout.renderCentered(htmlBundle);
  }

  /**
   * Render a form with inputs for program admin emails. If program admins exist, the input fields
   * will be pre-populated with their email addresses.
   */
  private FormTag renderAdminForm(
      Http.Request request, long programId, ImmutableList<String> existingAdminEmails) {
    DivTag emailFields =
        div()
            .withId(EMAIL_CONTAINER_DIV_ID)
            .withClasses("ml-4")
            .with(each(existingAdminEmails, email -> adminEmailInput(Optional.of(email))))
            .with(
                ViewUtils.makeSvgTextButton("Add admin", Icons.PLUS)
                    .withType("button")
                    .withId(ADD_BUTTON_ID)
                    .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON, "my-2"));

    return form()
        .with(makeCsrfTokenInputTag(request))
        .withAction(routes.ProgramAdminManagementController.update(programId).url())
        .withMethod("POST")
        .with(emailFields)
        .with(submitButton("Save").withClasses(ButtonStyles.SOLID_BLUE, "my-4"));
  }

  private DivTag adminEmailInput(Optional<String> existing) {
    // When there are existing admins, the only option is to remove that admin. The field is
    // disabled, so that no changes except removal can be made. The form does not submit disabled
    // fields, so these existing admins will not be removed unless the remove button is clicked,
    // which sets disabled to false (see TypeScript file).
    String inputFieldName = existing.isPresent() ? REMOVE_EMAIL_FIELD_NAME : ADD_EMAIL_FIELD_NAME;

    DivTag input =
        FieldWithLabel.email()
            .setFieldName(inputFieldName)
            .setScreenReaderText("New admin email")
            .setValue(existing)
            // If there is an existing value, do not allow changes in the input field.
            .setDisabled(existing.isPresent())
            .getEmailTag()
            .withClasses("flex", "m-2");

    ButtonTag removeAdminButton =
        ViewUtils.makeSvgTextButton("Delete", Icons.DELETE)
            .withType("button")
            .withClasses(
                ReferenceClasses.PROGRAM_ADMIN_REMOVE_BUTTON,
                ButtonStyles.OUTLINED_WHITE_WITH_ICON,
                "flex",
                "m-2");

    return div().with(input, removeAdminButton).withClasses(EMAIL_FIELD_STYLES);
  }

  /** A hidden template for adding and removing admins of a given program. */
  private DivTag adminEmailTemplate() {
    return adminEmailInput(Optional.empty())
        .withId(EMAIL_INPUT_TEMPLATE_ID)
        .withClasses("hidden", EMAIL_FIELD_STYLES);
  }
}
