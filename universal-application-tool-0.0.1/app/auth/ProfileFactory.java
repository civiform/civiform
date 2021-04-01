package auth;

import com.google.common.base.Preconditions;
import javax.inject.Inject;
import models.Account;
import models.Applicant;
import play.libs.concurrent.HttpExecutionContext;
import repository.DatabaseExecutionContext;

public class ProfileFactory {

  private DatabaseExecutionContext dbContext;
  private HttpExecutionContext httpContext;

  @Inject
  public ProfileFactory(DatabaseExecutionContext dbContext, HttpExecutionContext httpContext) {
    this.dbContext = Preconditions.checkNotNull(dbContext);
    this.httpContext = Preconditions.checkNotNull(httpContext);
  }

  public UatProfileData createNewApplicant() {
    return create(Roles.ROLE_APPLICANT);
  }

  public UatProfileData createNewAdmin() {
    return create(Roles.ROLE_UAT_ADMIN);
  }

  public UatProfile wrapProfileData(UatProfileData p) {
    return new UatProfile(dbContext, httpContext, p);
  }

  private UatProfileData create(Roles role) {
    // Not sure if this is bad to do
    UatProfileData p = new UatProfileData();
    p.init(dbContext);
    p.addRole(role.toString());
    return p;
  }

  public UatProfile wrap(Account account) {
    return wrapProfileData(new UatProfileData(account.id));
  }

  public UatProfile wrap(Applicant applicant) {
    return wrapProfileData(new UatProfileData(applicant.getAccount().id));
  }
}
