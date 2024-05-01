package auth;

import static org.assertj.core.api.Assertions.assertThat;

import controllers.WithMockedProfiles;
import java.util.HashSet;
import models.AccountModel;
import models.ApplicantModel;
import models.TrustedIntermediaryGroupModel;
import org.junit.Before;
import org.junit.Test;

public class ProgramAclsTest extends WithMockedProfiles {

  private CiviFormProfile tiProfileOne;
  private CiviFormProfile tiProfileTwo;
  private CiviFormProfile applicantProfile;
  private ProfileFactory profileFactory;
  private ProgramAcls programAcls;

  @Before
  public void setUp() throws Exception {
    profileFactory = instanceOf(ProfileFactory.class);
    tiProfileOne = profileFactory.wrapProfileData(profileFactory.createFakeTrustedIntermediary());
    tiProfileTwo = profileFactory.wrapProfileData(profileFactory.createFakeTrustedIntermediary());
    TrustedIntermediaryGroupModel tiGroupTwo =
        new TrustedIntermediaryGroupModel("CBO2", "Food shelter");
    AccountModel tiTwo = tiProfileTwo.getAccount().join();
    tiTwo.setMemberOfGroup(tiGroupTwo);
    tiTwo.save();
    ApplicantModel applicant = createApplicant();
    applicantProfile = profileFactory.wrap(applicant);
    HashSet<Long> tiList = new HashSet<>();
    tiList.add(tiProfileOne.getAccount().get().getMemberOfGroup().get().id);
    programAcls = new ProgramAcls(tiList);
  }

  @Test
  public void hasProgramViewPermission_hasPermission() {
    assertThat(programAcls.hasProgramViewPermission(tiProfileOne)).isTrue();
  }

  @Test
  public void hasProgramViewPermission_doesNotHavePermission() {
    assertThat(programAcls.hasProgramViewPermission(tiProfileTwo)).isFalse();
  }

  @Test
  public void hasProgramViewPermission_doesNotHavePermissionForApplicant() {
    assertThat(programAcls.hasProgramViewPermission(applicantProfile)).isFalse();
  }
}
