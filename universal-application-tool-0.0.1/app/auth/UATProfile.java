package auth;

import com.google.common.base.Preconditions;
import java.time.Clock;
import models.Account;
import models.Applicant;
import org.pac4j.core.profile.CommonProfile;

public class UATProfile extends CommonProfile {
  private final Clock clock;
  private Applicant applicant;

  public UATProfile() {
    this(Clock.systemDefaultZone());
  }

  public UATProfile(Clock clock) {
    super();
    this.clock = clock;
  }

  public Applicant getApplicant() {
    if (applicant == null) {
      this.applicant = new Applicant();
      this.applicant.id = Long.valueOf(this.getId());
      this.applicant.refresh();
    }
    return applicant;
  }

  public Account getAccount() {
    return getApplicant().getAccount();
  }

  public void init() {
    Account acc = new Account();
    acc.save();
    Applicant newA = new Applicant();
    newA.getApplicantData().put("$.metadata", "created_time", clock.instant().toString());
    newA.setAccount(acc);
    newA.save();

    setId(Preconditions.checkNotNull(newA.id).toString());
    this.applicant = newA;
  }
}
