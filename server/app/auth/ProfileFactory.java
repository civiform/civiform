package auth;

import com.google.common.base.Preconditions;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;
import models.Account;
import models.ApiKey;
import models.Applicant;
import play.libs.concurrent.HttpExecutionContext;
import repository.DatabaseExecutionContext;
import repository.VersionRepository;
import services.apikey.ApiKeyService;

/**
 * This class helps create {@link CiviFormProfile} and {@link CiviFormProfileData} objects for
 * existing and new accounts. New accounts are persisted in database.
 */
public class ProfileFactory {

  public static final String FAKE_ADMIN_AUTHORITY_ID = "fake-admin";
  private DatabaseExecutionContext dbContext;
  private HttpExecutionContext httpContext;
  private Provider<VersionRepository> versionRepositoryProvider;
  private Provider<ApiKeyService> apiKeyService;

  @Inject
  public ProfileFactory(
      DatabaseExecutionContext dbContext,
      HttpExecutionContext httpContext,
      Provider<VersionRepository> versionRepositoryProvider,
      Provider<ApiKeyService> apiKeyService) {
    this.dbContext = Preconditions.checkNotNull(dbContext);
    this.httpContext = Preconditions.checkNotNull(httpContext);
    this.versionRepositoryProvider = Preconditions.checkNotNull(versionRepositoryProvider);
    this.apiKeyService = Preconditions.checkNotNull(apiKeyService);
  }

  public CiviFormProfileData createNewApplicant() {
    return create(new Roles[] {Roles.ROLE_APPLICANT});
  }

  public CiviFormProfileData createNewAdmin() {
    return createNewAdmin(Optional.empty());
  }

  public CiviFormProfileData createNewFakeAdmin() {
    return createNewAdmin(Optional.of(FAKE_ADMIN_AUTHORITY_ID));
  }

  public CiviFormProfileData createNewAdmin(Optional<String> maybeAuthorityId) {
    CiviFormProfileData profileData = create(new Roles[] {Roles.ROLE_CIVIFORM_ADMIN});

    wrapProfileData(profileData)
        .getAccount()
        .thenAccept(
            account -> {
              account.setGlobalAdmin(true);
              maybeAuthorityId.ifPresent(account::setAuthorityId);
              account.save();
            })
        .join();

    return profileData;
  }

  public CiviFormProfile wrapProfileData(CiviFormProfileData p) {
    return new CiviFormProfile(dbContext, httpContext, p);
  }

  /**
   * Retrieves an API key. API keys are effectively the profile (i.e. record of identity and
   * authority) for API requests.
   */
  public ApiKey retrieveApiKey(String keyId) {
    Optional<ApiKey> apiKey = apiKeyService.get().findByKeyIdWithCache(keyId);

    return apiKey.orElseThrow(() -> new AccountNonexistentException("API key does not exist"));
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
              account.setAuthorityId(FAKE_ADMIN_AUTHORITY_ID);
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
              account.setAuthorityId(FAKE_ADMIN_AUTHORITY_ID);
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
