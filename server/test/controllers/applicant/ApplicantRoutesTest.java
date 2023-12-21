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
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import repository.ResetPostgres;
import services.settings.SettingsManifest;

@RunWith(JUnitParamsRunner.class)
public class ApplicantRoutesTest extends ResetPostgres {

  private ProfileFactory profileFactory;
  private static long applicantId = 123L;
  private static long applicantAccountId = 456L;
  private static long tiAccountId = 789L;
  private static long programId = 321L;
  private static String blockId = "test_block";
  private static int previousBlockIndex = 7;
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
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .show(applicantProfile, applicantId, programId)
                .url())
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
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .show(applicantProfile, applicantId, programId)
                .url())
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
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .show(applicantProfile, applicantId, programId)
                .url())
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
    assertThat(
            new ApplicantRoutes(mockSettingsManifest).show(tiProfile, applicantId, programId).url())
        .isEqualTo(expectedShowUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testEditRoute_forApplicantWithIdInProfile_newSchemaEnabled() {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(applicantId));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedEditUrl = String.format("/programs/%d/edit", programId);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .edit(applicantProfile, applicantId, programId)
                .url())
        .isEqualTo(expectedEditUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  public void testEditRoute_forApplicantWithIdInProfile_newSchemaDisabled() {
    disableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(applicantId));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedEditUrl =
        String.format("/applicants/%d/programs/%d/edit", applicantId, programId);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .edit(applicantProfile, applicantId, programId)
                .url())
        .isEqualTo(expectedEditUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  public void testEditRoute_forApplicantWithoutIdInProfile() {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedEditUrl =
        String.format("/applicants/%d/programs/%d/edit", applicantId, programId);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .edit(applicantProfile, applicantId, programId)
                .url())
        .isEqualTo(expectedEditUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testEditRoute_forTrustedIntermediary() {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(tiAccountId);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedEditUrl =
        String.format("/applicants/%d/programs/%d/edit", applicantId, programId);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest).edit(tiProfile, applicantId, programId).url())
        .isEqualTo(expectedEditUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testReviewRoute_forApplicantWithIdInProfile_newSchemaEnabled() {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(applicantId));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedReviewUrl = String.format("/programs/%d/review", programId);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .review(applicantProfile, applicantId, programId)
                .url())
        .isEqualTo(expectedReviewUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  public void testReviewRoute_forApplicantWithIdInProfile_newSchemaDisabled() {
    disableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(applicantId));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedReviewUrl =
        String.format("/applicants/%d/programs/%d/review", applicantId, programId);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .review(applicantProfile, applicantId, programId)
                .url())
        .isEqualTo(expectedReviewUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  public void testReviewRoute_forApplicantWithoutIdInProfile() {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedReviewUrl =
        String.format("/applicants/%d/programs/%d/review", applicantId, programId);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .review(applicantProfile, applicantId, programId)
                .url())
        .isEqualTo(expectedReviewUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testReviewRoute_forTrustedIntermediary() {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(tiAccountId);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedReviewUrl =
        String.format("/applicants/%d/programs/%d/review", applicantId, programId);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .review(tiProfile, applicantId, programId)
                .url())
        .isEqualTo(expectedReviewUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testSubmitRoute_forApplicantWithIdInProfile_newSchemaEnabled() {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(applicantId));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedSubmitUrl = String.format("/programs/%d/submit", programId);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .submit(applicantProfile, applicantId, programId)
                .url())
        .isEqualTo(expectedSubmitUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  public void testSubmitRoute_forApplicantWithIdInProfile_newSchemaDisabled() {
    disableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(applicantId));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedSubmitUrl =
        String.format("/applicants/%d/programs/%d/submit", applicantId, programId);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .submit(applicantProfile, applicantId, programId)
                .url())
        .isEqualTo(expectedSubmitUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  public void testSubmitRoute_forApplicantWithoutIdInProfile() {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedSubmitUrl =
        String.format("/applicants/%d/programs/%d/submit", applicantId, programId);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .submit(applicantProfile, applicantId, programId)
                .url())
        .isEqualTo(expectedSubmitUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testSubmitRoute_forTrustedIntermediary() {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(tiAccountId);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedSubmitUrl =
        String.format("/applicants/%d/programs/%d/submit", applicantId, programId);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .submit(tiProfile, applicantId, programId)
                .url())
        .isEqualTo(expectedSubmitUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testBlockEditRoute_forApplicantWithIdInProfile_newSchemaEnabled() {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(applicantId));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedBlockEditUrl = String.format("/programs/%d/blocks/%s/edit", programId, blockId);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .blockEdit(applicantProfile, applicantId, programId, blockId, Optional.empty())
                .url())
        .isEqualTo(expectedBlockEditUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  public void testBlockEditRoute_forApplicantWithIdInProfile_newSchemaDisabled() {
    disableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(applicantId));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedBlockEditUrl =
        String.format("/applicants/%d/programs/%d/blocks/%s/edit", applicantId, programId, blockId);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .blockEdit(applicantProfile, applicantId, programId, blockId, Optional.empty())
                .url())
        .isEqualTo(expectedBlockEditUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  public void testBlockEditRoute_forApplicantWithoutIdInProfile() {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedBlockEditUrl =
        String.format("/applicants/%d/programs/%d/blocks/%s/edit", applicantId, programId, blockId);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .blockEdit(applicantProfile, applicantId, programId, blockId, Optional.empty())
                .url())
        .isEqualTo(expectedBlockEditUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testBlockEditRoute_forTrustedIntermediary() {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(tiAccountId);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedBlockEditUrl =
        String.format("/applicants/%d/programs/%d/blocks/%s/edit", applicantId, programId, blockId);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .blockEdit(tiProfile, applicantId, programId, blockId, Optional.empty())
                .url())
        .isEqualTo(expectedBlockEditUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testBlockReviewRoute_forApplicantWithIdInProfile_newSchemaEnabled() {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(applicantId));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedBlockReviewUrl =
        String.format("/programs/%d/blocks/%s/review", programId, blockId);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .blockReview(applicantProfile, applicantId, programId, blockId, Optional.empty())
                .url())
        .isEqualTo(expectedBlockReviewUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  public void testBlockReviewRoute_forApplicantWithIdInProfile_newSchemaDisabled() {
    disableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(applicantId));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedBlockReviewUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/review", applicantId, programId, blockId);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .blockReview(applicantProfile, applicantId, programId, blockId, Optional.empty())
                .url())
        .isEqualTo(expectedBlockReviewUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  public void testBlockReviewRoute_forApplicantWithoutIdInProfile() {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedBlockReviewUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/review", applicantId, programId, blockId);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .blockReview(applicantProfile, applicantId, programId, blockId, Optional.empty())
                .url())
        .isEqualTo(expectedBlockReviewUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testBlockReviewRoute_forTrustedIntermediary() {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(tiAccountId);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedBlockReviewUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/review", applicantId, programId, blockId);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .blockReview(tiProfile, applicantId, programId, blockId, Optional.empty())
                .url())
        .isEqualTo(expectedBlockReviewUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  @Parameters({"true", "false"})
  public void testConfirmAddressRoute_forApplicantWithIdInProfile_newSchemaEnabled(
      String inReview) {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(applicantId));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedConfirmAddressUrl =
        String.format("/programs/%d/blocks/%s/confirmAddress/%s", programId, blockId, inReview);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .confirmAddress(
                    applicantProfile, applicantId, programId, blockId, Boolean.valueOf(inReview))
                .url())
        .isEqualTo(expectedConfirmAddressUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  @Parameters({"true", "false"})
  public void testConfirmAddressRoute_forApplicantWithIdInProfile_newSchemaDisabled(
      String inReview) {
    disableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(applicantId));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedConfirmAddressUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/confirmAddress/%s",
            applicantId, programId, blockId, inReview);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .confirmAddress(
                    applicantProfile, applicantId, programId, blockId, Boolean.valueOf(inReview))
                .url())
        .isEqualTo(expectedConfirmAddressUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  @Parameters({"true", "false"})
  public void testConfirmAddressRoute_forApplicantWithoutIdInProfile(String inReview) {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedConfirmAddressUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/confirmAddress/%s",
            applicantId, programId, blockId, inReview);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .confirmAddress(
                    applicantProfile, applicantId, programId, blockId, Boolean.valueOf(inReview))
                .url())
        .isEqualTo(expectedConfirmAddressUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  @Parameters({"true", "false"})
  public void testConfirmAddressRoute_forTrustedIntermediary(String inReview) {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(tiAccountId);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedConfirmAddressUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/confirmAddress/%s",
            applicantId, programId, blockId, inReview);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .confirmAddress(
                    tiProfile, applicantId, programId, blockId, Boolean.valueOf(inReview))
                .url())
        .isEqualTo(expectedConfirmAddressUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  @Parameters({"true", "false"})
  public void testPreviousRoute_forApplicantWithIdInProfile_newSchemaEnabled(String inReview) {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(applicantId));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedPreviousUrl =
        String.format(
            "/programs/%d/blocks/%d/previous/%s", programId, previousBlockIndex, inReview);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .blockPrevious(
                    applicantProfile,
                    applicantId,
                    programId,
                    previousBlockIndex,
                    Boolean.valueOf(inReview))
                .url())
        .isEqualTo(expectedPreviousUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  @Parameters({"true", "false"})
  public void testPreviousRoute_forApplicantWithIdInProfile_newSchemaDisabled(String inReview) {
    disableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(applicantId));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedPreviousUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%d/previous/%s",
            applicantId, programId, previousBlockIndex, inReview);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .blockPrevious(
                    applicantProfile,
                    applicantId,
                    programId,
                    previousBlockIndex,
                    Boolean.valueOf(inReview))
                .url())
        .isEqualTo(expectedPreviousUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  @Parameters({"true", "false"})
  public void testPreviousRoute_forApplicantWithoutIdInProfile(String inReview) {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedPreviousUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%d/previous/%s",
            applicantId, programId, previousBlockIndex, inReview);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .blockPrevious(
                    applicantProfile,
                    applicantId,
                    programId,
                    previousBlockIndex,
                    Boolean.valueOf(inReview))
                .url())
        .isEqualTo(expectedPreviousUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  @Parameters({"true", "false"})
  public void testPreviousRoute_forTrustedIntermediary(String inReview) {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(tiAccountId);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedPreviousUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%d/previous/%s",
            applicantId, programId, previousBlockIndex, inReview);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .blockPrevious(
                    tiProfile,
                    applicantId,
                    programId,
                    previousBlockIndex,
                    Boolean.valueOf(inReview))
                .url())
        .isEqualTo(expectedPreviousUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  @Parameters({"true", "false"})
  public void testUpdateFileRoute_forApplicantWithIdInProfile_newSchemaEnabled(String inReview) {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(applicantId));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedUpdateFileUrl =
        String.format("/programs/%d/blocks/%s/updateFile/%s", programId, blockId, inReview);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .updateFile(
                    applicantProfile, applicantId, programId, blockId, Boolean.valueOf(inReview))
                .url())
        .isEqualTo(expectedUpdateFileUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  @Parameters({"true", "false"})
  public void testUpdateFileRoute_forApplicantWithIdInProfile_newSchemaDisabled(String inReview) {
    disableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(applicantId));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedUpdateFileUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/updateFile/%s",
            applicantId, programId, blockId, inReview);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .updateFile(
                    applicantProfile, applicantId, programId, blockId, Boolean.valueOf(inReview))
                .url())
        .isEqualTo(expectedUpdateFileUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  @Parameters({"true", "false"})
  public void testUpdateFileRoute_forApplicantWithoutIdInProfile(String inReview) {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(applicantAccountId);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedUpdateFileUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/updateFile/%s",
            applicantId, programId, blockId, inReview);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .updateFile(
                    applicantProfile, applicantId, programId, blockId, Boolean.valueOf(inReview))
                .url())
        .isEqualTo(expectedUpdateFileUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  @Parameters({"true", "false"})
  public void testUpdateFileRoute_forTrustedIntermediary(String inReview) {
    enableNewApplicantUrlSchema();
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(tiAccountId);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedUpdateFileUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/updateFile/%s",
            applicantId, programId, blockId, inReview);
    assertThat(
            new ApplicantRoutes(mockSettingsManifest)
                .updateFile(tiProfile, applicantId, programId, blockId, Boolean.valueOf(inReview))
                .url())
        .isEqualTo(expectedUpdateFileUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }
}
