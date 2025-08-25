package durablejobs.jobs;

import org.junit.Test;
import repository.ResetPostgres;

public class UpdateLastActivityTimeForAccounts extends ResetPostgres{

  @Test
  public void run_LastActivityTimeForAccounts(){
    ApplicantModel applicant= resourceCreator.insertApplicantWithAccount();
    sleep
  }
}
