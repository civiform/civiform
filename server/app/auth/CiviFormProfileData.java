package auth;

import java.io.Serializable;
import org.pac4j.core.profile.UserProfile;
import repository.DatabaseExecutionContext;

/**
 * This interface is specifically intended for instances that will be serialized, encrypted, and
 * stored in the Play session cookie. Instances cannot contain anything that's not serializable -
 * this includes database connections, thread pools, etc.
 *
 * <p>This interface should only be implemented by subclasses of
 * org.pac4j.core.profile.CommonProfile.
 *
 * <p>Instances are wrapped by CiviFormProfile, which is what we should use server-side.
 */
public interface CiviFormProfileData extends UserProfile, Serializable {
  /**
   * This method is called directly after construction. It can perform any required work that should
   * not be performed in constructors.
   *
   * <p>It should be called before the object is used; the object has not been persisted / correctly
   * created until it is called.
   */
  void init(DatabaseExecutionContext dbContext);

  // These methods are not specified in the org.pac4j.core.profile.UserProfile interface, but
  String getEmail();

  String getDisplayName();
}
