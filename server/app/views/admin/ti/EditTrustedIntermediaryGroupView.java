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

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import controllers.admin.routes;

import j2html.tags.specialized.FormTag;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.TrTag;
import j2html.tags.specialized.TdTag;
import j2html.tags.specialized.TheadTag;
import j2html.tags.specialized.InputTag;

import models.Account;
import models.TrustedIntermediaryGroup;
import play.mvc.Http;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;
import views.components.LinkElement;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

/**
 * Renders a page for viewing one trusted intermediary group and editing the members in the group.
 */
public class EditTrustedIntermediaryGroupView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public EditTrustedIntermediaryGroupView(AdminLayout layout) {
    this.layout = checkNotNull(layout);
  }

  public Content render(TrustedIntermediaryGroup tiGroup, Http.Request request) {
    String title = "Trusted Intermediary Groups";

    HtmlBundle htmlBundle =
        layout
            .getBundle()
            .setTitle(title)
            .addMainContent(
                div().withClasses(Styles.MY_5).with(renderAddNewButton(tiGroup, request)),
                div(
                    table()
                        .withClasses(
                            Styles.BORDER, Styles.BORDER_GRAY_300, Styles.SHADOW_MD, Styles.W_FULL)
                        .with(renderGroupTableHeader())
                        .with(
                            tbody(
                                each(
                                    tiGroup.getTrustedIntermediaries(),
                                    account -> renderTIRow(tiGroup, account, request))))));

    return layout.renderCentered(htmlBundle);
  }

  private DivTag renderAddNewButton(TrustedIntermediaryGroup tiGroup, Http.Request request) {
    FormTag formTag =
        form()
            .withMethod("POST")
            .attr("action", 
                routes.TrustedIntermediaryManagementController.addIntermediary(tiGroup.id).url());
    FieldWithLabel emailField =
        FieldWithLabel.input()
            .setId("group-name-input")
            .setFieldName("emailAddress")
            .setLabelText("Member Email Address")
            .setValue(request.flash().get("providedEmail").orElse(""))
            .setPlaceholderText("The email address of the member you want to add.");
    return div()
        .with(
            formTag.with(
                emailField.getContainer(),
                makeCsrfTokenInputTag(request),
                submitButton("Add").withClasses(Styles.ML_2, Styles.MB_6)))
        .withClasses(Styles.BORDER, Styles.BORDER_GRAY_300, Styles.SHADOW_MD, Styles.MT_6);
  }

  private TrTag renderTIRow(TrustedIntermediaryGroup tiGroup, Account ti, Http.Request request) {
    return tr().withClasses(
            ReferenceClasses.ADMIN_QUESTION_TABLE_ROW,
            Styles.BORDER_B,
            Styles.BORDER_GRAY_300,
            StyleUtils.even(Styles.BG_GRAY_100))
        .with(renderInfoCell(ti))
        .with(renderStatusCell(ti))
        .with(renderActionsCell(tiGroup, ti, request));
  }

  private TdTag renderInfoCell(Account ti) {
    return td().with(div(ti.getApplicantName()).withClasses(Styles.FONT_SEMIBOLD))
        .with(div(ti.getEmailAddress()).withClasses(Styles.TEXT_XS))
        .withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.PR_12);
  }

  private TdTag renderStatusCell(Account ti) {
    String accountStatus = "OK";
    if (ti.ownedApplicantIds().isEmpty()) {
      accountStatus = "Not yet signed in.";
    }
    return td().with(div(accountStatus).withClasses(Styles.FONT_SEMIBOLD))
        .withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.PR_12);
  }

  private TdTag renderActionsCell(
      TrustedIntermediaryGroup tiGroup, Account account, Http.Request request) {
    return td().with(renderDeleteButton(tiGroup, account, request));
  }

  private FormTag renderDeleteButton(
      TrustedIntermediaryGroup tiGroup, Account account, Http.Request request) {
    return new LinkElement()
        .setText("Delete")
        .setId("delete-" + account.id + "-button")
        .setHref(
            routes.TrustedIntermediaryManagementController.removeIntermediary(tiGroup.id).url())
        .asHiddenForm(request, ImmutableMap.of("accountId", account.id.toString()));
  }

  private TheadTag renderGroupTableHeader() {
    return thead(
        tr().withClasses(Styles.BORDER_B, Styles.BG_GRAY_200, Styles.TEXT_LEFT)
            .with(th("Members").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_1_2))
            .with(th("Status").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_1_4))
            .with(
                th("Actions")
                    .withClasses(
                        BaseStyles.TABLE_CELL_STYLES,
                        Styles.TEXT_RIGHT,
                        Styles.PR_8,
                        Styles.W_1_6)));
  }
}
