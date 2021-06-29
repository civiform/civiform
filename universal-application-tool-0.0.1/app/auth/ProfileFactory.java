package auth;

import com.google.common.base.Preconditions;
import javax.inject.Inject;
import javax.inject.Provider;
import models.Account;
import models.Applicant;
import play.libs.concurrent.HttpExecutionContext;
import repository.DatabaseExecutionContext;
import repository.VersionRepository;

/**
 * This class helps create {@link CiviFormProfile} and {@link CiviFormProfileData} objects for
 * existing and new accounts. New accounts are persisted in database.
 */
public class ProfileFactory {

  private DatabaseExecutionContext dbContext;
  private HttpExecutionContext httpContext;
  private Provider<VersionRepository> versionRepositoryProvider;

  @Inject
  public ProfileFactory(
      DatabaseExecutionContext dbContext,
      HttpExecutionContext httpContext,
      Provider<VersionRepository> versionRepositoryProvider) {
    this.dbContext = Preconditions.checkNotNull(dbContext);
    this.httpContext = Preconditions.checkNotNull(httpContext);
    this.versionRepositoryProvider = Preconditions.checkNotNull(versionRepositoryProvider);
  }

  public CiviFormProfileData createNewApplicant() {
    return create(Roles.ROLE_APPLICANT);
  }

  public CiviFormProfileData createNewAdmin() {
    CiviFormProfileData p = create(Roles.ROLE_CIVIFORM_ADMIN);
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

  public CiviFormProfile wrapProfileData(CiviFormProfileData p) {
    return new CiviFormProfile(dbContext, httpContext, p);
  }

  private CiviFormProfileData create(Roles role) {
    CiviFormProfileData p = new CiviFormProfileData();
    p.init(dbContext);
    p.addRole(role.toString());
    return p;
  }

  public CiviFormProfile wrap(Account account) {
    return wrapProfileData(new CiviFormProfileData(account.id));
  }

  public CiviFormProfile wrap(Applicant applicant) {
    return wrapProfileData(new CiviFormProfileData(applicant.getAccount().id));
  }

  public CiviFormProfileData createNewProgramAdmin() {
    return create(Roles.ROLE_PROGRAM_ADMIN);
  }

  /**
   * This creates a program admin who is automatically the admin of all programs currently live,
   * with a fake email address.
   */
  public CiviFormProfileData createFakeProgramAdmin() {
    CiviFormProfileData p = create(Roles.ROLE_PROGRAM_ADMIN);
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
