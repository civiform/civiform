package auth;

import org.pac4j.core.profile.CommonProfile;

public class UATProfile extends CommonProfile {
  public UATProfile() {
    super();
    setId("hello");
  }
}
