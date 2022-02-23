package auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import com.google.common.collect.ImmutableList;
import java.util.concurrent.CompletionException;
import models.Account;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import repository.WithPostgresContainer;

public class CiviFormProfileTest extends WithPostgresContainer {

  private ProfileFactory profileFactory;

  @Before
  public void setupProfileData() {
    profileFactory = instanceOf(ProfileFactory.class);
  }

  @Test
  public void checkAuthorization_admin_failsForApplicantId() {
    CiviFormProfileData data = profileFactory.createNewAdmin();
    CiviFormProfile profile = profileFactory.wrapProfileData(data);

    try {
      profile.checkAuthorization(1234L).join();
      fail("Should not have successfully authorized admin.");
    } catch (CompletionException e) {
      // pass.
    }
  }

  @Test
  public void checkAuthorization_applicant_passesForOwnId() throws Exception {
    CiviFormProfileData data = profileFactory.createNewApplicant();
    CiviFormProfile profile = profileFactory.wrapProfileData(data);

    profile.checkAuthorization(profile.getApplicant().get().id).join();
  }

  @Test
  public void checkAuthorization_passesForOneOfSeveralIdsInAccount() {
    // We need to save these first so that the IDs are populated.
    Applicant one = resourceCreator.insertApplicant();
    Applicant two = resourceCreator.insertApplicant();
    Applicant three = resourceCreator.insertApplicant();
    Account account = resourceCreator.insertAccount();

    // Set the accounts on applicants and the applicants on the account. Saving required!
    one.setAccount(account);
    one.save();
    two.setAccount(account);
    two.save();
    three.setAccount(account);
    three.save();
    account.setApplicants(ImmutableList.of(one, two, three));
    account.save();

    CiviFormProfile profile = profileFactory.wrap(account);

    profile.checkAuthorization(two.id).join();
  }

  @Test
  public void checkAuthorization_fails() {
    CiviFormProfileData data = profileFactory.createNewApplicant();
    CiviFormProfile profile = profileFactory.wrapProfileData(data);

    assertThatThrownBy(() -> profile.checkAuthorization(1234L).join())
        .hasCauseInstanceOf(SecurityException.class);
  }
}
