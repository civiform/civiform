package auth;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import models.Account;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.oidc.profile.OidcProfile;
import repository.UserRepository;

@RunWith(MockitoJUnitRunner.class)
public class CiviFormProfileMergerTest {

  private static final String EMAIL_ATTR = "user_emailid";
  private static final String EMAIL1 = "foo@bar.com";
  private static final String EMAIL2 = "bar@foo.com";
  private static final Long ACCOUNT_ID = 12345L;

  @Mock private UserRepository repository;
  @Mock private ProfileFactory profileFactory;
  @Mock private CiviFormProfile civiFormProfile;

  private UserProfile userProfile;
  private OidcProfile oidcProfile;
  private CiviFormProfileData civiFormProfileData;
  private Applicant applicant;
  private Account account;

  private CiviFormProfileMerger civiFormProfileMerger;

  @Before
  public void setup() {
    civiFormProfileMerger = new CiviFormProfileMerger(profileFactory, () -> repository);

    userProfile = new CommonProfile();

    oidcProfile = new OidcProfile();
    oidcProfile.addAttribute(EMAIL_ATTR, EMAIL1);

    civiFormProfileData = new CiviFormProfileData();
    civiFormProfileData.setId(ACCOUNT_ID.toString());
    civiFormProfileData.addAttribute(EMAIL_ATTR, EMAIL2);

    account = new Account();
    account.id = ACCOUNT_ID;

    applicant = new Applicant();
    applicant.setAccount(account);
    account.setApplicants(Collections.singletonList(applicant));

    when(civiFormProfile.getProfileData()).thenReturn(civiFormProfileData);
    when(profileFactory.wrap(any(Applicant.class))).thenReturn(civiFormProfile);
    when(civiFormProfile.getApplicant()).thenReturn(completedFuture(applicant));
    when(repository.mergeApplicants(applicant, applicant, account))
        .thenReturn(completedFuture(applicant));
  }

  @Test
  public void mergeProfiles_succeeds_noExistingApplicantAndNoExistingProfile() {
    var merged =
        civiFormProfileMerger.mergeProfiles(
            /* applicantInDatabase = */ Optional.empty(),
            /* guestProfile = */ Optional.empty(),
            /* idToken = */ Optional.empty(),
            oidcProfile,
            (civiFormProfile, profile) -> {
              assertThat(civiFormProfile).isEmpty();
              assertThat(profile).isEqualTo(oidcProfile);
              return userProfile;
            });
    assertThat(merged).hasValue(userProfile);
  }

  @Test
  public void mergeProfiles_succeeds_noExistingProfile() {
    var merged =
        civiFormProfileMerger.mergeProfiles(
            Optional.of(applicant),
            /* guestProfile = */ Optional.empty(),
            /* idToken = */ Optional.empty(),
            oidcProfile,
            (civiFormProfile, profile) -> {
              var profileData = civiFormProfile.orElseThrow().getProfileData();
              assertThat(profileData.getId()).isEqualTo(ACCOUNT_ID.toString());
              assertThat(profile).isEqualTo(oidcProfile);
              return userProfile;
            });
    assertThat(merged).hasValue(userProfile);
  }

  @Test
  public void mergeProfiles_succeeds_noExistingApplicant() {
    var merged =
        civiFormProfileMerger.mergeProfiles(
            /* applicantInDatabase = */ Optional.empty(),
            Optional.of(civiFormProfile),
            /* idToken = */ Optional.empty(),
            oidcProfile,
            (civiFormProfile, profile) -> {
              var profileData = civiFormProfile.orElseThrow().getProfileData();
              assertThat(profileData.getAttribute(EMAIL_ATTR)).isEqualTo(EMAIL2);
              assertThat(profile).isEqualTo(oidcProfile);
              return userProfile;
            });
    assertThat(merged).hasValue(userProfile);
  }

  @Test
  public void mergeProfiles_succeeds_afterTwoWayMerge() {
    var merged =
        civiFormProfileMerger.mergeProfiles(
            Optional.of(applicant),
            Optional.of(civiFormProfile),
            /* idToken = */ Optional.empty(),
            oidcProfile,
            (civiFormProfile, profile) -> {
              var profileData = civiFormProfile.orElseThrow().getProfileData();
              assertThat(profileData.getId()).isEqualTo(ACCOUNT_ID.toString());
              assertThat(profile).isEqualTo(oidcProfile);
              return userProfile;
            });
    verify(repository).mergeApplicants(eq(applicant), eq(applicant), eq(account));
    assertThat(merged).hasValue(userProfile);
  }
}
