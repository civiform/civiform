package auth;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.base.Preconditions;
import models.Account;
import models.Applicant;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.definition.CommonProfileDefinition;
import repository.DatabaseExecutionContext;

/**
 * This class is specifically intended to be serialized, encrypted, and stored in the Play session
 * cookie. It cannot contain anything that's not serializable - this includes database connections,
 * thread pools, etc.
 *
 * <p>It is wrapped by CiviFormProfile, which is what we should use server-side.
 */
public class CiviFormProfileData extends CommonProfile {

  public CiviFormProfileData() {
    super();
  }

  public CiviFormProfileData(Long accountId) {
    this();
    this.setId(accountId.toString());
  }

  /**
   * Sets the "canonical" email field in the profile data. Some identity providers use non-standard
   * attribute names for email. We use the attribute name provided by pac4j here to ensure all
   * profiles store the email in the same place to make it is accessible via {@code
   * CommonProfile.getEmail()}.
   */
  public CiviFormProfileData setEmail(String email) {
    addAttribute(CommonProfileDefinition.EMAIL, email);
    return this;
  }

  /**
   * True if the "canonical" email attribute is set in the profile data. Some identity providers use
   * non-standard attribute names for email. We use the attribute name provided by pac4j here for
   * all profiles for consistency.
   */
  public boolean hasCanonicalEmail() {
    return getAttributes().containsKey(CommonProfileDefinition.EMAIL);
  }

  /**
   * This method needs to be called outside the constructor since constructors should not do
   * database accesses (or other work). It should be called before the object is used - the object
   * has not been persisted / correctly created until it is called.
   */
  public void init(DatabaseExecutionContext dbContext) {
    if (this.getId() != null && !this.getId().isEmpty()) {
      return;
    }
    // We use this async only to make sure we run in the db execution context - this method cannot
    // be
    // asynchronous because the security code that executes it is entirely synchronous.
    supplyAsync(
            () -> {
              Account acc = new Account();
              acc.save();
              Applicant newA = new Applicant();
              newA.setAccount(acc);
              newA.save();

              setId(Preconditions.checkNotNull(acc.id).toString());
              return null;
            },
            dbContext)
        .join();
  }
}
