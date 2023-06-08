package auth;

import static org.assertj.core.api.Assertions.assertThat;

import controllers.WithMockedProfiles;
import java.util.HashSet;
import models.Account;
import models.Applicant;
import models.TrustedIntermediaryGroup;
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
    TrustedIntermediaryGroup tiGroupTwo = new TrustedIntermediaryGroup("CBO2", "Food shelter");
    Account tiTwo = tiProfileTwo.getAccount().join();
    tiTwo.setMemberOfGroup(tiGroupTwo);
    tiTwo.save();
    Applicant applicant = createApplicant();
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
