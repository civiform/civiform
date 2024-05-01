package durablejobs.jobs;

import com.google.common.base.Preconditions;
import durablejobs.DurableJob;
import models.PersistedDurableJobModel;
import repository.ReportingRepository;

public final class ReportingDashboardMonthlyRefreshJob extends DurableJob {
  private final ReportingRepository reportingRepository;
  private final PersistedDurableJobModel persistedDurableJob;

  public ReportingDashboardMonthlyRefreshJob(
      ReportingRepository reportingRepository, PersistedDurableJobModel persistedDurableJob) {
    this.reportingRepository = Preconditions.checkNotNull(reportingRepository);
    this.persistedDurableJob = Preconditions.checkNotNull(persistedDurableJob);
  }

  @Override
  public PersistedDurableJobModel getPersistedDurableJob() {
    return persistedDurableJob;
  }

  @Override
  public void run() {
    reportingRepository.refreshMonthlyReportingView();
  }
}
