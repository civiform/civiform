package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import auth.Role;
import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.CollectorRegistry;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import repository.ResetPostgres;
import services.settings.SettingsManifest;

public class ApplicantRoutesTest extends ResetPostgres {

  private ProfileFactory profileFactory;
  private static long applicantId = 123L;
  private static long applicantAccountId = 456L;
  private static long tiAccountId = 789L;
  private static long programId = 321L;
  private static SettingsManifest mockSettingsManifest = mock(SettingsManifest.class);

  // Class to hold counter values.
  static class Counts {
    double present = 0;
    double absent = 0;
  }

  private Counts getApplicantIdInProfileCounts() {
    Counts counts = new Counts();
    CollectorRegistry registry = CollectorRegistry.defaultRegistry;
    for (MetricFamilySamples mfs : Collections.list(registry.metricFamilySamples())) {
      if (mfs.name.equals("applicant_id_in_profile")) {
        for (MetricFamilySamples.Sample sample : mfs.samples) {
          if (sample.labelValues.contains("present")) {
            counts.present = sample.value;
          } else if (sample.labelValues.contains("absent")) {
            counts.absent = sample.value;
          }
        }
      }
    }
    return counts;
  }

  private void setNewApplicantUrlSchemaEnabled(boolean enabled) {
    when(mockSettingsManifest.getNewApplicantUrlSchemaEnabled()).thenAnswer(invocation -> enabled);
  }

  private void enableNewApplicantUrlSchema() {
    setNewApplicantUrlSchemaEnabled(true);
  }

  private void disableNewApplicantUrlSchema() {
    setNewApplicantUrlSchemaEnabled(false);
  }

  @Before
  public void setup() {
    profileFactory = instanceOf(ProfileFactory.class);
  }

  @Test
  public void testIndexRoute_forApplicantWithIdInProfile_newSchemaEnabled() {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(applicantId));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    assertThat(new ApplicantRoutes(mockSettingsManifest).index(applicantProfile, applicantId).url())
        .isEqualTo("/programs");

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  public void testIndexRoute_forApplicantWithIdInProfile_newSchemaDisabled() {
    disableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(applicantId));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedIndexUrl = String.format("/applicants/%d/programs", applicantId);
    assertThat(new ApplicantRoutes(mockSettingsManifest).index(applicantProfile, applicantId).url())
        .isEqualTo(expectedIndexUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  public void testIndexRoute_forApplicantWithoutIdInProfile() {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedIndexUrl = String.format("/applicants/%d/programs", applicantId);
    assertThat(new ApplicantRoutes(mockSettingsManifest).index(applicantProfile, applicantId).url())
        .isEqualTo(expectedIndexUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testIndexRoute_forTrustedIntermediary() {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(tiAccountId);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedIndexUrl = String.format("/applicants/%d/programs", applicantId);
    assertThat(new ApplicantRoutes(mockSettingsManifest).index(tiProfile, applicantId).url())
        .isEqualTo(expectedIndexUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testShowRoute_forApplicantWithIdInProfile_newSchemaEnabled() {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(applicantId));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedShowUrl = String.format("/programs/%d", programId);
    assertThat(new ApplicantRoutes(mockSettingsManifest).show(applicantProfile, applicantId, programId).url())
        .isEqualTo(expectedShowUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  public void testShowRoute_forApplicantWithIdInProfile_newSchemaDisabled() {
    disableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
      ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(applicantId));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedShowUrl = String.format("/applicants/%d/programs/%d", applicantId, programId);
    assertThat(new ApplicantRoutes(mockSettingsManifest).show(applicantProfile, applicantId, programId).url())
      .isEqualTo(expectedShowUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  public void testShowRoute_forApplicantWithoutIdInProfile() {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedShowUrl = String.format("/applicants/%d/programs/%d", applicantId, programId);
    assertThat(new ApplicantRoutes(mockSettingsManifest).show(applicantProfile, applicantId, programId).url())
        .isEqualTo(expectedShowUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testShowRoute_forTrustedIntermediary() {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(tiAccountId);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedShowUrl = String.format("/applicants/%d/programs/%d", applicantId, programId);
    assertThat(new ApplicantRoutes(mockSettingsManifest).show(tiProfile, applicantId, programId).url())
        .isEqualTo(expectedShowUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }
}
