package auth;

import org.pac4j.core.profile.CommonProfile;
import repository.DatabaseExecutionContext;

/**
 * A fake implementation of the CiviFormProfileData interface, not tied to any authentication
 * mechanism.
 *
 * <p>Should only be used in tests.
 */
public class FakeCiviFormProfileData extends CommonProfile implements CiviFormProfileData {
  public FakeCiviFormProfileData() {
    super();
  }

  public FakeCiviFormProfileData(Long accountId) {
    this();
    this.setId(accountId.toString());
  }

  @Override
  public void init(DatabaseExecutionContext dbContext) {}
}
