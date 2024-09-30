package views.admin.ti;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.form;
import static j2html.TagCreator.input;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.thead;
import static j2html.TagCreator.tr;

import com.google.inject.Inject;
import controllers.FlashKey;
import controllers.admin.routes;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.TdTag;
import j2html.tags.specialized.TheadTag;
import j2html.tags.specialized.TrTag;
import models.AccountModel;
import models.TrustedIntermediaryGroupModel;
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

/**
 * Renders a page for viewing one trusted intermediary group and editing the members in the group.
 */
public class EditTrustedIntermediaryGroupView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public EditTrustedIntermediaryGroupView(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.INTERMEDIARIES);
  }

  public Content render(TrustedIntermediaryGroupModel tiGroup, Http.Request request) {
    String title = "Trusted intermediary groups";

    HtmlBundle htmlBundle =
        layout
            .getBundle(request)
            .setTitle(title)
            .addMainContent(
                div().withClasses("my-5").with(renderAddNewButton(tiGroup, request)),
                div(
                    table()
                        .withClasses("border", "border-gray-300", "shadow-md", "w-full")
                        .with(renderGroupTableHeader())
                        .with(
                            tbody(
                                each(
                                    tiGroup.getTrustedIntermediaries(),
                                    account -> renderTIRow(tiGroup, account, request))))));

    if (request.flash().get(FlashKey.ERROR).isPresent()) {
      LoggerFactory.getLogger(EditTrustedIntermediaryGroupView.class)
          .info(request.flash().get(FlashKey.ERROR).get());
      String error = request.flash().get(FlashKey.ERROR).get();
      htmlBundle.addToastMessages(
          ToastMessage.errorNonLocalized(error)
              .setId("warning-message-ti-form-fill")
              .setIgnorable(false)
              .setDuration(0));
    }

    return layout.renderCentered(htmlBundle);
  }

  private DivTag renderAddNewButton(TrustedIntermediaryGroupModel tiGroup, Http.Request request) {
    FormTag formTag =
        form()
            .withMethod("POST")
            .withAction(
                routes.TrustedIntermediaryManagementController.addIntermediary(tiGroup.id).url());
    FieldWithLabel emailField =
        FieldWithLabel.email()
            .setId("group-name-input")
            .setFieldName("emailAddress")
            .setLabelText("Member email address")
            .setValue(request.flash().get(FlashKey.PROVIDED_EMAIL_ADDRESS).orElse(""));
    return div()
        .with(
            formTag.with(
                emailField.getInputTag(),
                makeCsrfTokenInputTag(request),
                submitButton("Add").withClasses(ButtonStyles.SOLID_BLUE, "ml-2", "mb-6")))
        .withClasses("border", "border-gray-300", "shadow-md", "mt-6");
  }

  private TrTag renderTIRow(
      TrustedIntermediaryGroupModel tiGroup, AccountModel ti, Http.Request request) {
    return tr().withClasses(
            ReferenceClasses.ADMIN_QUESTION_TABLE_ROW,
            "border-b",
            "border-gray-300",
            StyleUtils.even("bg-gray-100"))
        .with(renderInfoCell(ti))
        .with(renderStatusCell(ti))
        .with(renderActionsCell(tiGroup, ti, request));
  }

  private TdTag renderInfoCell(AccountModel ti) {
    return td().with(div(ti.getApplicantDisplayName()).withClasses("font-semibold"))
        .with(div(ti.getEmailAddress()).withClasses("text-xs"))
        .withClasses(BaseStyles.TABLE_CELL_STYLES, "pr-12");
  }

  private TdTag renderStatusCell(AccountModel ti) {
    String accountStatus = "OK";
    if (ti.ownedApplicantIds().isEmpty()) {
      accountStatus = "Not yet signed in.";
    }
    return td().with(div(accountStatus).withClasses("font-semibold"))
        .withClasses(BaseStyles.TABLE_CELL_STYLES, "pr-12");
  }

  private TdTag renderActionsCell(
      TrustedIntermediaryGroupModel tiGroup, AccountModel account, Http.Request request) {
    return td().with(
            div()
                .withClasses("flex", "justify-end", "items-center", "pr-3")
                .with(renderDeleteButton(tiGroup, account, request)));
  }

  private FormTag renderDeleteButton(
      TrustedIntermediaryGroupModel tiGroup, AccountModel account, Http.Request request) {
    return form()
        .withMethod("POST")
        .withAction(
            routes.TrustedIntermediaryManagementController.removeIntermediary(tiGroup.id).url())
        .with(makeCsrfTokenInputTag(request))
        .with(input().isHidden().withName("accountId").withValue(account.id.toString()))
        .with(
            ViewUtils.makeSvgTextButton("Delete", Icons.DELETE)
                .withClasses(ButtonStyles.OUTLINED_WHITE_WITH_ICON));
  }

  private TheadTag renderGroupTableHeader() {
    return thead(
        tr().withClasses("border-b", "bg-gray-200", "text-left")
            .with(th("Members").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-1/2"))
            .with(th("Status").withClasses(BaseStyles.TABLE_CELL_STYLES, "w-1/4"))
            .with(
                th("Actions")
                    .withClasses(BaseStyles.TABLE_CELL_STYLES, "text-right", "pr-8", "w-1/6")));
  }
}
