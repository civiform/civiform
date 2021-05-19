package auth;

import com.google.common.base.Preconditions;
import javax.inject.Inject;
import javax.inject.Provider;
import models.Account;
import models.Applicant;
import play.libs.concurrent.HttpExecutionContext;
import repository.DatabaseExecutionContext;
import repository.ProgramRepository;
import repository.VersionRepository;

public class ProfileFactory {

  private DatabaseExecutionContext dbContext;
  private HttpExecutionContext httpContext;
  private Provider<ProgramRepository> programRepositoryProvider;
  private Provider<VersionRepository> versionRepositoryProvider;

  @Inject
  public ProfileFactory(
      DatabaseExecutionContext dbContext,
      HttpExecutionContext httpContext,
      Provider<ProgramRepository> programRepositoryProvider,
      Provider<VersionRepository> versionRepositoryProvider) {
    this.dbContext = Preconditions.checkNotNull(dbContext);
    this.httpContext = Preconditions.checkNotNull(httpContext);
    this.programRepositoryProvider = Preconditions.checkNotNull(programRepositoryProvider);
    this.versionRepositoryProvider = Preconditions.checkNotNull(versionRepositoryProvider);
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

  public UatProfileData createNewProgramAdmin() {
    UatProfileData p = create(Roles.ROLE_PROGRAM_ADMIN);
    wrapProfileData(p)
        .getAccount()
        .thenAccept(
            account -> {
              versionRepositoryProvider
                  .get()
                  .getActiveVersion()
                  .getPrograms()
                  .forEach(
                      program -> account.addAdministeredProgram(program.getProgramDefinition()));
              account.setEmailAddress(String.format("fake-local-admin-%d@example.com", account.id));
              account.save();
            })
        .join();
    return p;
  }
}
