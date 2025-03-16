package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.thead;
import static j2html.TagCreator.tr;

import com.google.common.collect.ImmutableList;
import controllers.admin.routes;
import j2html.tags.specialized.ATag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.TdTag;
import j2html.tags.specialized.TheadTag;
import j2html.tags.specialized.TrTag;
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
import views.components.LinkElement;
import views.components.LinkElement.IconPosition;
import views.components.ToastMessage;
import views.style.BaseStyles;
import views.style.StyleUtils;

/** Renders a form for adding and removing program admins via email for a given program. */
public class ManageProgramAdminsView extends BaseHtmlView {

  private static final String PAGE_TITLE = "Manage admins for program: ";
  private static final String EMAIL_FIELD_NAME = "adminEmail";

  private final AdminLayout layout;

  @Inject
  public ManageProgramAdminsView(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
  }

  /**
   * Display a page with existing program admin emails as well as a form for adding new admins. Adds
   * a toast error message if the message parameter is provided.
   */
  public Content render(
      Http.Request request,
      ProgramDefinition program,
      ImmutableList<String> existingAdminEmails,
      Optional<ToastMessage> message) {

    String fullTitle = PAGE_TITLE + program.adminName();

    HtmlBundle htmlBundle =
        layout
            .getBundle(request)
            .setTitle(fullTitle)
            .addMainContent(
                renderBackButton(),
                renderHeader(fullTitle),
                h2("Add new admin"),
                renderAddNewAdminForm(request, program.id()),
                h2("Existing admins"),
                renderExistingAdmins(request, program.id(), existingAdminEmails));

    message.map(m -> m.setDuration(12000)).ifPresent(htmlBundle::addToastMessages);

    return layout.renderCentered(htmlBundle);
  }

  private DivTag renderAddNewAdminForm(Http.Request request, long programId) {
    FormTag formTag =
        form()
            .withMethod("POST")
            .withAction(routes.ProgramAdminManagementController.add(programId).url());
    FieldWithLabel emailField =
        FieldWithLabel.email()
            .setId("admin-email-input")
            .setFieldName("adminEmail")
            .setLabelText("Admin email address (Email address is case-sensitive)");
    return div()
        .with(
            formTag.with(
                emailField.getInputTag(),
                makeCsrfTokenInputTag(request),
                submitButton("Add")
                    .withId("add-admin-button")
                    .withClasses(ButtonStyles.SOLID_BLUE, "ml-2")))
        .withClasses("border", "border-gray-300", "shadow-md", "w-1/2", "mt-2", "mb-6", "p-4");
  }

  private DivTag renderExistingAdmins(
      Http.Request request, long programId, ImmutableList<String> existingAdminEmails) {
    return div()
        .with(
            table()
                .withClasses("border", "border-gray-300", "shadow-md", "w-full", "mt-2")
                .with(renderExistingAdminsTableHeader())
                .with(
                    tbody(
                        each(
                            existingAdminEmails,
                            adminEmail ->
                                renderExistingAdminRow(request, programId, adminEmail)))));
  }

  private TheadTag renderExistingAdminsTableHeader() {
    return thead(
        tr().withClasses("border-b", "bg-gray-200", "text-left")
            .with(th("Admin email").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-2/3"))
            .with(
                th("Actions")
                    .withClasses(BaseStyles.TABLE_CELL_STYLES, "text-right", "pr-8", "w-1/3")));
  }

  private TrTag renderExistingAdminRow(Http.Request request, long programId, String adminEmail) {
    return tr().withClasses("border-b", "border-gray-300", StyleUtils.even("bg-gray-100"))
        .with(renderEmailCell(adminEmail))
        .with(renderDeleteCell(request, programId, adminEmail));
  }

  private TdTag renderEmailCell(String email) {
    return td().with(div(email).withClasses("font-semibold"))
        .withClasses(BaseStyles.TABLE_CELL_STYLES, "pr-12");
  }

  private TdTag renderDeleteCell(Http.Request request, long programId, String adminEmail) {
    return td().with(
            div()
                .withClasses("flex", "items-center", "justify-end", "gap-3", "pr-3")
                .with(renderDeleteButton(request, programId, adminEmail)));
  }

  private FormTag renderDeleteButton(Http.Request request, long programId, String adminEmail) {
    FieldWithLabel hiddenEmailField =
        FieldWithLabel.email()
            .setId("admin-email-to-delete")
            .setFieldName(EMAIL_FIELD_NAME)
            .setValue(adminEmail);
    return form()
        .withMethod("POST")
        .withAction(routes.ProgramAdminManagementController.delete(programId).url())
        .with(makeCsrfTokenInputTag(request))
        .with(hiddenEmailField.getInputTag().withClasses("hidden"))
        .with(
            ViewUtils.makeSvgTextButton("Delete", Icons.DELETE)
                .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON));
  }

  private ATag renderBackButton() {
    // The link to manage program admins is from the programs index page.
    return new LinkElement()
        .setHref(routes.AdminProgramController.index().url())
        .setIcon(Icons.ARROW_LEFT, IconPosition.START)
        .setText("Back")
        .setStyles("mt-6")
        .asAnchorText();
  }
}
