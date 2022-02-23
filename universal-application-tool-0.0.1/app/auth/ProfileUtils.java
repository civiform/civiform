package auth;

import com.google.common.base.Preconditions;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import javax.inject.Inject;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.context.session.SessionStore;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.play.PlayWebContext;
import play.mvc.Http;

/** A utility class for CiviForm profile. */
public class ProfileUtils {
  private SessionStore sessionStore;
  private ProfileFactory profileFactory;

  @Inject
  public ProfileUtils(SessionStore sessionStore, ProfileFactory profileFactory) {
    this.sessionStore = Preconditions.checkNotNull(sessionStore);
    this.profileFactory = Preconditions.checkNotNull(profileFactory);
  }

  /**
   * Fetch the current profile from the session cookie, which the ProfileManager will fetch from the
   * request's cookies, using the injected session store to decrypt it.
   */
  public Optional<CiviFormProfile> currentUserProfile(Http.RequestHeader request) {
    PlayWebContext webContext = new PlayWebContext(request);
    return currentUserProfile(webContext);
  }

  /**
   * Fetch the current profile from the session cookie, which the ProfileManager will fetch from the
   * context's cookies, using the injected session store to decrypt it.
   */
  public Optional<CiviFormProfile> currentUserProfile(WebContext webContext) {
    ProfileManager profileManager = new ProfileManager(webContext, sessionStore);
    Optional<CiviFormProfileData> p = profileManager.getProfile(CiviFormProfileData.class);
    if (p.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(profileFactory.wrapProfileData(p.get()));
  }

  // A temporary placeholder email value, used while the user needs to verify their account.
  private static final String IDCS_PLACEHOLDER_EMAIL_LOWERCASE =
      "ITD_UCSS_UAT@seattle.gov".toLowerCase();
  // Testing account to allow for manual verification of the check.
  private static final String IDCS_PLACEHOLDER_TEST_EMAIL_LOWERCASE =
      "CiviFormStagingTest@gmail.com".toLowerCase();

  /** Return true if the account is not a fully usable account to the City of Seattle. */
  public boolean accountIsIdcsPlaceholder(CiviFormProfile profile) {
    Optional<String> userEmail = Optional.ofNullable(profile.getProfileData().getEmail());

    if (userEmail.isEmpty()) {
      return false;
    }

    String userEmailLowercase = userEmail.get().toLowerCase();

    return IDCS_PLACEHOLDER_EMAIL_LOWERCASE.equals(userEmailLowercase)
        || IDCS_PLACEHOLDER_TEST_EMAIL_LOWERCASE.equals(userEmailLowercase);
  }

  /** Return true if the account referenced by the profile exists. */
  public boolean validCiviFormProfile(CiviFormProfile profile) {
    try {
      profile.getAccount().join();
      return true;
    } catch (CompletionException e) {
      if (e.getCause() instanceof AccountNonexistentException) {
        return false;
      }
      throw new RuntimeException(e);
    }
  }
}
