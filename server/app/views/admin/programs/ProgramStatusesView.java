package views.admin.programs;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.each;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.p;

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
            p("Loading").withClass(Styles.HIDDEN), p(numResultsText))
        .with(each(statuses, status -> renderStatusItem(status)));
  }

  private Tag renderStatusItem(String status) {
    return p(status);
  }
}
