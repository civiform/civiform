package durablejobs.jobs;

import com.google.common.base.Preconditions;
import com.google.inject.Provider;
import durablejobs.DurableJob;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import models.PersistedDurableJobModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.AccountRepository;

/** Destroys all guest accounts older than a set age that have not started any applications. */
public final class UnusedAccountCleanupJob extends DurableJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(UnusedAccountCleanupJob.class);
  private static final int UNUSED_ACCOUNT_MIN_AGE_IN_DAYS = 90;

  private final AccountRepository accountRepository;
  private final Provider<LocalDateTime> nowProvider;
  private final PersistedDurableJobModel persistedDurableJob;

  public UnusedAccountCleanupJob(
      AccountRepository accountRepository,
      Provider<LocalDateTime> nowProvider,
      PersistedDurableJobModel persistedDurableJob) {
    this.accountRepository = Preconditions.checkNotNull(accountRepository);
    this.nowProvider = Preconditions.checkNotNull(nowProvider);
    this.persistedDurableJob = Preconditions.checkNotNull(persistedDurableJob);
  }

  @Override
  public PersistedDurableJobModel getPersistedDurableJob() {
    return persistedDurableJob;
  }

  @Override
  public void run() {
    LocalDateTime cutoff = nowProvider.get().minus(UNUSED_ACCOUNT_MIN_AGE_IN_DAYS, ChronoUnit.DAYS);
    int numberDeleted = accountRepository.deleteUnusedGuestAccounts(UNUSED_ACCOUNT_MIN_AGE_IN_DAYS);

    LOGGER.info("Deleted {} accounts created before {}", numberDeleted, cutoff);
  }
}
