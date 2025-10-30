package controllers.applicant;

import static org.assertj.core.api.Assertions.assertThat;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import auth.Role;
import java.time.Clock;
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
  private static final long APPLICANT_ID = 123L;
  private static final long APPLICANT_ACCOUNT_ID = 456L;
  private static final long TI_ACCOUNT_ID = 789L;
  private static final long PROGRAM_ID = 321L;
  private static final String PROGRAM_SLUG = "test-program";
  private static final String BLOCK_ID = "test_block";
  private static final int CURRENT_BLOCK_INDEX = 7;
  private Clock clock;

  // Class to hold counter values.
  static class Counts {
    double present = 0;
    double absent = 0;
  }

  @Before
  public void setup() {
    profileFactory = instanceOf(ProfileFactory.class);
    clock = Clock.systemUTC();
  }

  @Test
  public void testIndexRoute_forApplicantWithIdInProfile_newSchemaEnabled() {
    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    assertThat(new ApplicantRoutes().index(applicantProfile, APPLICANT_ID).url())
        .isEqualTo("/programs");
  }

  @Test
  public void testIndexRoute_forTrustedIntermediary() {
    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedIndexUrl = String.format("/applicants/%d/programs", APPLICANT_ID);
    assertThat(new ApplicantRoutes().index(tiProfile, APPLICANT_ID).url())
        .isEqualTo(expectedIndexUrl);
  }

  @Test
  public void testShowRoute_withoutApplicant() {
    String expectedShowUrl = String.format("/programs/%d", PROGRAM_ID);
    assertThat(new ApplicantRoutes().show(PROGRAM_ID).url()).isEqualTo(expectedShowUrl);
  }

  @Test
  public void testShowRoute_withoutApplicantWithProgramSlug() {
    String expectedShowUrl = String.format("/programs/%s", PROGRAM_SLUG);
    assertThat(new ApplicantRoutes().show(PROGRAM_SLUG).url()).isEqualTo(expectedShowUrl);
  }

  @Test
  public void testShowRoute_forTrustedIntermediaryWithProgramSlug() {
    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedEditUrl =
        String.format("/applicants/%d/programs/%s", APPLICANT_ID, PROGRAM_SLUG);
    assertThat(new ApplicantRoutes().show(tiProfile, APPLICANT_ID, PROGRAM_SLUG).url())
        .isEqualTo(expectedEditUrl);
  }

  @Test
  public void testEditRoute_forApplicantWithIdInProfile_newSchemaEnabled() {
    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedEditUrl = String.format("/programs/%d/edit?isFromUrlCall=false", PROGRAM_ID);
    assertThat(new ApplicantRoutes().edit(applicantProfile, APPLICANT_ID, PROGRAM_ID).url())
        .isEqualTo(expectedEditUrl);
  }

  @Test
  public void testEditRoute_forTrustedIntermediary() {
    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedEditUrl =
        String.format(
            "/applicants/%d/programs/%d/edit?isFromUrlCall=false", APPLICANT_ID, PROGRAM_ID);
    assertThat(new ApplicantRoutes().edit(tiProfile, APPLICANT_ID, PROGRAM_ID).url())
        .isEqualTo(expectedEditUrl);
  }

  @Test
  public void testReviewRoute_forApplicantWithIdInProfile_newSchemaEnabled() {
    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedReviewUrl = String.format("/programs/%d/review?isFromUrlCall=false", PROGRAM_ID);
    assertThat(new ApplicantRoutes().review(applicantProfile, APPLICANT_ID, PROGRAM_ID).url())
        .isEqualTo(expectedReviewUrl);
  }

  @Test
  public void testReviewRoute_forTrustedIntermediary() {
    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedReviewUrl =
        String.format(
            "/applicants/%d/programs/%d/review?isFromUrlCall=false", APPLICANT_ID, PROGRAM_ID);
    assertThat(new ApplicantRoutes().review(tiProfile, APPLICANT_ID, PROGRAM_ID).url())
        .isEqualTo(expectedReviewUrl);
  }

  @Test
  public void testReviewRoute_withoutApplicant() {
    String expectedShowUrl = String.format("/programs/%d/review?isFromUrlCall=false", PROGRAM_ID);
    assertThat(new ApplicantRoutes().review(PROGRAM_ID).url()).isEqualTo(expectedShowUrl);
  }

  @Test
  public void testSubmitRoute_forApplicantWithIdInProfile_newSchemaEnabled() {
    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedSubmitUrl = String.format("/programs/%d/submit", PROGRAM_ID);
    assertThat(new ApplicantRoutes().submit(applicantProfile, APPLICANT_ID, PROGRAM_ID).url())
        .isEqualTo(expectedSubmitUrl);
  }

  @Test
  public void testSubmitRoute_forTrustedIntermediary() {
    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedSubmitUrl =
        String.format("/applicants/%d/programs/%d/submit", APPLICANT_ID, PROGRAM_ID);
    assertThat(new ApplicantRoutes().submit(tiProfile, APPLICANT_ID, PROGRAM_ID).url())
        .isEqualTo(expectedSubmitUrl);
  }

  @Test
  public void testBlockEditRoute_forApplicantWithIdInProfile_newSchemaEnabled() {
    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedBlockEditUrl =
        String.format("/programs/%d/blocks/%s/edit?isFromUrlCall=false", PROGRAM_ID, BLOCK_ID);
    assertThat(
            new ApplicantRoutes()
                .blockEdit(applicantProfile, APPLICANT_ID, PROGRAM_ID, BLOCK_ID, Optional.empty())
                .url())
        .isEqualTo(expectedBlockEditUrl);
  }

  @Test
  public void testBlockEditRoute_forTrustedIntermediary() {
    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedBlockEditUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/edit?isFromUrlCall=false",
            APPLICANT_ID, PROGRAM_ID, BLOCK_ID);
    assertThat(
            new ApplicantRoutes()
                .blockEdit(tiProfile, APPLICANT_ID, PROGRAM_ID, BLOCK_ID, Optional.empty())
                .url())
        .isEqualTo(expectedBlockEditUrl);
  }

  @Test
  public void testBlockReviewRoute_forApplicantWithIdInProfile_newSchemaEnabled() {
    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedBlockReviewUrl =
        String.format("/programs/%d/blocks/%s/review?isFromUrlCall=false", PROGRAM_ID, BLOCK_ID);
    assertThat(
            new ApplicantRoutes()
                .blockReview(applicantProfile, APPLICANT_ID, PROGRAM_ID, BLOCK_ID, Optional.empty())
                .url())
        .isEqualTo(expectedBlockReviewUrl);
  }

  @Test
  public void testBlockReviewRoute_forTrustedIntermediary() {
    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedBlockReviewUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/review?isFromUrlCall=false",
            APPLICANT_ID, PROGRAM_ID, BLOCK_ID);
    assertThat(
            new ApplicantRoutes()
                .blockReview(tiProfile, APPLICANT_ID, PROGRAM_ID, BLOCK_ID, Optional.empty())
                .url())
        .isEqualTo(expectedBlockReviewUrl);
  }

  @Test
  @Parameters({"true", "false"})
  public void testBlockEditOrBlockReviewRoute_forApplicantWithIdInProfile_newSchemaEnabled(
      String inReview) {
    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    boolean inReviewBoolean = Boolean.parseBoolean(inReview);
    String expectedUrl =
        String.format(
            "/programs/%d/blocks/%s/%s?isFromUrlCall=false",
            PROGRAM_ID, BLOCK_ID, inReviewBoolean ? "review" : "edit");
    assertThat(
            new ApplicantRoutes()
                .blockEditOrBlockReview(
                    applicantProfile, APPLICANT_ID, PROGRAM_ID, BLOCK_ID, inReviewBoolean)
                .url())
        .isEqualTo(expectedUrl);
  }

  @Test
  @Parameters({"true", "false"})
  public void testBlockReviewRoute_forTrustedIntermediary(String inReview) {
    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    boolean inReviewBoolean = Boolean.parseBoolean(inReview);
    String expectedBlockReviewUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/%s?isFromUrlCall=false",
            APPLICANT_ID, PROGRAM_ID, BLOCK_ID, inReviewBoolean ? "review" : "edit");
    assertThat(
            new ApplicantRoutes()
                .blockEditOrBlockReview(
                    tiProfile, APPLICANT_ID, PROGRAM_ID, BLOCK_ID, inReviewBoolean)
                .url())
        .isEqualTo(expectedBlockReviewUrl);
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
    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID, clock);
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
  }

  @Parameters({
    "true, PREVIOUS_BLOCK",
    "false, REVIEW_PAGE",
    "true, NEXT_BLOCK",
    "false, NEXT_BLOCK",
  })
  public void testConfirmAddressRoute_forTrustedIntermediary(
      String inReview, String applicantRequestedAction) {
    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID, clock);
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
  }

  @Test
  @Parameters({"true", "false"})
  public void testPreviousOrReviewRoute_forApplicantWithIdInProfile_newSchemaEnabled(
      String inReview) {
    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedPreviousUrl =
        String.format(
            "/programs/%d/blocks/%d/previous/%s?isFromUrlCall=false",
            PROGRAM_ID, CURRENT_BLOCK_INDEX - 1, inReview);
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
  }

  @Test
  @Parameters({"true", "false"})
  public void testPreviousOrReviewRoute_forTrustedIntermediary(String inReview) {
    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedPreviousUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%d/previous/%s?isFromUrlCall=false",
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
  }

  @Test
  public void testPreviousOrReviewRoute_currentBlockIndexOne_returnsPreviousBlock() {
    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedPreviousUrl =
        String.format(
            "/programs/%d/blocks/%d/previous/%s?isFromUrlCall=false", PROGRAM_ID, 0, false);
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
    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedReviewUrl = String.format("/programs/%d/review?isFromUrlCall=false", PROGRAM_ID);

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
    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedReviewUrl = String.format("/programs/%d/review?isFromUrlCall=false", PROGRAM_ID);

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
  @Parameters({
    "true, PREVIOUS_BLOCK",
    "false, REVIEW_PAGE",
    "true, NEXT_BLOCK",
    "false, NEXT_BLOCK",
  })
  public void testUpdateFileRoute_forApplicantWithIdInProfile_newSchemaEnabled(
      String inReview, String applicantRequestedAction) {
    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_APPLICANT.toString());
    profileData.addAttribute(
        ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, String.valueOf(APPLICANT_ID));
    CiviFormProfile applicantProfile = profileFactory.wrapProfileData(profileData);

    String expectedUpdateFileUrl =
        String.format(
            "/programs/%d/blocks/%s/updateFile/%s/%s?isFromUrlCall=false",
            PROGRAM_ID, BLOCK_ID, inReview, applicantRequestedAction);
    assertThat(
            new ApplicantRoutes()
                .updateFile(
                    applicantProfile,
                    APPLICANT_ID,
                    PROGRAM_ID,
                    BLOCK_ID,
                    Boolean.valueOf(inReview),
                    ApplicantRequestedAction.valueOf(applicantRequestedAction))
                .url())
        .isEqualTo(expectedUpdateFileUrl);
  }

  @Test
  @Parameters({
    "true, PREVIOUS_BLOCK",
    "false, PREVIOUS_BLOCK",
    "true, REVIEW_PAGE",
    "false, NEXT_BLOCK",
  })
  public void testUpdateFileRoute_forTrustedIntermediary(
      String inReview, String applicantRequestedAction) {
    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID, clock);
    profileData.addRole(Role.ROLE_TI.toString());
    CiviFormProfile tiProfile = profileFactory.wrapProfileData(profileData);

    String expectedUpdateFileUrl =
        String.format(
            "/applicants/%d/programs/%d/blocks/%s/updateFile/%s/%s?isFromUrlCall=false",
            APPLICANT_ID, PROGRAM_ID, BLOCK_ID, inReview, applicantRequestedAction);
    assertThat(
            new ApplicantRoutes()
                .updateFile(
                    tiProfile,
                    APPLICANT_ID,
                    PROGRAM_ID,
                    BLOCK_ID,
                    Boolean.valueOf(inReview),
                    ApplicantRequestedAction.valueOf(applicantRequestedAction))
                .url())
        .isEqualTo(expectedUpdateFileUrl);
  }

  @Test
  @Parameters({"true, REVIEW_PAGE", "false, REVIEW_PAGE", "true, NEXT_BLOCK", "false, NEXT_BLOCK"})
  public void testUpdateBlockRoute_forApplicantWithIdInProfile_newSchemaEnabled(
      String inReview, String applicantRequestedAction) {
    CiviFormProfileData profileData = new CiviFormProfileData(APPLICANT_ACCOUNT_ID, clock);
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
  }

  @Test
  @Parameters({"true, REVIEW_PAGE", "false, REVIEW_PAGE", "true, NEXT_BLOCK", "false, NEXT_BLOCK"})
  public void testUpdateBlockRoute_forTrustedIntermediary(
      String inReview, String applicantRequestedAction) {
    CiviFormProfileData profileData = new CiviFormProfileData(TI_ACCOUNT_ID, clock);
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
  }
}
