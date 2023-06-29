package views.admin.ti;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.thead;
import static j2html.TagCreator.tr;

import com.google.inject.Inject;
import controllers.admin.routes;
import j2html.tags.specialized.ButtonTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.TdTag;
import j2html.tags.specialized.TheadTag;
import j2html.tags.specialized.TrTag;
import java.util.List;
import models.TrustedIntermediaryGroup;
import org.slf4j.LoggerFactory;
import play.mvc.Http;
import play.twirl.api.Content;
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
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;

/** Renders a page for viewing trusted intermediary groups. */
public class TrustedIntermediaryGroupListView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public TrustedIntermediaryGroupListView(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.INTERMEDIARIES);
  }

  public Content render(List<TrustedIntermediaryGroup> tis, Http.Request request) {
    String title = "Manage trusted intermediaries";
    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainContent(
                renderHeader("Create New Trusted Intermediary").withClass("mt-8"),
                renderAddNewButton(request),
                renderHeader("Existing Trusted Intermediaries"),
                renderTiGroupCards(tis, request));

    if (request.flash().get("error").isPresent()) {
      LoggerFactory.getLogger(TrustedIntermediaryGroupListView.class)
          .info(request.flash().get("error").get());
      String error = request.flash().get("error").get();
      htmlBundle.addToastMessages(
          ToastMessage.errorNonLocalized(error)
              .setId("warning-message-ti-form-fill")
              .setIgnorable(false)
              .setDuration(0));
    }
    return layout.renderCentered(htmlBundle);
  }

  private DivTag renderTiGroupCards(List<TrustedIntermediaryGroup> tis, Http.Request request) {
    return div(
        table()
            .withClasses("border", "border-gray-300", "shadow-md", "w-full")
            .with(renderGroupTableHeader())
            .with(tbody(each(tis, ti -> renderGroupRow(ti, request)))));
  }

  private DivTag renderAddNewButton(Http.Request request) {
    FormTag formTag =
        form()
            .withMethod("POST")
            .withAction(routes.TrustedIntermediaryManagementController.create().url());
    FieldWithLabel nameField =
        FieldWithLabel.input()
            .setId("group-name-input")
            .setFieldName("name")
            .setLabelText("Name")
            .setValue(request.flash().get("providedName").orElse(""));
    FieldWithLabel descriptionField =
        FieldWithLabel.input()
            .setId("group-description-input")
            .setFieldName("description")
            .setLabelText("Description")
            .setValue(request.flash().get("providedDescription").orElse(""));
    return div()
        .with(
            formTag.with(
                nameField.getInputTag(),
                descriptionField.getInputTag(),
                makeCsrfTokenInputTag(request),
                submitButton("Create").withClasses(ButtonStyles.SOLID_BLUE, "ml-2", "mb-6")))
        .withClasses("border", "border-gray-300", "shadow-md", "w-1/2", "mt-6");
  }

  private TrTag renderGroupRow(TrustedIntermediaryGroup ti, Http.Request request) {
    return tr().withClasses(
            ReferenceClasses.ADMIN_TI_GROUP_ROW,
            "border-b",
            "border-gray-300",
            StyleUtils.even("bg-gray-100"))
        .with(renderInfoCell(ti))
        .with(renderMemberCountCell(ti))
        .with(renderActionsCell(ti, request));
  }

  private TdTag renderInfoCell(TrustedIntermediaryGroup tiGroup) {
    return td().with(div(tiGroup.getName()).withClasses("font-semibold"))
        .with(div(tiGroup.getDescription()).withClasses("text-xs"))
        .withClasses(BaseStyles.TABLE_CELL_STYLES, "pr-12");
  }

  private TdTag renderMemberCountCell(TrustedIntermediaryGroup tiGroup) {
    return td().with(
            div("Members: " + tiGroup.getTrustedIntermediaries().size())
                .withClasses("font-semibold"))
        .with(div("Clients: " + tiGroup.getManagedAccounts().size()).withClasses("text-sm"))
        .withClasses(BaseStyles.TABLE_CELL_STYLES, "pr-12");
  }

  private TdTag renderActionsCell(TrustedIntermediaryGroup tiGroup, Http.Request request) {
    return td().with(
            div()
                .withClasses("flex", "items-center", "justify-end", "gap-3", "pr-3")
                .with(renderEditButton(tiGroup), renderDeleteButton(tiGroup, request)));
  }

  private FormTag renderDeleteButton(TrustedIntermediaryGroup tiGroup, Http.Request request) {
    return form()
        .withMethod("POST")
        .withAction(routes.TrustedIntermediaryManagementController.delete(tiGroup.id).url())
        .with(makeCsrfTokenInputTag(request))
        .with(
            ViewUtils.makeSvgTextButton("Delete", Icons.DELETE)
                .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON));
  }

  private ButtonTag renderEditButton(TrustedIntermediaryGroup tiGroup) {
    return asRedirectElement(
        ViewUtils.makeSvgTextButton("Edit members", Icons.EDIT)
            .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON),
        routes.TrustedIntermediaryManagementController.edit(tiGroup.id).url());
  }

  private TheadTag renderGroupTableHeader() {
    return thead(
        tr().withClasses("border-b", "bg-gray-200", "text-left")
            .with(th("Name / Description").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-1/3"))
            .with(th("Size").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-1/3"))
            .with(
                th("Actions")
                    .withClasses(BaseStyles.TABLE_CELL_STYLES, "text-right", "pr-8", "w-1/3")));
  }
}
