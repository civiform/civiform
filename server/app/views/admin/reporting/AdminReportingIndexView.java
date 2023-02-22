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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.admin.AdminReportingController;
import j2html.tags.specialized.DivTag;
import j2html.tags.specialized.TableTag;
import j2html.tags.specialized.TbodyTag;
import j2html.tags.specialized.ThTag;
import j2html.tags.specialized.TrTag;
import java.text.DecimalFormat;
import java.time.Duration;
import java.util.Optional;
import play.twirl.api.Content;
import services.DateConverter;
import services.reporting.ApplicationSubmissionsStat;
import services.reporting.ReportingService;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.ViewUtils;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.components.Icons;

/** Summary view for reporting data. */
public final class AdminReportingIndexView extends BaseHtmlView {

  // Number format used for displaying application counts
  private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("###,###,###");
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

    contentDiv.with(renderAggregatedProgramStats(totalSubmissionsByProgram));
    contentDiv.with(renderAllApplicationsMonthlyStats(allApplicationsMonthlyStats));

    HtmlBundle htmlBundle = layout.getBundle().setTitle(title).addMainContent(contentDiv);
    return layout.renderCentered(htmlBundle);
  }

  /** Represents a column header in a reporting view. */
  @AutoValue
  public abstract static class ReportingTableHeader {
    public static ReportingTableHeader create(String headerText) {
      return new AutoValue_AdminReportingIndexView_ReportingTableHeader(
          headerText, /* helpText= */ Optional.empty());
    }

    public static ReportingTableHeader create(String headerText, String helpText) {
      return new AutoValue_AdminReportingIndexView_ReportingTableHeader(
          headerText, Optional.of(helpText));
    }

    /** The text in the header for this column. */
    public abstract String headerText();

    /** Optional help text that provides more detail on the data in the column. */
    public abstract Optional<String> helpText();
  }

  public static final ImmutableList<ReportingTableHeader> APPLICATION_COUNTS_BY_MONTH_HEADERS =
      ImmutableList.of(
          ReportingTableHeader.create("Month"),
          ReportingTableHeader.create(
              "Submissions", "The total number of applications submitted during this month."),
          ReportingTableHeader.create(
              "Time to complete (25th percentile)",
              "The 25th percentile time between when an applicant started and completed an"
                  + " application. Meaning 25% of applicants completed their application in this"
                  + " amount of time or less."),
          ReportingTableHeader.create(
              "Median time to complete",
              "The median time between when an applicant started and completed an application."
                  + " Meaning 50% of applicants completed their application in this amount of time"
                  + " or less."),
          ReportingTableHeader.create(
              "Time to complete (75th percentile)",
              "The 75th percentile time between when an applicant started and completed an"
                  + " application. Meaning 75% of applicants completed their application in this"
                  + " amount of time or less."),
          ReportingTableHeader.create(
              "Time to complete (99th percentile)",
              "The 99th percentile time between when an applicant started and completed an"
                  + " application. Meaning 99% of applicants completed their application in this"
                  + " amount of time or less."));

  private DivTag renderAllApplicationsMonthlyStats(
      ImmutableList<ApplicationSubmissionsStat> allApplicationsMonthlyStats) {

    return renderTable(
            "Submissions by month (all programs)",
            APPLICATION_COUNTS_BY_MONTH_HEADERS,
            allApplicationsMonthlyStats.stream()
                .map(
                    stat ->
                        tr(
                            td(dateConverter.renderMonthAndYear(stat.timestamp().get())),
                            td(DECIMAL_FORMAT.format(stat.applicationCount())),
                            td(renderDuration(stat.submissionDurationSeconds25p())),
                            td(renderDuration(stat.submissionDurationSeconds50p())),
                            td(renderDuration(stat.submissionDurationSeconds75p())),
                            td(renderDuration(stat.submissionDurationSeconds99p()))))
                .collect(ImmutableList.toImmutableList()))
        .with(
            redirectButton(
                "by-programs-csv",
                "Download CSV",
                controllers.admin.routes.AdminReportingController.downloadCsv(
                        AdminReportingController.DataSetName.APPLICATION_COUNTS_BY_MONTH.toString())
                    .url()));
  }

  public static final ImmutableList<ReportingTableHeader> APPLICATION_COUNTS_BY_PROGRAM_HEADERS =
      ImmutableList.of(
          ReportingTableHeader.create("Program"),
          ReportingTableHeader.create(
              "Submissions", "The total number of applications submitted to this program."),
          ReportingTableHeader.create(
              "Time to complete (25th percentile)",
              "The 25th percentile time between when an applicant started and completed an"
                  + " application. Meaning 25% of applicants completed their application in this"
                  + " amount of time or less."),
          ReportingTableHeader.create(
              "Median time to complete",
              "The median time between when an applicant started and completed an application."
                  + " Meaning 50% of applicants completed their application in this amount of time"
                  + " or less."),
          ReportingTableHeader.create(
              "Time to complete (75th percentile)",
              "The 75th percentile time between when an applicant started and completed an"
                  + " application. Meaning 75% of applicants completed their application in this"
                  + " amount of time or less."),
          ReportingTableHeader.create(
              "Time to complete (99th percentile)",
              "The 99th percentile time between when an applicant started and completed an"
                  + " application. Meaning 99% of applicants completed their application in this"
                  + " amount of time or less."));

  private DivTag renderAggregatedProgramStats(
      ImmutableList<ApplicationSubmissionsStat> aggregatedProgramStats) {

    return renderTable(
            "Submissions by program (all time)",
            APPLICATION_COUNTS_BY_PROGRAM_HEADERS,
            aggregatedProgramStats.stream()
                .map(
                    stat ->
                        tr(
                            td(stat.programName()),
                            td(DECIMAL_FORMAT.format(stat.applicationCount())),
                            td(renderDuration(stat.submissionDurationSeconds25p())),
                            td(renderDuration(stat.submissionDurationSeconds50p())),
                            td(renderDuration(stat.submissionDurationSeconds75p())),
                            td(renderDuration(stat.submissionDurationSeconds99p()))))
                .collect(ImmutableList.toImmutableList()))
        .with(
            redirectButton(
                "by-programs-csv",
                "Download CSV",
                controllers.admin.routes.AdminReportingController.downloadCsv(
                        AdminReportingController.DataSetName.APPLICATION_COUNTS_BY_PROGRAM
                            .toString())
                    .url()));
  }

  private DivTag renderTable(
      String title, ImmutableList<ReportingTableHeader> headers, Iterable<TrTag> dataRows) {
    DivTag content =
        div(h2(title)).withClasses("shadow border border-gray-300 rounded-lg p-6 mb-10");

    TbodyTag tbodyTag = tbody();

    TrTag headerRow = tr();
    for (ReportingTableHeader header : headers) {
      ThTag thTag = th(header.headerText()).withClasses("text-left pr-2 whitespace-nowrap");

      header
          .helpText()
          .ifPresent(helpText -> thTag.with(ViewUtils.makeSvgToolTip(helpText, Icons.INFO)));

      headerRow.with(thTag);
    }

    TableTag table =
        table().withClasses("border-collapse table-auto w-full").with(thead(headerRow), tbodyTag);

    int rowCount = 0;
    for (TrTag dataRow : dataRows) {
      tbodyTag.with(dataRow.withCondClass(rowCount++ % 2 == 0, "bg-slate-100"));
    }

    DivTag tableInnerContainer = div(table).withClasses("shadow-sm overflow-hidden my-8");
    DivTag tableOuterContainer = div(tableInnerContainer).withClasses("relative rounded-xl");

    return content.with(tableOuterContainer);
  }

  public static String renderDuration(double durationSeconds) {
    Duration duration = Duration.ofSeconds((long) durationSeconds);

    long days = duration.toDaysPart();
    long hours = duration.toHoursPart();
    int minutes = duration.toMinutesPart();
    int seconds = duration.toSecondsPart();

    StringBuilder result = new StringBuilder();

    if (days > 0) {
      result.append(days);
      result.append(":");
    }

    result.append(hours);
    result.append(":");
    result.append(minutes);
    result.append(":");
    result.append(seconds);

    return result.toString();
  }
}
