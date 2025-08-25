package durablejobs.jobs;

import static com.google.common.base.Preconditions.checkNotNull;

import durablejobs.DurableJob;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import models.PersistedDurableJobModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateLastActivityTimeForAccounts extends DurableJob {
  private static final Logger logger =
      LoggerFactory.getLogger(UpdateLastActivityTimeForAccounts.class);
  private final Database database;
  private final PersistedDurableJobModel persistedDurableJobModel;

  public UpdateLastActivityTimeForAccounts(PersistedDurableJobModel persistedDurableJobModel) {
    this.persistedDurableJobModel = checkNotNull(persistedDurableJobModel);
    this.database = DB.getDefault();
  }

  @Override
  public PersistedDurableJobModel getPersistedDurableJob() {
    return persistedDurableJobModel;
  }

  @Override
  public void run() {
    logger.debug("Starting job to calculate last activity time for accounts");

    try (Transaction jobTransaction = database.beginTransaction(TxIsolation.SERIALIZABLE)) {
      int errorCount = 0;
      String sqlUpdate =
          """
UPDATE accounts
SET last_activity_time = subquery.last_activity_time
FROM (
    SELECT
        t1.account_id,
        GREATEST(
            COALESCE(MAX(t1.when_created), '1900-01-01 00:00:00'),
            COALESCE(MAX(t2.create_time), '1900-01-01 00:00:00'),
            COALESCE(MAX(t2.submit_time), '1900-01-01 00:00:00'),
            COALESCE(MAX(t2.status_last_modified_time), '1900-01-01 00:00:00')
        ) AS last_activity_time
    FROM
        applicants AS t1
    LEFT JOIN
        applications AS t2 ON t1.id = t2.applicant_id
    GROUP BY
        t1.account_id
) AS subquery
WHERE
    accounts.id = subquery.account_id
    AND accounts.last_activity_time IS NULL;
""";
      try {
        database.sqlUpdate(sqlUpdate).execute();
        logger.debug("Updated Accounts table with last_activity_time.");
      } catch (RuntimeException e) {
        logger.error(e.getMessage(), e);
        errorCount++;
      }
      if (errorCount == 0) {
        logger.debug("JOB SUCCESSFULLY EXECUTED");
        jobTransaction.commit();
      } else {
        jobTransaction.rollback();
      }
    }
  }
}
