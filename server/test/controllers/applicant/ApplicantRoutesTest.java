package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;

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

@RunWith(JUnitParamsRunner.class)
public class ApplicantRoutesTest extends ResetPostgres {

  private ProfileFactory profileFactory;
  private static long APPLICANT_ID = 123L;
  private static long APPLICANT_ACCOUNT_ID = 456L;
  private static long TI_ACCOUNT_ID = 789L;
  private static long PROGRAM_ID = 321L;
  private static String BLOCK_ID = "test_block";
  private static final int CURRENT_BLOCK_INDEX = 7;

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

  @Before
  public void setup() {
    profileFactory = instanceOf(ProfileFactory.class);
  }

  @Test
  public void testIndexRoute_forApplicantWithIdInProfile_newSchemaEnabled() {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    assertThat(new ApplicantRoutes().index(applicantProfile, APPLICANT_ID).url())
        .isEqualTo("/programs");

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  public void testIndexRoute_forApplicantWithoutIdInProfile() {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedIndexUrl = String.format("/applicants/%d/programs", APPLICANT_ID);
    assertThat(new ApplicantRoutes().index(applicantProfile, APPLICANT_ID).url())
        .isEqualTo(expectedIndexUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testIndexRoute_forTrustedIntermediary() {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedIndexUrl = String.format("/applicants/%d/programs", APPLICANT_ID);
    assertThat(new ApplicantRoutes().index(tiProfile, APPLICANT_ID).url())
        .isEqualTo(expectedIndexUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testShowRoute_forApplicantWithIdInProfile_newSchemaEnabled() {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedShowUrl = String.format("/programs/%d", PROGRAM_ID);
    assertThat(new ApplicantRoutes().show(applicantProfile, APPLICANT_ID, PROGRAM_ID).url())
        .isEqualTo(expectedShowUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  public void testShowRoute_forApplicantWithoutIdInProfile() {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedShowUrl = String.format("/applicants/%d/programs/%d", APPLICANT_ID, PROGRAM_ID);
    assertThat(new ApplicantRoutes().show(applicantProfile, APPLICANT_ID, PROGRAM_ID).url())
        .isEqualTo(expectedShowUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testShowRoute_forTrustedIntermediary() {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedShowUrl = String.format("/applicants/%d/programs/%d", APPLICANT_ID, PROGRAM_ID);
    assertThat(new ApplicantRoutes().show(tiProfile, APPLICANT_ID, PROGRAM_ID).url())
        .isEqualTo(expectedShowUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testEditRoute_forApplicantWithIdInProfile_newSchemaEnabled() {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedEditUrl = String.format("/programs/%d/edit", PROGRAM_ID);
    assertThat(new ApplicantRoutes().edit(applicantProfile, APPLICANT_ID, PROGRAM_ID).url())
        .isEqualTo(expectedEditUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  public void testEditRoute_forApplicantWithoutIdInProfile() {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedEditUrl =
        String.format("/applicants/%d/programs/%d/edit", APPLICANT_ID, PROGRAM_ID);
    assertThat(new ApplicantRoutes().edit(applicantProfile, APPLICANT_ID, PROGRAM_ID).url())
        .isEqualTo(expectedEditUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testEditRoute_forTrustedIntermediary() {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedEditUrl =
        String.format("/applicants/%d/programs/%d/edit", APPLICANT_ID, PROGRAM_ID);
    assertThat(new ApplicantRoutes().edit(tiProfile, APPLICANT_ID, PROGRAM_ID).url())
        .isEqualTo(expectedEditUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testReviewRoute_forApplicantWithIdInProfile_newSchemaEnabled() {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedReviewUrl = String.format("/programs/%d/review", PROGRAM_ID);
    assertThat(new ApplicantRoutes().review(applicantProfile, APPLICANT_ID, PROGRAM_ID).url())
        .isEqualTo(expectedReviewUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  public void testReviewRoute_forApplicantWithoutIdInProfile() {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedReviewUrl =
        String.format("/applicants/%d/programs/%d/review", APPLICANT_ID, PROGRAM_ID);
    assertThat(new ApplicantRoutes().review(applicantProfile, APPLICANT_ID, PROGRAM_ID).url())
        .isEqualTo(expectedReviewUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testReviewRoute_forTrustedIntermediary() {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedReviewUrl =
        String.format("/applicants/%d/programs/%d/review", APPLICANT_ID, PROGRAM_ID);
    assertThat(new ApplicantRoutes().review(tiProfile, APPLICANT_ID, PROGRAM_ID).url())
        .isEqualTo(expectedReviewUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testSubmitRoute_forApplicantWithIdInProfile_newSchemaEnabled() {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedSubmitUrl = String.format("/programs/%d/submit", PROGRAM_ID);
    assertThat(new ApplicantRoutes().submit(applicantProfile, APPLICANT_ID, PROGRAM_ID).url())
        .isEqualTo(expectedSubmitUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  public void testSubmitRoute_forApplicantWithoutIdInProfile() {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedSubmitUrl =
        String.format("/applicants/%d/programs/%d/submit", APPLICANT_ID, PROGRAM_ID);
    assertThat(new ApplicantRoutes().submit(applicantProfile, APPLICANT_ID, PROGRAM_ID).url())
        .isEqualTo(expectedSubmitUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testSubmitRoute_forTrustedIntermediary() {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedSubmitUrl =
        String.format("/applicants/%d/programs/%d/submit", APPLICANT_ID, PROGRAM_ID);
    assertThat(new ApplicantRoutes().submit(tiProfile, APPLICANT_ID, PROGRAM_ID).url())
        .isEqualTo(expectedSubmitUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testBlockEditRoute_forApplicantWithIdInProfile_newSchemaEnabled() {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedBlockEditUrl =
        String.format("/programs/%d/blocks/%s/edit", PROGRAM_ID, BLOCK_ID);
    assertThat(
            new ApplicantRoutes()
                .blockEdit(applicantProfile, APPLICANT_ID, PROGRAM_ID, BLOCK_ID, Optional.empty())
                .url())
        .isEqualTo(expectedBlockEditUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  public void testBlockEditRoute_forApplicantWithoutIdInProfile() {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedBlockEditUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/edit", APPLICANT_ID, PROGRAM_ID, BLOCK_ID);
    assertThat(
            new ApplicantRoutes()
                .blockEdit(applicantProfile, APPLICANT_ID, PROGRAM_ID, BLOCK_ID, Optional.empty())
                .url())
        .isEqualTo(expectedBlockEditUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testBlockEditRoute_forTrustedIntermediary() {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedBlockEditUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/edit", APPLICANT_ID, PROGRAM_ID, BLOCK_ID);
    assertThat(
            new ApplicantRoutes()
                .blockEdit(tiProfile, APPLICANT_ID, PROGRAM_ID, BLOCK_ID, Optional.empty())
                .url())
        .isEqualTo(expectedBlockEditUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testBlockReviewRoute_forApplicantWithIdInProfile_newSchemaEnabled() {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedBlockReviewUrl =
        String.format("/programs/%d/blocks/%s/review", PROGRAM_ID, BLOCK_ID);
    assertThat(
            new ApplicantRoutes()
                .blockReview(applicantProfile, APPLICANT_ID, PROGRAM_ID, BLOCK_ID, Optional.empty())
                .url())
        .isEqualTo(expectedBlockReviewUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  public void testBlockReviewRoute_forApplicantWithoutIdInProfile() {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedBlockReviewUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/review", APPLICANT_ID, PROGRAM_ID, BLOCK_ID);
    assertThat(
            new ApplicantRoutes()
                .blockReview(applicantProfile, APPLICANT_ID, PROGRAM_ID, BLOCK_ID, Optional.empty())
                .url())
        .isEqualTo(expectedBlockReviewUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testBlockReviewRoute_forTrustedIntermediary() {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedBlockReviewUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/review", APPLICANT_ID, PROGRAM_ID, BLOCK_ID);
    assertThat(
            new ApplicantRoutes()
                .blockReview(tiProfile, APPLICANT_ID, PROGRAM_ID, BLOCK_ID, Optional.empty())
                .url())
        .isEqualTo(expectedBlockReviewUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  @Parameters({
    "true, REVIEW_PAGE",
    "false, PREVIOUS_BLOCK",
    "true, NEXT_BLOCK",
    "false, NEXT_BLOCK",
  })
  public void testConfirmAddressRoute_forApplicantWithIdInProfile_newSchemaEnabled(
      String inReview, String applicantRequestedAction) {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedConfirmAddressUrl =
        String.format(
            "/programs/%d/blocks/%s/confirmAddress/%s/%s",
            PROGRAM_ID, BLOCK_ID, inReview, applicantRequestedAction);
    assertThat(
            new ApplicantRoutes()
                .confirmAddress(
                    applicantProfile,
                    APPLICANT_ID,
                    PROGRAM_ID,
                    BLOCK_ID,
                    Boolean.valueOf(inReview),
                    ApplicantRequestedAction.valueOf(applicantRequestedAction))
                .url())
        .isEqualTo(expectedConfirmAddressUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  @Parameters({
    "true, PREVIOUS_BLOCK",
    "false, REVIEW_PAGE",
    "true, NEXT_BLOCK",
    "false, NEXT_BLOCK",
  })
  public void testConfirmAddressRoute_forApplicantWithoutIdInProfile(
      String inReview, String applicantRequestedAction) {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedConfirmAddressUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/confirmAddress/%s/%s",
            APPLICANT_ID, PROGRAM_ID, BLOCK_ID, inReview, applicantRequestedAction);
    assertThat(
            new ApplicantRoutes()
                .confirmAddress(
                    applicantProfile,
                    APPLICANT_ID,
                    PROGRAM_ID,
                    BLOCK_ID,
                    Boolean.valueOf(inReview),
                    ApplicantRequestedAction.valueOf(applicantRequestedAction))
                .url())
        .isEqualTo(expectedConfirmAddressUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  @Parameters({
    "true, PREVIOUS_BLOCK",
    "false, REVIEW_PAGE",
    "true, NEXT_BLOCK",
    "false, NEXT_BLOCK",
  })
  public void testConfirmAddressRoute_forTrustedIntermediary(
      String inReview, String applicantRequestedAction) {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedConfirmAddressUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/confirmAddress/%s/%s",
            APPLICANT_ID, PROGRAM_ID, BLOCK_ID, inReview, applicantRequestedAction);
    assertThat(
            new ApplicantRoutes()
                .confirmAddress(
                    tiProfile,
                    APPLICANT_ID,
                    PROGRAM_ID,
                    BLOCK_ID,
                    Boolean.valueOf(inReview),
                    ApplicantRequestedAction.valueOf(applicantRequestedAction))
                .url())
        .isEqualTo(expectedConfirmAddressUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  @Parameters({"true", "false"})
  public void testPreviousOrReviewRoute_forApplicantWithIdInProfile_newSchemaEnabled(
      String inReview) {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedPreviousUrl =
        String.format(
            "/programs/%d/blocks/%d/previous/%s", PROGRAM_ID, CURRENT_BLOCK_INDEX - 1, inReview);
    assertThat(
            new ApplicantRoutes()
                .blockPreviousOrReview(
                    applicantProfile,
                    APPLICANT_ID,
                    PROGRAM_ID,
                    CURRENT_BLOCK_INDEX,
                    Boolean.valueOf(inReview))
                .url())
        .isEqualTo(expectedPreviousUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  @Parameters({"true", "false"})
  public void testPreviousOrReviewRoute_forApplicantWithoutIdInProfile(String inReview) {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedPreviousUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%d/previous/%s",
            APPLICANT_ID, PROGRAM_ID, CURRENT_BLOCK_INDEX - 1, inReview);
    assertThat(
            new ApplicantRoutes()
                .blockPreviousOrReview(
                    applicantProfile,
                    APPLICANT_ID,
                    PROGRAM_ID,
                    CURRENT_BLOCK_INDEX,
                    Boolean.valueOf(inReview))
                .url())
        .isEqualTo(expectedPreviousUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  @Parameters({"true", "false"})
  public void testPreviousOrReviewRoute_forTrustedIntermediary(String inReview) {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedPreviousUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%d/previous/%s",
            APPLICANT_ID, PROGRAM_ID, CURRENT_BLOCK_INDEX - 1, inReview);
    assertThat(
            new ApplicantRoutes()
                .blockPreviousOrReview(
                    tiProfile,
                    APPLICANT_ID,
                    PROGRAM_ID,
                    CURRENT_BLOCK_INDEX,
                    Boolean.valueOf(inReview))
                .url())
        .isEqualTo(expectedPreviousUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  public void testPreviousOrReviewRoute_currentBlockIndexOne_returnsPreviousBlock() {
    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedPreviousUrl =
        String.format("/programs/%d/blocks/%d/previous/%s", PROGRAM_ID, 0, false);
    assertThat(
            new ApplicantRoutes()
                .blockPreviousOrReview(
                    applicantProfile,
                    APPLICANT_ID,
                    PROGRAM_ID,
                    /* currentBlockIndex= */ 1,
                    /* inReview= */ false)
                .url())
        .isEqualTo(expectedPreviousUrl);
  }

  @Test
  public void testPreviousOrReviewRoute_currentBlockIndexZero_returnsReviewUrl() {
    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedReviewUrl = String.format("/programs/%d/review", PROGRAM_ID);

    assertThat(
            new ApplicantRoutes()
                .blockPreviousOrReview(
                    applicantProfile,
                    APPLICANT_ID,
                    PROGRAM_ID,
                    /* currentBlockIndex= */ 0,
                    /* inReview= */ false)
                .url())
        .isEqualTo(expectedReviewUrl);
  }

  @Test
  public void testPreviousOrReviewRoute_currentBlockIndexNegativeOne_returnsReviewUrl() {
    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedReviewUrl = String.format("/programs/%d/review", PROGRAM_ID);

    assertThat(
            new ApplicantRoutes()
                .blockPreviousOrReview(
                    applicantProfile,
                    APPLICANT_ID,
                    PROGRAM_ID,
                    /* currentBlockIndex= */ -1,
                    /* inReview= */ false)
                .url())
        .isEqualTo(expectedReviewUrl);
  }

  @Test
  @Parameters({"true", "false"})
  public void testUpdateFileRoute_forApplicantWithIdInProfile_newSchemaEnabled(String inReview) {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedUpdateFileUrl =
        String.format("/programs/%d/blocks/%s/updateFile/%s", PROGRAM_ID, BLOCK_ID, inReview);
    assertThat(
            new ApplicantRoutes()
                .updateFile(
                    applicantProfile, APPLICANT_ID, PROGRAM_ID, BLOCK_ID, Boolean.valueOf(inReview))
                .url())
        .isEqualTo(expectedUpdateFileUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  @Parameters({"true", "false"})
  public void testUpdateFileRoute_forApplicantWithoutIdInProfile(String inReview) {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedUpdateFileUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/updateFile/%s",
            APPLICANT_ID, PROGRAM_ID, BLOCK_ID, inReview);
    assertThat(
            new ApplicantRoutes()
                .updateFile(
                    applicantProfile, APPLICANT_ID, PROGRAM_ID, BLOCK_ID, Boolean.valueOf(inReview))
                .url())
        .isEqualTo(expectedUpdateFileUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  @Parameters({"true", "false"})
  public void testUpdateFileRoute_forTrustedIntermediary(String inReview) {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedUpdateFileUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/updateFile/%s",
            APPLICANT_ID, PROGRAM_ID, BLOCK_ID, inReview);
    assertThat(
            new ApplicantRoutes()
                .updateFile(
                    tiProfile, APPLICANT_ID, PROGRAM_ID, BLOCK_ID, Boolean.valueOf(inReview))
                .url())
        .isEqualTo(expectedUpdateFileUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  @Parameters({"true, REVIEW_PAGE", "false, REVIEW_PAGE", "true, NEXT_BLOCK", "false, NEXT_BLOCK"})
  public void testUpdateBlockRoute_forApplicantWithIdInProfile_newSchemaEnabled(
      String inReview, String applicantRequestedAction) {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedUpdateBlockUrl =
        String.format(
            "/programs/%d/blocks/%s/%s/%s",
            PROGRAM_ID, BLOCK_ID, inReview, applicantRequestedAction);
    assertThat(
            new ApplicantRoutes()
                .updateBlock(
                    applicantProfile,
                    APPLICANT_ID,
                    PROGRAM_ID,
                    BLOCK_ID,
                    Boolean.valueOf(inReview),
                    ApplicantRequestedAction.valueOf(applicantRequestedAction))
                .url())
        .isEqualTo(expectedUpdateBlockUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present + 1);
    assertThat(after.absent).isEqualTo(before.absent);
  }

  @Test
  @Parameters({"true, REVIEW_PAGE", "false, REVIEW_PAGE", "true, NEXT_BLOCK", "false, NEXT_BLOCK"})
  public void testUpdateBlockRoute_forApplicantWithoutIdInProfile(
      String inReview, String applicantRequestedAction) {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.removeAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME);
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedUpdateBlockUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/%s/%s",
            APPLICANT_ID, PROGRAM_ID, BLOCK_ID, inReview, applicantRequestedAction);
    assertThat(
            new ApplicantRoutes()
                .updateBlock(
                    applicantProfile,
                    APPLICANT_ID,
                    PROGRAM_ID,
                    BLOCK_ID,
                    Boolean.valueOf(inReview),
                    ApplicantRequestedAction.valueOf(applicantRequestedAction))
                .url())
        .isEqualTo(expectedUpdateBlockUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }

  @Test
  @Parameters({"true, REVIEW_PAGE", "false, REVIEW_PAGE", "true, NEXT_BLOCK", "false, NEXT_BLOCK"})
  public void testUpdateBlockRoute_forTrustedIntermediary(
      String inReview, String applicantRequestedAction) {
    Counts before = getApplicantIdInProfileCounts();

    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedUpdateBlockUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/%s/%s",
            APPLICANT_ID, PROGRAM_ID, BLOCK_ID, inReview, applicantRequestedAction);
    assertThat(
            new ApplicantRoutes()
                .updateBlock(
                    tiProfile,
                    APPLICANT_ID,
                    PROGRAM_ID,
                    BLOCK_ID,
                    Boolean.valueOf(inReview),
                    ApplicantRequestedAction.valueOf(applicantRequestedAction))
                .url())
        .isEqualTo(expectedUpdateBlockUrl);

    Counts after = getApplicantIdInProfileCounts();
    assertThat(after.present).isEqualTo(before.present);
    assertThat(after.absent).isEqualTo(before.absent + 1);
  }
}
