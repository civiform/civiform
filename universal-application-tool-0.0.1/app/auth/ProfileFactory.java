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

  public UatProfile createNewApplicant() {
    UatProfile p = new UatProfile(clock);
    p.init();
    p.addRole(Roles.ROLE_APPLICANT.toString());
    return p;
  }
}
