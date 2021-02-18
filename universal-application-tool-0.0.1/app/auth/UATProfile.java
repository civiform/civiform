package auth;

import org.pac4j.core.profile.CommonProfile;

public class UATProfile extends CommonProfile {
  private static final long serialVersionUID = 1L;

  public UATProfile() {
    setId("hello");
  }
}
