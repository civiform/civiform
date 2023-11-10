package auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import auth.CiviFormProfile;
import auth.CiviFormProfileData;
import auth.ProfileFactory;
import auth.oidc.applicant.IdcsApplicantProfileCreator;
import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import models.Account;
import models.Applicant;
import models.TrustedIntermediaryGroup;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import repository.AccountRepository;
import repository.ResetPostgres;
import services.applicant.ApplicantData;
import support.CfTestHelpers;

public class CiviformOidcProfileCreatorTest extends ResetPostgres {
  private static final String EMAIL = "foo@bar.com";
  private static final String NAME = "Philip J. Fry";
  private static final String ISSUER = "issuer";
  private static final String SUBJECT = "subject";
  private static final String AUTHORITY_ID = "iss: issuer sub: subject";

  private static OidcProfile profile;

  private CiviformOidcProfileCreator oidcProfileAdapter;
  private ProfileFactory profileFactory;
  private static AccountRepository accountRepository;

  @Before
  public void setup() {
    accountRepository = instanceOf(AccountRepository.class);
    profileFactory = instanceOf(ProfileFactory.class);
    OidcClient client = CfTestHelpers.getOidcClient("dev-oidc", 3390);
    OidcConfiguration client_config = CfTestHelpers.getOidcConfiguration("dev-oidc", 3390);
    // Just need some complete adaptor to access methods.
    oidcProfileAdapter =
        new IdcsApplicantProfileCreator(
            client_config,
            client,
            OidcClientProviderParams.create(
                profileFactory, CfTestHelpers.userRepositoryProvider(accountRepository)));

    profile = new OidcProfile();
    profile.addAttribute("user_emailid", EMAIL);
    profile.addAttribute("user_displayname", NAME);
    profile.addAttribute("user_locale", "fr");
    profile.addAttribute("iss", ISSUER);
    profile.setId(SUBJECT);
  }

  @Test
  public void getExistingApplicant_succeeds_noAuthorityFallsBackToEmail() {
    // When an existing account doesn't have an authority_id we still find it by
    // email.

    // Setup.
    // Existing account doesn't have an authority.
    resourceCreator
        .insertAccount()
        .setEmailAddress(EMAIL)
        .setApplicants(ImmutableList.of(resourceCreator.insertApplicant()))
        .save();

    // Execute.
    Optional<Applicant> applicant = oidcProfileAdapter.getExistingApplicant(profile);

    // Verify.
    assertThat(applicant).isPresent();
    Account account = applicant.get().getAccount();

    assertThat(account.getEmailAddress()).isEqualTo(EMAIL);
    // The existing account doesn't have an authority as it didn't before.
    assertThat(account.getAuthorityId()).isNull();
  }

  @Test
  public void getExistingApplicant_succeeds_sameAuthorityDifferentEmail() {
    // Authority ID is the main key and returns the local account even with
    // different other old keys like email.

    // Setup.
    final String otherEmail = "OTHER@EMAIL.com";
    // Existing account has authority but some other email.
    resourceCreator
        .insertAccount()
        .setEmailAddress(otherEmail)
        .setAuthorityId(AUTHORITY_ID)
        .setApplicants(ImmutableList.of(resourceCreator.insertApplicant()))
        .save();

    // Execute.
    Optional<Applicant> applicant = oidcProfileAdapter.getExistingApplicant(profile);

    // Verify.
    assertThat(applicant).isPresent();
    Account account = applicant.get().getAccount();

    // The email of the existing account is the pre-existing one, not a new profile
    // one.
    assertThat(account.getEmailAddress()).isEqualTo(otherEmail);
    assertThat(account.getAuthorityId()).isEqualTo(AUTHORITY_ID);
  }

  @Test
  public void mergeCiviFormProfile_succeeds_new_user() {
    // Execute.
    CiviFormProfileData profileData =
        oidcProfileAdapter.mergeCiviFormProfile(Optional.empty(), profile);

    // Verify.
    assertThat(profileData).isNotNull();

    // The email of the existing account is the pre-existing one, not a new profile
    // one.
    assertThat(profileData.getEmail()).isEqualTo(EMAIL);

    Optional<Applicant> maybeApplicant = oidcProfileAdapter.getExistingApplicant(profile);
    assertThat(maybeApplicant).isPresent();

    ApplicantData applicantData = maybeApplicant.get().getApplicantData();

    assertThat(applicantData.getApplicantName().orElse("<empty optional>"))
        .isEqualTo("Fry, Philip");
    Locale l = applicantData.preferredLocale();
    assertThat(l).isEqualTo(Locale.FRENCH);
  }

  @Test
  public void mergeCiviFormProfile_skipped_forTrustedIntermediaries() {
    // Setup.
    Account accountWithTiGroup = new Account();
    accountWithTiGroup.setMemberOfGroup(new TrustedIntermediaryGroup("name", "description"));
    CiviFormProfile trustedIntermediary = mock(CiviFormProfile.class);
    when(trustedIntermediary.getAccount())
        .thenReturn(CompletableFuture.completedFuture(accountWithTiGroup));
    when(trustedIntermediary.getApplicant())
        .thenReturn(CompletableFuture.completedFuture(new Applicant()));

    CiviFormProfileData fakeProfileData = new CiviFormProfileData(123L);
    when(trustedIntermediary.getProfileData()).thenReturn(fakeProfileData);

    // Execute.
    CiviFormProfileData profileData =
        oidcProfileAdapter.mergeCiviFormProfile(Optional.of(trustedIntermediary), profile);

    // Verify.
    // Profile data should still be present after the no-op merge.
    assertThat(profileData).isNotNull();
    assertThat(profileData).isEqualTo(fakeProfileData);

    // email is set
    assertThat(profileData.getEmail()).isEqualTo(EMAIL);
    assertThat(profileData.getDisplayName()).isNull();

    Optional<Applicant> maybeApplicant = oidcProfileAdapter.getExistingApplicant(profile);
    assertThat(maybeApplicant).isNotPresent();
  }
}
