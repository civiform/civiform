package durablejobs.jobs;

import com.google.common.base.Preconditions;
import com.google.inject.Provider;
import durablejobs.DurableJob;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import models.PersistedDurableJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.AccountRepository;
import services.DateConverter;

public final class UnusedAccountCleanupJob extends DurableJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(OldJobCleanupJob.class);
  private static final Duration MAX_AGE_UNUSED_ACCOUNT = Duration.of(3L, ChronoUnit.MONTHS);

  private final AccountRepository accountRepository;
  private final DateConverter dateConverter;
  private final Provider<LocalDateTime> nowProvider;
  private final PersistedDurableJob persistedDurableJob;

  public UnusedAccountCleanupJob(
    AccountRepository accountRepository,
    DateConverter dateConverter,
    Provider<LocalDateTime> nowProvider,
    PersistedDurableJob persistedDurableJob) {
    this.accountRepository = Preconditions.checkNotNull(accountRepository);
    this.dateConverter = Preconditions.checkNotNull(dateConverter);
    this.nowProvider = Preconditions.checkNotNull(nowProvider);
    this.persistedDurableJob = Preconditions.checkNotNull(persistedDurableJob);
  }

  @Override
  public PersistedDurableJob getPersistedDurableJob() {
    return persistedDurableJob;
  }

  @Override
  public void run() {
    LocalDateTime cutoff = nowProvider.get().minus(MAX_AGE_UNUSED_ACCOUNT);
    Instant cutoffInstant = dateConverter.localDateTimeToInstant(cutoff);

    int numberDeleted = accountRepository.deleteUnusedGuestAccounts(cutoffInstant);

    LOGGER.info("Deleted {} accounts created before {}", numberDeleted, cutoff);
  }
}
