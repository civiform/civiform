package views.admin.reporting;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.p;
import static j2html.TagCreator.script;
import static j2html.TagCreator.td;
import static j2html.TagCreator.tr;
import static j2html.TagCreator.rawHtml;

import java.util.ArrayList;
import java.util.List;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.inject.Inject;
import j2html.tags.specialized.DivTag;
import play.mvc.Http;
import play.twirl.api.Content;
import services.reporting.ApplicationSubmissionsStat;
import services.reporting.ReportingService;
import views.BaseHtmlView;
import views.CspUtil;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Displays reporting statistics about applications to a specific program. */
public class AdminReportingShowView extends BaseHtmlView {
  private final AdminLayout layout;
  private final ReportingTableRenderer reportingTableRenderer;
  private final ObjectMapper objectMapper;

  @Inject
  public AdminReportingShowView(
      ReportingTableRenderer reportingTableRenderer, AdminLayoutFactory layoutFactory, ObjectMapper objectMapper) {
    this.reportingTableRenderer = checkNotNull(reportingTableRenderer);
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.REPORTING);
    this.objectMapper = checkNotNull(objectMapper);
  }

  public static final ImmutableList<ReportingTableRenderer.ReportingTableHeader>
      APPLICATION_COUNTS_FOR_PROGRAM_BY_MONTH_HEADERS =
          ImmutableList.of(
              ReportingTableRenderer.ReportingTableHeader.create("Month"),
              ReportingTableRenderer.ReportingTableHeader.create(
                  "Submissions", "The total number of applications submitted during this month."),
              ReportingTableRenderer.ReportingTableHeader.create(
                  "Time to complete (p25)",
                  "The 25th percentile time between when an applicant started and completed an"
                      + " application. Meaning 25% of applicants completed their application in"
                      + " this amount of time or less."),
              ReportingTableRenderer.ReportingTableHeader.create(
                  "Median time to complete",
                  "The median time between when an applicant started and completed an application."
                      + " Meaning 50% of applicants completed their application in this amount of"
                      + " time or less."),
              ReportingTableRenderer.ReportingTableHeader.create(
                  "Time to complete (p75)",
                  "The 75th percentile time between when an applicant started and completed an"
                      + " application. Meaning 75% of applicants completed their application in"
                      + " this amount of time or less."),
              ReportingTableRenderer.ReportingTableHeader.create(
                  "Time to complete (p99)",
                  "The 99th percentile time between when an applicant started and completed an"
                      + " application. Meaning 99% of applicants completed their application in"
                      + " this amount of time or less."));

  public Content render(
      Http.Request request,
      CiviFormProfile profile,
      String programSlug,
      String programName,
      String enUSLocalizedProgramName,
      ReportingService.MonthlyStats monthlyStats,
      ImmutableSortedMap<String, Long> completedBlockCount) {
    var title = String.format("%s reporting", enUSLocalizedProgramName);

    DivTag headerDiv =
        div()
            .withClasses("my-8")
            .with(h1(title).withClasses("my-4"))
            .with(p("Data may be up to an hour delayed."));

    DivTag contentDiv = div().withClasses("px-20").with(headerDiv);

    contentDiv.with(
        renderProgramMonthlyStats(
            programSlug, monthlyStats.monthlySubmissionsForProgram(programName)),
            renderChartData(request, completedBlockCount));

    HtmlBundle htmlBundle =
        layout.setAdminType(profile).getBundle(request).setTitle(title).addMainContent(contentDiv);
    return layout.renderCentered(htmlBundle);
  }

  private DivTag renderChartData(Http.Request request, ImmutableSortedMap<String, Long> completedBlockCount) {
    List<List<Object>> chartData = new ArrayList<>();
    // chartData.add(List.of("Block", "Completed Applications")); // Header row
    completedBlockCount.forEach((blockName, count) -> chartData.add(List.of(blockName, count)));

    String dataString = "";
    try {
      dataString = objectMapper.writeValueAsString(chartData);
    } catch (JsonProcessingException e) {
    }


    var scriptTag = script()
    .withType("text/javascript")
    .with(rawHtml("var applicantChartData = " + dataString + ";"));

    return div().with(
      div(),
      div().withId("eligibility-line"),
      div().withId("chart_data").with(CspUtil.applyCsp(request, scriptTag)),
      div().withId("chart_div"));
  }

  // private DivTag renderChart() {
  //   return div().with(
  //     script().withType("text/javascript")
  //     .withSrc("https://www.gstatic.com/charts/loader.js"),
  //     script("""
  //             google.charts.load('current', {packages: ['corechart']});
  //             google.charts.setOnLoadCallback(drawChart);

  //             drawChart() {
  //               alert("loaded;");
  //             }
  //         """).withType("text/javascript")
  //     );
  // }

  private DivTag renderProgramMonthlyStats(
      String programSlug, ImmutableList<ApplicationSubmissionsStat> programMonthlyStats) {
    return reportingTableRenderer
        .renderTable(
            "Submissions by month",
            APPLICATION_COUNTS_FOR_PROGRAM_BY_MONTH_HEADERS,
            programMonthlyStats.stream()
                .map(
                    stat ->
                        tr(
                            td(reportingTableRenderer.getDisplayMonth(stat)),
                            td(
                                ReportingTableRenderer.DECIMAL_FORMAT.format(
                                    stat.applicationCount())),
                            td(
                                reportingTableRenderer.renderDurationWithTestStubbing(
                                    stat.submissionDurationSeconds25p())),
                            td(
                                reportingTableRenderer.renderDurationWithTestStubbing(
                                    stat.submissionDurationSeconds50p())),
                            td(
                                reportingTableRenderer.renderDurationWithTestStubbing(
                                    stat.submissionDurationSeconds75p())),
                            td(
                                reportingTableRenderer.renderDurationWithTestStubbing(
                                    stat.submissionDurationSeconds99p()))))
                .collect(ImmutableList.toImmutableList()))
        .with(
            redirectButton(
                "by-programs-csv",
                "Download CSV",
                controllers.admin.routes.AdminReportingController.downloadProgramCsv(programSlug)
                    .url()));
  }
}
