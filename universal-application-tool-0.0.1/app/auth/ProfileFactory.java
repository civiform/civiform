package auth;

import com.google.common.base.Preconditions;
import java.time.Clock;
import javax.inject.Inject;

public class ProfileFactory {

  private Clock clock;

  @Inject
  public ProfileFactory(Clock clock) {
    this.clock = Preconditions.checkNotNull(clock);
  }

  public UATProfile createNewApplicant() {
    UATProfile p = new UATProfile(clock);
    p.init();
    p.addRole(Roles.ROLE_APPLICANT);
    return p;
  }
}
