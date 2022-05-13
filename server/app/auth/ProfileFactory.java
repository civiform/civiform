package auth;

import com.google.common.base.Preconditions;
import javax.inject.Inject;
import javax.inject.Provider;
import models.Account;
import models.ApiKey;
import models.Applicant;
import play.cache.NamedCache;
import play.cache.SyncCacheApi;
import play.libs.concurrent.HttpExecutionContext;
import repository.DatabaseExecutionContext;
import repository.VersionRepository;

import java.util.Optional;

/**
 * This class helps create {@link CiviFormProfile} and {@link CiviFormProfileData} objects for
 * existing and new accounts. New accounts are persisted in database.
 */
public class ProfileFactory {

  private DatabaseExecutionContext dbContext;
  private HttpExecutionContext httpContext;
  private Provider<VersionRepository> versionRepositoryProvider;
  private SyncCacheApi syncCacheApi;

  @Inject
  public ProfileFactory(
      DatabaseExecutionContext dbContext,
      HttpExecutionContext httpContext,
      Provider<VersionRepository> versionRepositoryProvider,
      @NamedCache("api-keys") SyncCacheApi syncCacheApi) {
    this.dbContext = Preconditions.checkNotNull(dbContext);
    this.httpContext = Preconditions.checkNotNull(httpContext);
    this.versionRepositoryProvider = Preconditions.checkNotNull(versionRepositoryProvider);
    this.syncCacheApi = Preconditions.checkNotNull(syncCacheApi);
  }

  public CiviFormProfileData createNewApplicant() {
    return create(new Roles[] {Roles.ROLE_APPLICANT});
  }

  public CiviFormProfileData createNewAdmin() {
    CiviFormProfileData p = create(new Roles[] {Roles.ROLE_CIVIFORM_ADMIN});
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

  public Optional<ApiKey> retrieveApiKey(String keyId) {
    return syncCacheApi.get(keyId);
  }

  /* One admin can have multiple roles; they can be both a program admin and a civiform admin. */
  private CiviFormProfileData create(Roles[] roleList) {
    CiviFormProfileData p = new CiviFormProfileData();
    p.init(dbContext);
    for (Roles role : roleList) {
      p.addRole(role.toString());
    }
    return p;
  }

  public CiviFormProfile wrap(Account account) {
    return wrapProfileData(new CiviFormProfileData(account.id));
  }

  public CiviFormProfile wrap(Applicant applicant) {
    return wrapProfileData(new CiviFormProfileData(applicant.getAccount().id));
  }

  public CiviFormProfileData createNewProgramAdmin() {
    return create(new Roles[] {Roles.ROLE_PROGRAM_ADMIN});
  }

  /**
   * This creates a program admin who is automatically the admin of all programs currently live,
   * with a fake email address.
   */
  public CiviFormProfileData createFakeProgramAdmin() {
    CiviFormProfileData p = create(new Roles[] {Roles.ROLE_PROGRAM_ADMIN});
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

  /**
   * This creates an admin who is both a civiform admin and a program admin of all currently live
   * programs with a fake email address.
   */
  public CiviFormProfileData createFakeDualAdmin() {
    CiviFormProfileData p =
        create(new Roles[] {Roles.ROLE_PROGRAM_ADMIN, Roles.ROLE_CIVIFORM_ADMIN});
    wrapProfileData(p)
        .getAccount()
        .thenAccept(
            account -> {
              account.setGlobalAdmin(true);
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
