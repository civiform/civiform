package auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static support.CfTestHelpers.requestBuilderWithSettings;
import static support.FakeRequestBuilder.fakeRequest;

import com.google.common.collect.ImmutableList;
import models.AccountModel;
import models.ApplicantModel;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http.Request;
import repository.ResetPostgres;
import services.program.ProgramDefinition;
import support.ProgramBuilder;

public class CiviFormProfileTest extends ResetPostgres {

  private ProfileFactory profileFactory;

  @Before
  public void setupProfileData() {
    profileFactory = instanceOf(ProfileFactory.class);
  }

  @Test
  public void checkAuthorization_admin_failsForApplicantId() {
    CiviFormProfileData data = profileFactory.createNewAdmin();
    CiviFormProfile profile = profileFactory.wrapProfileData(data);

    assertThatThrownBy(() -> profile.checkAuthorization(1234L).join())
        .hasCauseInstanceOf(SecurityException.class);
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
    ApplicantModel one = resourceCreator.insertApplicant();
    ApplicantModel two = resourceCreator.insertApplicant();
    ApplicantModel three = resourceCreator.insertApplicant();
    AccountModel account = resourceCreator.insertAccount();

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

  @Test
  public void checkProgramAuthorization_noPrograms_fails() {
    CiviFormProfileData data = profileFactory.createNewProgramAdmin();
    CiviFormProfile profile = profileFactory.wrapProfileData(data);

    assertThatThrownBy(() -> profile.checkProgramAuthorization("program1", fakeRequest()).join())
        .hasCauseInstanceOf(SecurityException.class);
  }

  @Test
  public void checkProgramAuthorization_differentPrograms_fails() {
    CiviFormProfileData data = profileFactory.createNewProgramAdmin();
    CiviFormProfile profile = profileFactory.wrapProfileData(data);
    ProgramDefinition programOne = ProgramBuilder.newActiveProgram("program1").buildDefinition();
    profile.getAccount().join().addAdministeredProgram(programOne);

    assertThatThrownBy(() -> profile.checkProgramAuthorization("program2", fakeRequest()).join())
        .hasCauseInstanceOf(SecurityException.class);
  }

  @Test
  public void checkProgramAuthorization_success() {
    CiviFormProfileData data = profileFactory.createNewProgramAdmin();
    CiviFormProfile profile = profileFactory.wrapProfileData(data);
    ProgramDefinition programOne = ProgramBuilder.newActiveProgram("program1").buildDefinition();
    profile
        .getAccount()
        .thenAccept(
            account -> {
              account.addAdministeredProgram(programOne);
              account.save();
            })
        .join();

    profile.getAccount().join().addAdministeredProgram(programOne);
    assertThat(profile.checkProgramAuthorization("program1", fakeRequest()).join()).isEqualTo(null);
  }

  @Test
  public void checkProgramAuthorization_CiviformAdmin_fail() {
    CiviFormProfileData data = profileFactory.createNewAdmin();
    CiviFormProfile profile = profileFactory.wrapProfileData(data);
    assertThatThrownBy(() -> profile.checkProgramAuthorization("program1", fakeRequest()).join())
        .hasCauseInstanceOf(SecurityException.class);
  }

  @Test
  public void checkProgramAuthorization_CiviformAdminAllowed_success() {
    CiviFormProfileData data = profileFactory.createNewAdmin();
    CiviFormProfile profile = profileFactory.wrapProfileData(data);
    Request civiformAdminAllowedRequest =
        requestBuilderWithSettings("ALLOW_CIVIFORM_ADMIN_ACCESS_PROGRAMS", "true").build();

    assertThat(profile.checkProgramAuthorization("program1", civiformAdminAllowedRequest).join())
        .isEqualTo(null);
  }
}
