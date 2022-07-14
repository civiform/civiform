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
import j2html.tags.specialized.ATag;
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
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.FieldWithLabel;
import views.components.LinkElement;
import views.components.ToastMessage;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

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
                renderHeader("Create New Trusted Intermediary").withClass(Styles.MT_8),
                renderAddNewButton(request),
                renderHeader("Existing Trusted Intermediaries"),
                renderTiGroupCards(tis, request));

    if (request.flash().get("error").isPresent()) {
      LoggerFactory.getLogger(TrustedIntermediaryGroupListView.class)
          .info(request.flash().get("error").get());
      String error = request.flash().get("error").get();
      htmlBundle.addToastMessages(
          ToastMessage.error(error)
              .setId("warning-message-ti-form-fill")
              .setIgnorable(false)
              .setDuration(0));
    }
    return layout.renderCentered(htmlBundle);
  }

  private DivTag renderTiGroupCards(List<TrustedIntermediaryGroup> tis, Http.Request request) {
    return div(
        table()
            .withClasses(Styles.BORDER, Styles.BORDER_GRAY_300, Styles.SHADOW_MD, Styles.W_FULL)
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
            .setValue(request.flash().get("providedName").orElse(""))
            .setPlaceholderText("The name of this Trusted Intermediary Group.");
    FieldWithLabel descriptionField =
        FieldWithLabel.input()
            .setId("group-description-input")
            .setFieldName("description")
            .setLabelText("Description")
            .setValue(request.flash().get("providedDescription").orElse(""))
            .setPlaceholderText("The description of this group.");
    return div()
        .with(
            formTag.with(
                nameField.getInputTag(),
                descriptionField.getInputTag(),
                makeCsrfTokenInputTag(request),
                submitButton("Create").withClasses(Styles.ML_2, Styles.MB_6)))
        .withClasses(
            Styles.BORDER, Styles.BORDER_GRAY_300, Styles.SHADOW_MD, Styles.W_1_2, Styles.MT_6);
  }

  private TrTag renderGroupRow(TrustedIntermediaryGroup ti, Http.Request request) {
    return tr().withClasses(
            ReferenceClasses.ADMIN_TI_GROUP_ROW,
            Styles.BORDER_B,
            Styles.BORDER_GRAY_300,
            StyleUtils.even(Styles.BG_GRAY_100))
        .with(renderInfoCell(ti))
        .with(renderMemberCountCell(ti))
        .with(renderActionsCell(ti, request));
  }

  private TdTag renderInfoCell(TrustedIntermediaryGroup tiGroup) {
    return td().with(div(tiGroup.getName()).withClasses(Styles.FONT_SEMIBOLD))
        .with(div(tiGroup.getDescription()).withClasses(Styles.TEXT_XS))
        .withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.PR_12);
  }

  private TdTag renderMemberCountCell(TrustedIntermediaryGroup tiGroup) {
    return td().with(
            div("Members: " + tiGroup.getTrustedIntermediaries().size())
                .withClasses(Styles.FONT_SEMIBOLD))
        .with(div("Clients: " + tiGroup.getManagedAccounts().size()).withClasses(Styles.TEXT_SM))
        .withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.PR_12);
  }

  private TdTag renderActionsCell(TrustedIntermediaryGroup tiGroup, Http.Request request) {
    return td().with(renderEditButton(tiGroup), renderDeleteButton(tiGroup, request));
  }

  private FormTag renderDeleteButton(TrustedIntermediaryGroup tiGroup, Http.Request request) {
    return new LinkElement()
        .setText("Delete")
        .setId("delete-" + tiGroup.id + "-button")
        .setHref(routes.TrustedIntermediaryManagementController.delete(tiGroup.id).url())
        .asHiddenForm(request);
  }

  private ATag renderEditButton(TrustedIntermediaryGroup tiGroup) {
    return new LinkElement()
        .setText("Edit")
        .setId("edit-" + tiGroup.id + "-button")
        .setHref(routes.TrustedIntermediaryManagementController.edit(tiGroup.id).url())
        .asButton();
  }

  private TheadTag renderGroupTableHeader() {
    return thead(
        tr().withClasses(Styles.BORDER_B, Styles.BG_GRAY_200, Styles.TEXT_LEFT)
            .with(th("Name / Description").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_1_2))
            .with(th("Size").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_1_4))
            .with(
                th("Actions")
                    .withClasses(
                        BaseStyles.TABLE_CELL_STYLES,
                        Styles.TEXT_RIGHT,
                        Styles.PR_8,
                        Styles.W_1_6)));
  }
}
