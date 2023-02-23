package durablejobs.jobs;

import com.google.common.base.Preconditions;
import durablejobs.DurableJob;
import models.PersistedDurableJob;
import repository.ReportingRepository;

public final class ReportingDashboardMonthlyRefreshJob extends DurableJob {
  private final ReportingRepository reportingRepository;
  private final PersistedDurableJob persistedDurableJob;

  public ReportingDashboardMonthlyRefreshJob(
      ReportingRepository persistedDurableJobRepository, PersistedDurableJob persistedDurableJob) {
    this.reportingRepository = Preconditions.checkNotNull(persistedDurableJobRepository);
    this.persistedDurableJob = Preconditions.checkNotNull(persistedDurableJob);
  }

  @Override
  public PersistedDurableJob getPersistedDurableJob() {
    return persistedDurableJob;
  }

  @Override
  public void run() {
    reportingRepository.refreshMonthlyReportingView();
  }
}
