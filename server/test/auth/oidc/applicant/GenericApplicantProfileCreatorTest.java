package auth.oidc.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static play.test.Helpers.fakeRequest;

import auth.CiviFormProfileData;
import auth.IdentityProviderType;
import auth.ProfileFactory;
import auth.oidc.IdTokensFactory;
import auth.oidc.OidcClientProviderParams;
import com.google.common.collect.ImmutableList;
import java.util.Locale;
import java.util.Optional;
import models.Applicant;
import org.junit.Before;
import org.junit.Test;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.oidc.profile.OidcProfile;
import org.pac4j.play.PlayWebContext;
import repository.AccountRepository;
import repository.ResetPostgres;
import services.applicant.ApplicantData;
import support.CfTestHelpers;

public class GenericApplicantProfileCreatorTest extends ResetPostgres {
  private static final String ISSUER = "issuer";
  private static final String SUBJECT = "subject";

  private static final String EMAIL_ATTRIBUTE_NAME = "email";
  private static final String LOCALE_ATTRIBUTE_NAME = "locale";
  private static final String FIRST_NAME_ATTRIBUTE_NAME = "first_name";
  private static final String MIDDLE_NAME_ATTRIBUTE_NAME = "middle_name";
  private static final String LAST_NAME_ATTRIBUTE_NAME = "last_name";

  private GenericApplicantProfileCreator oidcProfileAdapter;
  private ProfileFactory profileFactory;
  private IdTokensFactory idTokensFactory;
  private static AccountRepository accountRepository;

  @Before
  public void setup() {
    accountRepository = instanceOf(AccountRepository.class);
    profileFactory = instanceOf(ProfileFactory.class);
    idTokensFactory = instanceOf(IdTokensFactory.class);
    OidcClient client = CfTestHelpers.getOidcClient("dev-oidc", 3390);
    OidcConfiguration client_config = CfTestHelpers.getOidcConfiguration("dev-oidc", 3390);
    // Just need some complete adaptor to access methods.
    oidcProfileAdapter =
        new GenericApplicantProfileCreator(
            client_config,
            client,
            OidcClientProviderParams.create(
                profileFactory,
                idTokensFactory,
                CfTestHelpers.userRepositoryProvider(accountRepository)),
            EMAIL_ATTRIBUTE_NAME,
            LOCALE_ATTRIBUTE_NAME,
            ImmutableList.of(
                FIRST_NAME_ATTRIBUTE_NAME, MIDDLE_NAME_ATTRIBUTE_NAME, LAST_NAME_ATTRIBUTE_NAME));
  }

  @Test
  public void mergeCiviFormProfile_succeeds_new_user() {
    OidcProfile profile = new OidcProfile();
    profile.addAttribute(EMAIL_ATTRIBUTE_NAME, "foo@bar.com");
    profile.addAttribute(FIRST_NAME_ATTRIBUTE_NAME, "Philip");
    profile.addAttribute(MIDDLE_NAME_ATTRIBUTE_NAME, "J.");
    profile.addAttribute(LAST_NAME_ATTRIBUTE_NAME, "Fry");
    profile.addAttribute(LOCALE_ATTRIBUTE_NAME, "en");
    profile.addAttribute("iss", ISSUER);
    profile.setId(SUBJECT);

    PlayWebContext context = new PlayWebContext(fakeRequest().build());
    CiviFormProfileData profileData =
        oidcProfileAdapter.mergeCiviFormProfile(Optional.empty(), profile, context);
    assertThat(profileData).isNotNull();
    assertThat(profileData.getEmail()).isEqualTo("foo@bar.com");

    Optional<Applicant> maybeApplicant = oidcProfileAdapter.getExistingApplicant(profile);
    assertThat(maybeApplicant).isPresent();

    ApplicantData applicantData = maybeApplicant.get().getApplicantData();

    assertThat(applicantData.getApplicantName().orElse("<empty optional>"))
        .isEqualTo("Fry, Philip");
    Locale l = applicantData.preferredLocale();
    assertThat(l).isEqualTo(Locale.ENGLISH);
  }

  @Test
  public void applicantProfileCreator_identityProviderTypeIsCorrect() {
    assertThat(oidcProfileAdapter.identityProviderType())
        .isEqualTo(IdentityProviderType.APPLICANT_IDENTITY_PROVIDER);
  }
}
