package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.p;
import static j2html.TagCreator.span;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import play.twirl.api.Content;
import services.program.ProgramDefinition;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayout.NavPage;
import views.admin.AdminLayoutFactory;
import views.components.Icons;
import views.style.AdminStyles;
import views.style.StyleUtils;
import views.style.Styles;

public final class ProgramStatusesView extends BaseHtmlView {
  private final AdminLayout layout;

  @Inject
  public ProgramStatusesView(AdminLayoutFactory layoutFactory) {
    this.layout = checkNotNull(layoutFactory).getLayout(NavPage.PROGRAMS);
  }

  public Content render(ProgramDefinition program) {
    // TODO(clouser): Pull this from the database model once available.
    ImmutableList<String> actualStatuses =
        ImmutableList.of("Approved", "Denied", "Needs more information");
    ContainerTag contentDiv =
        div()
            .withClasses(Styles.PX_4)
            .with(
                div()
                    .withClasses(
                        Styles.FLEX,
                        Styles.ITEMS_CENTER,
                        Styles.SPACE_X_4,
                        Styles.MT_12,
                        Styles.MB_10)
                    .with(
                        h1(
                            String.format(
                                "Manage application status options for %s", program.adminName())),
                        div().withClass(Styles.FLEX_GROW),
                        // TODO(clouser): Make this a link or modal button once that part of the UI
                        // has been created (and routes have been created).
                        makeSvgTextButton("Create a new status", Icons.PLUS_SVG_PATH)
                            .withClasses(AdminStyles.SECONDARY_BUTTON_STYLES, Styles.MY_2)),
                renderStatusContainer(actualStatuses));

    HtmlBundle htmlBundle =
        layout.getBundle().setTitle("Manage program statuses").addMainContent(contentDiv);

    return layout.renderCentered(htmlBundle);
  }

  private Tag renderStatusContainer(ImmutableList<String> statuses) {
    String numResultsText =
        statuses.size() == 1 ? "1 result" : String.format("%d results", statuses.size());
    return div()
        .with(
            // TODO(clouser): Add JS for hiding / unhiding this.
            p("Loading").withClass(Styles.HIDDEN),
            p(numResultsText),
            div()
                .withClasses(Styles.MT_6, Styles.BORDER, Styles.ROUNDED_MD, Styles.DIVIDE_Y)
                .with(each(statuses, status -> renderStatusItem(status))));
  }

  private Tag renderStatusItem(String status) {
    return div()
        .withClasses(
            Styles.PL_7,
            Styles.PR_6,
            Styles.PY_9,
            Styles.FONT_NORMAL,
            Styles.SPACE_X_2,
            Styles.FLEX,
            Styles.ITEMS_CENTER,
            StyleUtils.hover(Styles.BG_GRAY_100))
        .with(
            div()
                .withClass(Styles.W_1_4)
                .with(
                    // TODO(clouser): Optional SVG icon.
                    span(status).withClasses(Styles.ML_2, Styles.BREAK_WORDS)),
            div()
                .with(
                    p().withClass(Styles.TEXT_SM)
                        .with(
                            span("Edited on "),
                            // TODO(clouser): Get actual edit date from data model.
                            span("06/02/2022").withClass(Styles.FONT_SEMIBOLD)))
                .condWith(
                    status.equals("Approved"),
                    p().withClasses(Styles.MT_1, Styles.TEXT_XS, Styles.FLEX, Styles.ITEMS_CENTER)
                        .with(
                            Icons.svg(Icons.EMAIL_SVG_PATH, 22)
                                // TODO(clouser): Once SVG icon sizes are consistent, just set size
                                // to 18.
                                .attr("width", 18)
                                .attr("height", 18)
                                .withClasses(Styles.MR_2, Styles.INLINE_BLOCK),
                            span("Applicant notification email added"))),
            div().withClass(Styles.FLEX_GROW),
            makeSvgTextButton("Delete", Icons.DELETE_SVG_PATH)
                .withClass(AdminStyles.TERTIARY_BUTTON_STYLES),
            makeSvgTextButton("Edit", Icons.EDIT_SVG_PATH)
                .withClass(AdminStyles.TERTIARY_BUTTON_STYLES));
  }
}
