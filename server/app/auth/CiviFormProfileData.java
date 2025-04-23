package auth;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInput;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import models.AccountModel;
import models.ApplicantModel;
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
  public static final String SESSION_ID = "sessionId";

  // It is crucial that serialization of this class does not change, so that user profiles continue
  // to be honored and in-progress applications are not lost.
  //
  // However, serialization is highly sensitive to details of the class, well beyond the actual data
  // being serialized: see https://docs.oracle.com/javase/7/docs/api/java/io/Serializable.html
  //
  // The value below corresponds to the value computed by the compiler for the current version of
  // the class. Specifying this value will prevent us from introducing changes that break
  // serialization.
  private static final long serialVersionUID = 3142603030317816700L;

  public CiviFormProfileData() {
    super();
    addAttribute(SESSION_ID, UUID.randomUUID().toString());
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

  /** Returns the session ID for this profile. */
  public String getSessionId() {
    return getAttributeAsString(SESSION_ID);
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
              AccountModel acc = new AccountModel();
              acc.save();
              ApplicantModel newA = new ApplicantModel();
              newA.setAccount(acc);
              newA.save();

              setId(Preconditions.checkNotNull(acc.id).toString());
              return null;
            },
            dbContext)
        .join();
  }

  /* In pac4j 5, there was a deprecated "permissions" field in the profile. This was removed
   * in pac4j 6. Because pac4j doesn't automatically handle this (arg), we override the
   * readExternal function here to skip over that field if we find it. This isn't great, but
   * it preserves existing sessions. When we move to pac4j 7, we should remove this (assuming
   * they haven't messed with the profile objects yet again).
   *
   * pac4j 5: https://github.com/pac4j/pac4j/blob/40681a51ecd674415e7024a286f2558b4c2cf125/pac4j-core/src/main/java/org/pac4j/core/profile/BasicUserProfile.java#L473-L483
   * pac4j 6: https://github.com/pac4j/pac4j/blob/294dc121432533cb7e81d0cf57c5cfebf6641953/pac4j-core/src/main/java/org/pac4j/core/profile/BasicUserProfile.java#L409-L418
   */
  @SuppressWarnings("unchecked")
  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    setId((String) in.readObject());
    addAttributes((Map<String, Object>) in.readObject());
    addAuthenticationAttributes((Map<String, Object>) in.readObject());
    setRemembered(in.readBoolean());
    setRoles((Set<String>) in.readObject());

    Object clientNameObject = in.readObject();
    if (clientNameObject instanceof Set) {
      clientNameObject = in.readObject();
    }
    setClientName((String) clientNameObject);

    setLinkedId((String) in.readObject());
  }
}
