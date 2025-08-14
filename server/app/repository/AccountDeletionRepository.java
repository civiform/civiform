package repository;

import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import models.AccountModel;
import models.ApplicantModel;
import models.ApplicationModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.settings.SettingsManifest;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public final class AccountDeletionRepository {

  private static final Logger logger = LoggerFactory.getLogger(AccountDeletionRepository.class);
  private static final QueryProfileLocationBuilder queryProfileLocationBuilder =
    new QueryProfileLocationBuilder("AccountDeletionRepository");
  private final Database database;
  private final SettingsManifest settingsManifest;

  public  AccountDeletionRepository(Database database, SettingsManifest settingsManifest) {
    this.database = DB.getDefault();
    this.settingsManifest = checkNotNull(settingsManifest);
  }

  public  void deleteAccount(AccountModel account) {
    ImmutableList<ApplicantModel> applicants = database
      .sqlQuery(  "SELECT id FROM applicants WHERE account_id=:currentAccount")
      .setParameter("currentAccount",account.id)
      .findList()
      .stream()
      .collect(ImmutableList.toImmutableList());

    for(ApplicantModel applicant :  applicants){
      ImmutableList<ApplicationModel> applications =
        database
          .sqlQuery(  "SELECT id FROM applications WHERE applicant_id=:currentApplicant")
          .setParameter("currentApplicant",applicant.id )
          .findList()
          .stream()
          .collect(ImmutableList.toImmutableList());

      for(ApplicationModel application :  applications){
       database.sqlQuery("DELETE FROM application_events WHERE application_id=:applicationId");
      }


    }

  }
}
