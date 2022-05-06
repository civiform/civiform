package auth;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.Optional;
import models.Account;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.oidc.profile.OidcProfile;
import repository.ResetPostgres;
import repository.UserRepository;

public class CiviFormProfileMergerTest extends ResetPostgres {

  private static final String EMAIL_ATTR = "user_emailid";
  private static final String EMAIL1 = "foo@bar.com";
  private static final String EMAIL2 = "bar@foo.com";
  private static final Long ACCOUNT_ID = 12345L;
  private static final UserProfile USER_PROFILE = new CommonProfile();

  private UserRepository repository;
  private ProfileFactory profileFactory;

  private OidcProfile oidcProfile;
  private CiviFormProfileData civiFormProfileData;
  private Applicant applicant;
  private Account account;

  private CiviFormProfileMerger civiFormProfileMerger;

  @Before
  public void setup() {
    profileFactory = spy(instanceOf(ProfileFactory.class));
    repository = spy(instanceOf(UserRepository.class));
    civiFormProfileMerger = new CiviFormProfileMerger(profileFactory, () -> repository);

    oidcProfile = new OidcProfile();
    oidcProfile.addAttribute(EMAIL_ATTR, EMAIL1);

    civiFormProfileData = new CiviFormProfileData();
    civiFormProfileData.setId(ACCOUNT_ID.toString());
    civiFormProfileData.addAttribute(EMAIL_ATTR, EMAIL2);

    account = new Account();
    account.id = ACCOUNT_ID;

    applicant = spy(new Applicant());
    applicant.setAccount(account);
    account.setApplicants(Collections.singletonList(applicant));
  }

  @Test
  public void mergeProfiles_succeeds_noExistingApplicantAndNoExistingProfile() {
    var merged =
        civiFormProfileMerger.mergeProfiles(
            /* applicantInDatabase= */ Optional.empty(),
            /* guestProfile= */ Optional.empty(),
            oidcProfile,
            (civiFormProfile, profile) -> {
              assertThat(civiFormProfile).isEmpty();
              assertThat(profile).isEqualTo(oidcProfile);
              return USER_PROFILE;
            });
    assertThat(merged).hasValue(USER_PROFILE);
  }

  @Test
  public void mergeProfiles_succeeds_noExistingProfile() {
    var merged =
        civiFormProfileMerger.mergeProfiles(
            /* applicantInDatabase= */ Optional.of(applicant),
            /* guestProfile= */ Optional.empty(),
            oidcProfile,
            (civiFormProfile, profile) -> {
              var profileData = civiFormProfile.orElseThrow().getProfileData();
              assertThat(profileData.getId()).isEqualTo(ACCOUNT_ID.toString());
              assertThat(profile).isEqualTo(oidcProfile);
              return USER_PROFILE;
            });
    assertThat(merged).hasValue(USER_PROFILE);
  }

  @Test
  public void mergeProfiles_succeeds_noExistingApplicant() {
    var merged =
        civiFormProfileMerger.mergeProfiles(
            Optional.empty(),
            /* guestProfile= */ Optional.of(profileFactory.wrapProfileData(civiFormProfileData)),
            oidcProfile,
            (civiFormProfile, profile) -> {
              var profileData = civiFormProfile.orElseThrow().getProfileData();
              assertThat(profileData.getAttribute(EMAIL_ATTR)).isEqualTo(EMAIL2);
              assertThat(profile).isEqualTo(oidcProfile);
              return USER_PROFILE;
            });
    assertThat(merged).hasValue(USER_PROFILE);
  }

  @Test
  public void mergeProfiles_succeeds_afterTwoWayMerge() {
    var existingProfile = spy(profileFactory.wrapProfileData(civiFormProfileData));
    doReturn(completedFuture(account)).when(existingProfile).getAccount();

    var expectedApplicant = new Applicant();
    expectedApplicant.setAccount(new Account());
    expectedApplicant.getAccount().id = ACCOUNT_ID;
    doReturn(completedFuture(expectedApplicant))
        .when(repository)
        .mergeApplicants(applicant, applicant, account);

    var merged =
        civiFormProfileMerger.mergeProfiles(
            Optional.of(applicant),
            Optional.of(existingProfile),
            oidcProfile,
            (civiFormProfile, profile) -> {
              var id = civiFormProfile.orElseThrow().getId();
              assertThat(id).isEqualTo(ACCOUNT_ID.toString());
              assertThat(profile).isEqualTo(oidcProfile);
              return USER_PROFILE;
            });
    verify(repository).mergeApplicants(eq(applicant), eq(applicant), eq(account));
    assertThat(merged).hasValue(USER_PROFILE);
  }
}
