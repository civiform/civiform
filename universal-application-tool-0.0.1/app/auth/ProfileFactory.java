package auth;

import com.google.common.base.Preconditions;
import javax.inject.Inject;
import javax.inject.Provider;
import models.Account;
import models.Applicant;
import play.libs.concurrent.HttpExecutionContext;
import repository.DatabaseExecutionContext;
import repository.ProgramRepository;

public class ProfileFactory {

  private DatabaseExecutionContext dbContext;
  private HttpExecutionContext httpContext;
  private Provider<ProgramRepository> programRepositoryProvider;

  @Inject
  public ProfileFactory(
      DatabaseExecutionContext dbContext,
      HttpExecutionContext httpContext,
      Provider<ProgramRepository> programRepositoryProvider) {
    this.dbContext = Preconditions.checkNotNull(dbContext);
    this.httpContext = Preconditions.checkNotNull(httpContext);
    this.programRepositoryProvider = Preconditions.checkNotNull(programRepositoryProvider);
  }

  public UatProfileData createNewApplicant() {
    return create(Roles.ROLE_APPLICANT);
  }

  public UatProfileData createNewAdmin() {
    UatProfileData p = create(Roles.ROLE_UAT_ADMIN);
    wrapProfileData(p)
        .getAccount()
        .thenAccept(
            account -> {
              account.setGlobalAdmin(true);
              account.save();
            })
        .join();
    return p;
  }

  public UatProfile wrapProfileData(UatProfileData p) {
    return new UatProfile(dbContext, httpContext, p, programRepositoryProvider.get());
  }

  private UatProfileData create(Roles role) {
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
