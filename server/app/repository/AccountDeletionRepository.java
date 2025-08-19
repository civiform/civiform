package repository;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import models.AccountModel;
import models.ApplicantModel;
import models.ApplicationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.settings.SettingsManifest;

public final class AccountDeletionRepository {

  private static final Logger logger = LoggerFactory.getLogger(AccountDeletionRepository.class);
  private static final QueryProfileLocationBuilder queryProfileLocationBuilder =
      new QueryProfileLocationBuilder("AccountDeletionRepository");
  private final Database database;
  private final SettingsManifest settingsManifest;

  public AccountDeletionRepository(Database database, SettingsManifest settingsManifest) {
    this.database = DB.getDefault();
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  public void deleteAccount(AccountModel account) {
    ImmutableList<ApplicantModel> applicants =
        ImmutableList.copyOf(
            database
                .find(ApplicantModel.class)
                .where()
                .eq("account_id", account.id)
                .setLabel("ApplicantModel.findList")
                .findList());

    for (ApplicantModel applicant : applicants) {
      ImmutableList<ApplicationModel> applications =
          ImmutableList.copyOf(
              database
                  .find(ApplicationModel.class)
                  .where()
                  .eq("applicant_id", applicant.id)
                  .findList());

      for (ApplicationModel application : applications) {
        database.database.sqlQuery(
            "DELETE FROM application_events WHERE application_id=:applicationId");
      }
    }
  }
}
