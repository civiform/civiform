package views.admin.ti;

import static j2html.TagCreator.body;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.thead;
import static j2html.TagCreator.tr;

import com.google.inject.Inject;
import j2html.tags.Tag;
import java.util.List;
import models.TrustedIntermediaryGroup;
import play.twirl.api.Content;
import views.BaseHtmlView;
import views.admin.AdminLayout;
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

  public Content render(List<TrustedIntermediaryGroup> tis) {
    return layout.render(
        body(renderHeader("Trusted Intermediary Groups"), renderTiGroupCards(tis)));
  }

  private Tag renderTiGroupCards(List<TrustedIntermediaryGroup> tis) {
    return div(
        table()
            .withClasses(Styles.BORDER, Styles.BORDER_GRAY_300, Styles.SHADOW_MD, Styles.W_FULL)
            .with(renderGroupTableHeader())
            .with(tbody(each(tis, ti -> renderGroupRow(ti)))));
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
