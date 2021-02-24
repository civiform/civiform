package auth;

import com.google.common.base.Preconditions;
import java.time.Clock;
import javax.inject.Inject;
import play.libs.concurrent.HttpExecutionContext;
import repository.DatabaseExecutionContext;

public class ProfileFactory {

  private Clock clock;
  private DatabaseExecutionContext dbContext;
  private HttpExecutionContext httpContext;

  @Inject
  public ProfileFactory(
      Clock clock, DatabaseExecutionContext dbContext, HttpExecutionContext httpContext) {
    this.clock = Preconditions.checkNotNull(clock);
    this.dbContext = Preconditions.checkNotNull(dbContext);
    this.httpContext = Preconditions.checkNotNull(httpContext);
  }

  public UatProfileData createNewApplicant() {
    UatProfileData p = new UatProfileData(clock);
    p.init(dbContext);
    p.addRole(Roles.ROLE_APPLICANT.toString());
    return p;
  }

  public UatProfile wrapProfileData(UatProfileData p) {
    return new UatProfile(dbContext, httpContext, p);
  }
}
