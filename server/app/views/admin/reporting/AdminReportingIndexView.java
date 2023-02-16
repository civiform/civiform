package views.admin.reporting;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.h2;
import static j2html.TagCreator.table;
import static j2html.TagCreator.tbody;
import static j2html.TagCreator.td;
import static j2html.TagCreator.th;
import static j2html.TagCreator.thead;
import static j2html.TagCreator.tr;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.TableTag;
import j2html.tags.specialized.TbodyTag;
import j2html.tags.specialized.TrTag;
import java.text.DecimalFormat;
import play.twirl.api.Content;
import services.DateConverter;
import services.reporting.ApplicationSubmissionsStat;
import services.reporting.ReportingService;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.components.LinkElement;

/** Summary view for reporting data. */
public final class AdminReportingIndexView extends BaseHtmlView {

  // Number format used for displaying application counts
  public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###,###,###");
  private final AdminLayout layout;
  private final DateConverter dateConverter;

  @Inject
  public AdminReportingIndexView(AdminLayoutFactory layoutFactory, DateConverter dateConverter) {
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.REPORTING);
    this.dateConverter = checkNotNull(dateConverter);
  }

  public Content render(ReportingService.MonthlyStats monthlyStats) {
    ImmutableList<ApplicationSubmissionsStat> allApplicationsMonthlyStats =
        monthlyStats.monthlySubmissionsAggregated();
    ImmutableList<ApplicationSubmissionsStat> totalSubmissionsByProgram =
        monthlyStats.totalSubmissionsByProgram();

    var title = "Reporting";

    DivTag headerDiv =
        div()
            .withClasses("flex", "place-content-between", "my-8")
            .with(h1(title).withClasses("my-4"));

    DivTag contentDiv = div().withClasses("px-20").with(headerDiv);

    contentDiv.with(renderAllApplicationsMonthlyStats(allApplicationsMonthlyStats));
    contentDiv.with(renderAggregatedProgramStats(totalSubmissionsByProgram));

    HtmlBundle htmlBundle = layout.getBundle().setTitle(title).addMainContent(contentDiv);
    return layout.renderCentered(htmlBundle);
  }

  private DivTag renderAllApplicationsMonthlyStats(
      ImmutableList<ApplicationSubmissionsStat> allApplicationsMonthlyStats) {

    return renderTable(
        "By month, all programs",
        ImmutableList.of(
            "Time period",
            "Applications submitted",
            "25p time to submit",
            "50p time to submit",
            "75p time to submit",
            "99p time to submit"),
        allApplicationsMonthlyStats.stream()
            .map(
                stat ->
                    tr(
                        td(dateConverter.renderMonthAndYear(stat.timestamp())),
                        td(DECIMAL_FORMAT.format(stat.applicationCount())),
                        td(renderDuration(stat.submissionDurationSeconds25p())),
                        td(renderDuration(stat.submissionDurationSeconds50p())),
                        td(renderDuration(stat.submissionDurationSeconds75p())),
                        td(renderDuration(stat.submissionDurationSeconds99p()))))
            .collect(ImmutableList.toImmutableList()));
  }

  private DivTag renderAggregatedProgramStats(
      ImmutableList<ApplicationSubmissionsStat> aggregatedProgramStats) {

    return renderTable(
        "By program",
        ImmutableList.of(
            "Program",
            "Applications submitted, total",
            "25p time to submit",
            "50p time to submit",
            "75p time to submit",
            "99p time to submit"),
        aggregatedProgramStats.stream()
            .map(
                stat ->
                    tr(
                        td(
                            new LinkElement()
                                .setHref("/")
                                .setText(stat.programName())
                                .asAnchorText()),
                        td(DECIMAL_FORMAT.format(stat.applicationCount())),
                        td(renderDuration(stat.submissionDurationSeconds25p())),
                        td(renderDuration(stat.submissionDurationSeconds50p())),
                        td(renderDuration(stat.submissionDurationSeconds75p())),
                        td(renderDuration(stat.submissionDurationSeconds99p()))))
            .collect(ImmutableList.toImmutableList()));
  }

  private DivTag renderTable(
      String title, ImmutableList<String> headers, Iterable<TrTag> dataRows) {
    DivTag content =
        div(h2(title)).withClasses("shadow border border-gray-300 rounded-lg p-6 mb-10");

    TbodyTag tbodyTag = tbody();

    TrTag headerRow = tr();
    for (String header : headers) {
      headerRow.with(th(header).withClasses("text-left pr-1"));
    }

    TableTag table =
        table().withClasses("border-collapse table-auto w-full").with(thead(headerRow), tbodyTag);

    int rowCount = 0;
    for (TrTag dataRow : dataRows) {
      tbodyTag.with(dataRow.withCondClass(rowCount++ % 2 == 0, "bg-slate-100"));
    }

    DivTag tableInnerContainer = div(table).withClasses("shadow-sm overflow-hidden my-8");
    DivTag tableOuterContainer =
        div(tableInnerContainer).withClasses("relative rounded-xl overflow-auto");

    return content.with(tableOuterContainer, button("Download CSV"));
  }

  private static String renderDuration(double durationSeconds) {
    return durationSeconds + " seconds";
  }
}
