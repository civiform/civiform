package repository;

import io.ebean.DB;
import io.ebean.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.settings.SettingsManifest;

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
    List<Long> applicant_ids = database
      .sqlQuery(  "SELECT id FROM applicants WHERE account_id=:currentAccount")
      .setParameter("currentAccount",account.id)
      .find(ApplicantModal.class)
      .findList()
      .stream()
      .map(sqlRow -> sqlRow.getLong("id"))
      .collect(ArrayList.toList());

    for(Long appId :  applicant_ids){
      List<Long> applicationIds =
        database
          .sqlQuery(  "SELECT id FROM applications WHERE applicant_id=:currentApplicant")
          .setParameter("currentApplicant",applicantId )
          .find(ApplicationModel.class)
          .findList()
          .stream()
          .map(sqlRow -> sqlRow.getLong("id"))
          .collect(ArrayList.toList());

    }

  }
}
