package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import auth.Role;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;

public class ApplicantRoutesTest extends ResetPostgres {

  private ProfileFactory profileFactory;
  private static long applicantId = 123L;
  private static long applicantAccountId = 456L;
  private static long tiAccountId = 789L;

  @Before
  public void setupProfileFactory() {
    profileFactory = instanceOf(ProfileFactory.class);
  }

  @Test
  public void testIndexRouteForApplicantWithIdInProfile() {
    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(applicantId));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    assertThat(ApplicantRoutes.index(applicantProfile, applicantId).url()).isEqualTo("/programs");
  }

  @Test
  public void testIndexRouteForApplicantWithoutIdInProfile() {
    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedIndexUrl = String.format("/applicants/%d/programs", applicantId);
    assertThat(ApplicantRoutes.index(applicantProfile, applicantId).url())
        .isEqualTo(expectedIndexUrl);
  }

  @Test
  public void testIndexRouteForTrustedIntermediary() {
    CiviFormProfileData profileData = new CiviFormProfileData(tiAccountId);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedIndexUrl = String.format("/applicants/%d/programs", applicantId);
    assertThat(ApplicantRoutes.index(tiProfile, applicantId).url()).isEqualTo(expectedIndexUrl);
  }
}
