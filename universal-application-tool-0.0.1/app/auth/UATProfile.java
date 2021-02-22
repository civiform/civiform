package auth;

import org.pac4j.core.profile.CommonProfile;

public class UATProfile extends CommonProfile {
  public UATProfile() {
    super();
    // TODO: write this to the database, read the ID back, and use that as the ID here.
    setId("TODO");
  }
}
