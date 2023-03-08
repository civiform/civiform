package auth;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.base.Preconditions;
import com.nimbusds.jwt.JWT;
import java.util.Optional;
import models.Account;
import models.Applicant;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.DatabaseExecutionContext;

/**
 * This class is specifically intended to be serialized, encrypted, and stored in the Play session
 * cookie. It cannot contain anything that's not serializable - this includes database connections,
 * thread pools, etc.
 *
 * <p>It is wrapped by CiviFormProfile, which is what we should use server-side.
 */
public class CiviFormProfileData extends CommonProfile {
  private static final Logger logger = LoggerFactory.getLogger(CiviFormProfileData.class);

  private Optional<JWT> idToken;

  public CiviFormProfileData() {
    super();
  }

  public CiviFormProfileData(Long accountId) {
    this();
    this.setId(accountId.toString());
    this.idToken = Optional.empty();
    logger.warn("DEBUG LOGOUT: CiviFormProfileData 1 param constructor");
  }

  public CiviFormProfileData(Long accountId, JWT idToken) {
    this();
    this.setId(accountId.toString());
    this.idToken = Optional.of(idToken);
    logger.warn("DEBUG LOGOUT: CiviFormProfileData 2 param constructor. idToken = {}", idToken);
  }

  public Optional<JWT> getIdToken() {
    return idToken;
  }

  public void setIdToken(JWT idToken) {
    this.idToken = Optional.of(idToken);
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
