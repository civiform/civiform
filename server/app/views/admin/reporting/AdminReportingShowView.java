package views.admin.reporting;

import static com.google.common.base.Preconditions.checkNotNull;
import static j2html.TagCreator.div;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.p;
import static j2html.TagCreator.td;
import static j2html.TagCreator.tr;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import j2html.tags.specialized.DivTag;
import play.twirl.api.Content;
import services.reporting.ApplicationSubmissionsStat;
import services.reporting.ReportingService;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;

/** Displays reporting statistics about applications to a specific program. */
public class AdminReportingShowView extends BaseHtmlView {
  private final AdminLayout layout;
  private final ReportingTableRenderer reportingTableRenderer;

  @Inject
  public AdminReportingShowView(
      ReportingTableRenderer reportingTableRenderer, AdminLayoutFactory layoutFactory) {
    this.reportingTableRenderer = checkNotNull(reportingTableRenderer);
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.REPORTING);
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
      CiviFormProfile profile,
      String programSlug,
      String programName,
      ReportingService.MonthlyStats monthlyStats) {
    if (profile.isProgramAdmin()) {
      layout.setOnlyProgramAdminType();
    }

    var title = String.format("%s Reporting", programName);

    DivTag headerDiv =
        div()
            .withClasses("my-8")
            .with(h1(title).withClasses("my-4"))
            .with(p("Data may be up to an hour delayed."));

    DivTag contentDiv = div().withClasses("px-20").with(headerDiv);

    contentDiv.with(
        renderProgramMonthlyStats(
            programSlug, monthlyStats.monthlySubmissionsForProgram(programName)));

    HtmlBundle htmlBundle = layout.getBundle().setTitle(title).addMainContent(contentDiv);
    return layout.renderCentered(htmlBundle);
  }

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
