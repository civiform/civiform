package views.admin.ti;

import static j2html.TagCreator.body;
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
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import java.util.List;
import models.TrustedIntermediaryGroup;
import org.slf4j.LoggerFactory;
import play.mvc.Http;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.admin.AdminLayout;
import views.components.FieldWithLabel;
import views.components.ToastMessage;
import views.style.BaseStyles;
import views.style.ReferenceClasses;
import views.style.StyleUtils;
import views.style.Styles;

public class TrustedIntermediaryGroupListView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public TrustedIntermediaryGroupListView(AdminLayout layout) {
    this.layout = layout;
  }

  public Content render(List<TrustedIntermediaryGroup> tis, Http.Request request) {
    ContainerTag body =
        body(
            renderHeader("Trusted Intermediary Groups"),
            renderTiGroupCards(tis),
            renderHeader("Create New Group").withClass(Styles.MT_8),
            renderAddNewButton(request));
    if (request.flash().get("error").isPresent()) {
      LoggerFactory.getLogger(TrustedIntermediaryGroupListView.class)
          .info(request.flash().get("error").get());
      String error = request.flash().get("error").get();
      body.with(
          ToastMessage.error(error)
              .setId("warning-message-ti-form-fill")
              .setIgnorable(false)
              .setDuration(0)
              .getContainerTag());
    }
    return layout.render(body);
  }

  private Tag renderTiGroupCards(List<TrustedIntermediaryGroup> tis) {
    return div(
        table()
            .withClasses(Styles.BORDER, Styles.BORDER_GRAY_300, Styles.SHADOW_MD, Styles.W_FULL)
            .with(renderGroupTableHeader())
            .with(tbody(each(tis, ti -> renderGroupRow(ti)))));
  }

  private Tag renderAddNewButton(Http.Request request) {
    ContainerTag formTag =
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
                nameField.getContainer(),
                descriptionField.getContainer(),
                makeCsrfTokenInputTag(request),
                submitButton("Create").withClasses(Styles.ML_2, Styles.MB_6)))
        .withClasses(
            Styles.BORDER, Styles.BORDER_GRAY_300, Styles.SHADOW_MD, Styles.W_1_2, Styles.MT_6);
  }

  private Tag renderGroupRow(TrustedIntermediaryGroup ti) {
    return tr().withClasses(
            ReferenceClasses.ADMIN_QUESTION_TABLE_ROW,
            Styles.BORDER_B,
            Styles.BORDER_GRAY_300,
            StyleUtils.even(Styles.BG_GRAY_100))
        .with(renderInfoCell(ti))
        .with(renderMemberCountCell(ti))
        .with(renderActionsCell(ti));
  }

  private Tag renderInfoCell(TrustedIntermediaryGroup tiGroup) {
    return td().with(div(tiGroup.getName()).withClasses(Styles.FONT_SEMIBOLD))
        .with(div(tiGroup.getDescription()).withClasses(Styles.TEXT_XS))
        .withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.PR_12);
  }

  private Tag renderMemberCountCell(TrustedIntermediaryGroup tiGroup) {
    return td().with(
            div("Trusted Intermediaries: " + tiGroup.getTrustedIntermediaries().size())
                .withClasses(Styles.FONT_SEMIBOLD))
        .with(
            div("Managed Accounts: " + tiGroup.getManagedAccounts().size())
                .withClasses(Styles.TEXT_SM))
        .withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.PR_12);
  }

  private Tag renderActionsCell(TrustedIntermediaryGroup tiGroup) {
    return td().with(div(tiGroup.toString()));
  }

  private Tag renderGroupTableHeader() {
    return thead(
        tr().withClasses(Styles.BORDER_B, Styles.BG_GRAY_200, Styles.TEXT_LEFT)
            .with(th("Info").withClasses(BaseStyles.TABLE_CELL_STYLES, Styles.W_1_2))
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
