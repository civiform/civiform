package durablejobs.jobs;

import static com.google.common.base.Preconditions.checkNotNull;

import durablejobs.DurableJob;
import io.ebean.DB;
import io.ebean.Database;
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
    database
        .sqlUpdate(
            "UPDATE accounts\n"
                + "SET last_activity_time = subquery.last_activity_time\n"
                + "FROM (\n"
                + "    SELECT\n"
                + "        t1.account_id,\n"
                + "        GREATEST(\n"
                + "            COALESCE(MAX(t1.when_created), '1900-01-01 00:00:00'),\n"
                + "            COALESCE(MAX(t2.create_time), '1900-01-01 00:00:00'),\n"
                + "            COALESCE(MAX(t2.submit_time), '1900-01-01 00:00:00'),\n"
                + "            COALESCE(MAX(t2.status_last_modified_time), '1900-01-01 00:00:00')\n"
                + "        ) AS last_activity_time\n"
                + "    FROM\n"
                + "        applicants AS t1\n"
                + "    LEFT JOIN\n"
                + "        applications AS t2 ON t1.id = t2.applicant_id\n"
                + "    GROUP BY\n"
                + "        t1.account_id\n"
                + ") AS subquery\n"
                + "WHERE\n"
                + "    accounts.id = subquery.account_id\n"
                + "    AND accounts.last_activity_time IS NULL;")
        .execute();
    logger.debug("Ending job to calculate last activity time for accounts");
  }
}
