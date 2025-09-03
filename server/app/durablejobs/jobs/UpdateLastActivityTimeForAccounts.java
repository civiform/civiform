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
    Transaction jobTransaction = database.beginTransaction(TxIsolation.SERIALIZABLE);
    String sqlUpdate =
        """
UPDATE accounts
SET last_activity_time = subquery.last_activity_time
FROM (
  SELECT
      accounts.id,
      GREATEST(
          COALESCE(MAX(applicants.when_created), '1900-01-01 00:00:00'::timestamp),
          COALESCE(MAX(applications.create_time), '1900-01-01 00:00:00'::timestamp),
          COALESCE(MAX(applications.submit_time), '1900-01-01 00:00:00'::timestamp),
          COALESCE(MAX(applications.status_last_modified_time), '1900-01-01 00:00:00'::timestamp)
      ) AS last_activity_time
  FROM
      accounts
  LEFT JOIN
    applicants ON accounts.id=applicants.account_id
    LEFT JOIN
       applications ON applicants.id = applications.applicant_id
  GROUP BY
      accounts.id
) AS subquery
WHERE
  accounts.id = subquery.id
  AND accounts.last_activity_time IS NULL;
""";
    try (jobTransaction) {
      database.sqlUpdate(sqlUpdate).execute();
      jobTransaction.commit();
      logger.debug("JOB SUCCESSFULLY EXECUTED");
    } catch (RuntimeException e) {
      logger.error(e.getMessage(), e);
      jobTransaction.rollback();
    }
  }
}
