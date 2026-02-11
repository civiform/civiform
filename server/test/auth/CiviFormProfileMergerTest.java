package auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import java.util.Optional;
import models.ApplicantModel;
import models.ApplicationModel;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.oidc.profile.OidcProfile;
import repository.AccountRepository;
import repository.DatabaseExecutionContext;
import repository.ResetPostgres;
import services.program.ProgramDefinition;
import support.ProgramBuilder;

public class CiviFormProfileMergerTest extends ResetPostgres {

  private static final String EMAIL_ATTR = "user_emailid";
  private static final String EMAIL1 = "foo@bar.com";
  private static final String EMAIL2 = "bar@foo.com";

  private AccountRepository repository;
  private ProfileFactory profileFactory;
  private CiviFormProfileMerger civiFormProfileMerger;
  private UserProfile userProfile;
  private OidcProfile oidcProfile;

  @Before
  public void setup() {
    repository = instanceOf(AccountRepository.class);
    profileFactory = instanceOf(ProfileFactory.class);
    civiFormProfileMerger =
        new CiviFormProfileMerger(
            profileFactory, () -> repository, instanceOf(DatabaseExecutionContext.class));
    userProfile = new CommonProfile();
    oidcProfile = new OidcProfile();
    oidcProfile.addAttribute(EMAIL_ATTR, EMAIL1);
  }

  /** Helper method to create an applicant with an associated account. */
  private ApplicantModel createApplicant() {
    ApplicantModel applicant = resourceCreator.insertApplicant();
    applicant.setAccount(resourceCreator.insertAccount());
    applicant.save();
    return applicant;
  }

  @Test
  public void mergeProfiles_noExistingApplicantAndNoExistingProfile_succeeds() {
    var merged =
        civiFormProfileMerger.mergeProfiles(
            /* optionalApplicantInDatabase= */ Optional.empty(),
            /* optionalGuestProfile= */ Optional.empty(),
            oidcProfile,
            /* mergeFunction= */ (civiFormProfile, profile) -> {
              assertThat(civiFormProfile).isEmpty();
              assertThat(profile).isEqualTo(oidcProfile);
              return userProfile;
            });

    assertThat(merged).hasValue(userProfile);
  }

  @Test
  public void mergeProfiles_noExistingProfile_succeeds() {
    ApplicantModel applicant = createApplicant();
    Long expectedAccountId = applicant.getAccount().id;

    var merged =
        civiFormProfileMerger.mergeProfiles(
            Optional.of(applicant),
            /* optionalGuestProfile= */ Optional.empty(),
            oidcProfile,
            /* mergeFunction= */ (civiFormProfile, profile) -> {
              var profileData = civiFormProfile.orElseThrow().getProfileData();
              assertThat(profileData.getId()).isEqualTo(expectedAccountId.toString());
              assertThat(profile).isEqualTo(oidcProfile);
              return userProfile;
            });

    assertThat(merged).hasValue(userProfile);
  }

  @Test
  public void mergeProfiles_guestProfileWithNoApplications_succeeds() {
    ApplicantModel applicant = createApplicant();
    Long expectedAccountId = applicant.getAccount().id;
    CiviFormProfile civiFormProfile = profileFactory.wrap(applicant);

    var merged =
        civiFormProfileMerger.mergeProfiles(
            Optional.of(applicant),
            Optional.of(civiFormProfile),
            oidcProfile,
            /* mergeFunction= */ (innerCiviformProfile, innerProfile) -> {
              assertThat(innerCiviformProfile.orElseThrow().getProfileData().getId())
                  .isEqualTo(expectedAccountId.toString());
              assertThat(innerProfile).isEqualTo(oidcProfile);
              return userProfile;
            });

    assertThat(merged).hasValue(userProfile);
  }

  @Test
  public void mergeProfiles_noExistingApplicant_succeeds() {
    ApplicantModel guestApplicant = createApplicant();
    CiviFormProfileData profileData = new CiviFormProfileData();
    profileData.setId(guestApplicant.getAccount().id.toString());
    profileData.addAttribute(EMAIL_ATTR, EMAIL2);
    CiviFormProfile civiFormProfile = profileFactory.wrapProfileData(profileData);

    var merged =
        civiFormProfileMerger.mergeProfiles(
            /* optionalApplicantInDatabase= */ Optional.empty(),
            Optional.of(civiFormProfile),
            oidcProfile,
            /* mergeFunction= */ (innerCiviformProfile, innerProfile) -> {
              assertThat(
                      innerCiviformProfile.orElseThrow().getProfileData().getAttribute(EMAIL_ATTR))
                  .isEqualTo(EMAIL2);
              assertThat(innerProfile).isEqualTo(oidcProfile);
              return userProfile;
            });

    assertThat(merged).hasValue(userProfile);
  }

  @Test
  public void mergeProfiles_afterTwoWayMerge_succeeds() {
    // Create the logged-in user's applicant
    ApplicantModel loggedInApplicant = createApplicant();

    // Create a guest applicant with an application
    ApplicantModel guestApplicant = createApplicant();

    // Create a program and application for the guest
    ProgramDefinition program = ProgramBuilder.newActiveProgram("test-program").buildDefinition();
    ApplicationModel guestApplication =
        new ApplicationModel(guestApplicant, program.toProgram(), null);
    guestApplication.save();
    guestApplicant.refresh();

    // Create profile data for the guest
    CiviFormProfileData guestProfileData = new CiviFormProfileData();
    guestProfileData.setId(guestApplicant.getAccount().id.toString());
    CiviFormProfile guestProfile = profileFactory.wrapProfileData(guestProfileData);

    // Count applications before merge
    int initialGuestApplicationCount = guestApplicant.getApplications().size();
    assertThat(initialGuestApplicationCount).isEqualTo(1);

    Long expectedAccountId = loggedInApplicant.getAccount().id;
    var merged =
        civiFormProfileMerger.mergeProfiles(
            Optional.of(loggedInApplicant),
            Optional.of(guestProfile),
            oidcProfile,
            /* mergeFunction= */ (innerCiviFormProfile, innerProfile) -> {
              assertThat(innerCiviFormProfile.orElseThrow().getProfileData().getId())
                  .isEqualTo(expectedAccountId.toString());
              assertThat(innerProfile).isEqualTo(oidcProfile);
              return userProfile;
            });

    assertThat(merged).hasValue(userProfile);

    loggedInApplicant.refresh();
    guestApplicant.refresh();

    // Both applicants should now belong to the same account
    assertThat(loggedInApplicant.getAccount().id).isEqualTo(expectedAccountId);
    assertThat(guestApplicant.getAccount().id).isEqualTo(expectedAccountId);

    assertThat(loggedInApplicant.getApplications()).hasSize(0);
    assertThat(guestApplicant.getApplications()).hasSize(1);
  }

  @Test
  public void mergeProfiles_mergeFunctionThrows_applicantsNotMerged() {
    // Create the logged-in user's applicant
    ApplicantModel loggedInApplicant = createApplicant();

    // Create a guest applicant with an application
    ApplicantModel guestApplicant = createApplicant();

    // Create a program and application for the guest
    ProgramDefinition program = ProgramBuilder.newActiveProgram("test-program").buildDefinition();
    ApplicationModel guestApplication =
        new ApplicationModel(guestApplicant, program.toProgram(), null);
    guestApplication.save();
    guestApplicant.refresh();

    // Create profile data for the guest
    CiviFormProfileData guestProfileData = new CiviFormProfileData();
    guestProfileData.setId(guestApplicant.getAccount().id.toString());
    CiviFormProfile guestProfile = profileFactory.wrapProfileData(guestProfileData);
    // Count applications before merge
    int initialGuestApplicationCount = guestApplicant.getApplications().size();
    assertThat(initialGuestApplicationCount).isEqualTo(1);

    long loggedInApplicantId = loggedInApplicant.getAccount().id;
    long guestApplicantId = guestApplicant.getAccount().id;
    assertThrows(
        RuntimeException.class,
        () ->
            civiFormProfileMerger.mergeProfiles(
                Optional.of(loggedInApplicant),
                Optional.of(guestProfile),
                oidcProfile,
                /* mergeFunction= */ (unused, unused2) -> {
                  throw new RuntimeException();
                }));

    loggedInApplicant.refresh();
    guestApplicant.refresh();

    // The merge was not done due to the exception.
    // Both applicants have their original IDs.
    assertThat(loggedInApplicant.getAccount().id).isEqualTo(loggedInApplicantId);
    assertThat(guestApplicant.getAccount().id).isEqualTo(guestApplicantId);
  }
}
