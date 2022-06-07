package auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;

import auth.ProfileFactory;
import auth.oidc.applicant.IdcsProfileAdapter;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import javax.inject.Provider;
import models.Account;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.oidc.profile.OidcProfile;
import repository.ResetPostgres;
import repository.UserRepository;

public class OidcProfileAdapterTest extends ResetPostgres {
  private static final String EMAIL = "foo@bar.com";
  private static final String ISSUER = "issuer";
  private static final String SUBJECT = "subject";
  private static final String AUTHORITY_ID = "iss: issuer sub: subject";

  private OidcProfileAdapter oidcProfileAdapter;
  private ProfileFactory profileFactory;

  @Before
  public void setup() {
    UserRepository repository = instanceOf(UserRepository.class);
    profileFactory = instanceOf(ProfileFactory.class);
    oidcProfileAdapter =
        // Just need some complete adaptor to access methods.
        new IdcsProfileAdapter(
            /* configuration= */ null,
            /* client= */ null,
            profileFactory,
            new Provider<UserRepository>() {
              @Override
              public UserRepository get() {
                return repository;
              }
            });
  }

  @Test
  public void getExistingApplicant_succeeds_noAuthorityFallsBackToEmail() {
    // When an existing account doesn't have an authority_id we still find it by email.

    // Setup.
    // Existing account doesn't have an authority.
    resourceCreator
        .insertAccount()
        .setEmailAddress(EMAIL)
        .setApplicants(ImmutableList.of(resourceCreator.insertApplicant()))
        .save();

    // Current OIDC info has an authority and email.
    OidcProfile profile = new OidcProfile();
    profile.addAttribute("user_emailid", EMAIL);
    profile.addAttribute("iss", ISSUER);
    profile.setId(SUBJECT);

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
    // Authority ID is the main key and returns the local account even with different other old keys
    // like email.

    // Setup.
    final String otherEmail = "OTHER@EMAIL.com";
    // Existing account has authority but some other email.
    resourceCreator
        .insertAccount()
        .setEmailAddress(otherEmail)
        .setAuthorityId(AUTHORITY_ID)
        .setApplicants(ImmutableList.of(resourceCreator.insertApplicant()))
        .save();

    // Current OIDC info has an authority and email.
    OidcProfile profile = new OidcProfile();
    profile.addAttribute("user_emailid", EMAIL);
    profile.addAttribute("iss", ISSUER);
    profile.setId(SUBJECT);

    // Execute.
    Optional<Applicant> applicant = oidcProfileAdapter.getExistingApplicant(profile);

    // Verify.
    assertThat(applicant).isPresent();
    Account account = applicant.get().getAccount();

    // The email of the existing account is the pre-existing one, not a new profile one.
    assertThat(account.getEmailAddress()).isEqualTo(otherEmail);
    assertThat(account.getAuthorityId()).isEqualTo(AUTHORITY_ID);
  }
}
