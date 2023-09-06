package auth;

import org.pac4j.core.profile.UserProfile;
import repository.DatabaseExecutionContext;

/**
 * This interface is specifically intended for instances that will be serialized, encrypted, and
 * stored in the Play session cookie. Instances cannot contain anything that's not serializable -
 * this includes database connections, thread pools, etc.
 *
 * <p>Instances are wrapped by CiviFormProfile, which is what we should use server-side.
 */
public interface CiviFormProfileData extends UserProfile {

  // XXX
  //  public CiviFormProfileData(Long accountId) {
  //    this();
  //    this.setId(accountId.toString());
  //  }

  /**
   * This method is called directly after construction. It can perform any required work that should
   * not be performed in constructors.
   *
   * <p>It should be called before the object is used; the object has not been persisted / correctly
   * created until it is called.
   */
  void init(DatabaseExecutionContext dbContext);
}
