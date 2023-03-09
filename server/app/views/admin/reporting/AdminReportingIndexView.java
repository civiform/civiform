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
import controllers.admin.AdminReportingController;
import j2html.tags.specialized.DivTag;
import modules.MainModule;
import play.twirl.api.Content;
import services.reporting.ApplicationSubmissionsStat;
import services.reporting.ReportingService;
import views.BaseHtmlView;
import views.HtmlBundle;
import views.admin.AdminLayout;
import views.admin.AdminLayoutFactory;
import views.components.LinkElement;

/** Summary view for reporting data. */
public final class AdminReportingIndexView extends BaseHtmlView {

  private final AdminLayout layout;
  private final ReportingTableRenderer reportingTableRenderer;

  @Inject
  public AdminReportingIndexView(
      ReportingTableRenderer reportingTableRenderer, AdminLayoutFactory layoutFactory) {
    this.reportingTableRenderer = checkNotNull(reportingTableRenderer);
    this.layout = checkNotNull(layoutFactory).getLayout(AdminLayout.NavPage.REPORTING);
  }

  public Content render(CiviFormProfile profile, ReportingService.MonthlyStats monthlyStats) {
    if (profile.isProgramAdmin()) {
      layout.setOnlyProgramAdminType();
    }

    ImmutableList<ApplicationSubmissionsStat> allApplicationsMonthlyStats =
        monthlyStats.monthlySubmissionsAggregated();
    ImmutableList<ApplicationSubmissionsStat> totalSubmissionsByProgram =
        monthlyStats.totalSubmissionsByProgram();

    var title = "Reporting";

    DivTag headerDiv =
        div()
            .withClasses("my-8")
            .with(h1(title).withClasses("my-4"))
            .with(p("Data may be up to an hour delayed."));

    DivTag contentDiv = div().withClasses("px-20").with(headerDiv);

    contentDiv.with(renderAggregatedProgramStats(totalSubmissionsByProgram));
    contentDiv.with(renderAllApplicationsMonthlyStats(allApplicationsMonthlyStats));

    HtmlBundle htmlBundle = layout.getBundle().setTitle(title).addMainContent(contentDiv);
    return layout.renderCentered(htmlBundle);
  }

  public static final ImmutableList<ReportingTableRenderer.ReportingTableHeader>
      APPLICATION_COUNTS_BY_MONTH_HEADERS =
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

  private DivTag renderAllApplicationsMonthlyStats(
      ImmutableList<ApplicationSubmissionsStat> allApplicationsMonthlyStats) {

    return reportingTableRenderer
        .renderTable(
            "Submissions by month (all programs)",
            APPLICATION_COUNTS_BY_MONTH_HEADERS,
            allApplicationsMonthlyStats.stream()
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
                controllers.admin.routes.AdminReportingController.downloadCsv(
                        AdminReportingController.DataSetName.APPLICATION_COUNTS_BY_MONTH.toString())
                    .url()));
  }

  public static final ImmutableList<ReportingTableRenderer.ReportingTableHeader>
      APPLICATION_COUNTS_BY_PROGRAM_HEADERS =
          ImmutableList.of(
              ReportingTableRenderer.ReportingTableHeader.create("Program"),
              ReportingTableRenderer.ReportingTableHeader.create(
                  "Submissions", "The total number of applications submitted to this program."),
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

  private DivTag renderAggregatedProgramStats(
      ImmutableList<ApplicationSubmissionsStat> aggregatedProgramStats) {

    return reportingTableRenderer
        .renderTable(
            "Submissions by program (all time)",
            APPLICATION_COUNTS_BY_PROGRAM_HEADERS,
            aggregatedProgramStats.stream()
                .map(
                    stat ->
                        tr(
                            td(
                                new LinkElement()
                                    .setText(stat.programName())
                                    .setHref(
                                        controllers.admin.routes.AdminReportingController.show(
                                                MainModule.SLUGIFIER.slugify(stat.programName()))
                                            .url())
                                    .asAnchorText()),
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
                controllers.admin.routes.AdminReportingController.downloadCsv(
                        AdminReportingController.DataSetName.APPLICATION_COUNTS_BY_PROGRAM
                            .toString())
                    .url()));
  }
}
