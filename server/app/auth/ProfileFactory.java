package auth;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;
import models.AccountModel;
import models.ApiKeyModel;
import models.ApplicantModel;
import models.TrustedIntermediaryGroupModel;
import org.apache.commons.lang3.RandomStringUtils;
import play.libs.concurrent.HttpExecutionContext;
import repository.AccountRepository;
import repository.DatabaseExecutionContext;
import repository.VersionRepository;
import services.apikey.ApiKeyService;
import services.settings.SettingsManifest;

/**
 * This class helps create {@link CiviFormProfile} and {@link CiviFormProfileData} objects for
 * existing and new accounts. New accounts are persisted in database.
 */
public final class ProfileFactory {

  public static final String FAKE_ADMIN_AUTHORITY_ID = "fake-admin";
  private final DatabaseExecutionContext dbContext;
  private final HttpExecutionContext httpContext;
  private final Provider<VersionRepository> versionRepositoryProvider;
  private final Provider<ApiKeyService> apiKeyService;
  private final Provider<AccountRepository> userRepositoryProvider;
  private final SettingsManifest settingsManifest;

  @Inject
  public ProfileFactory(
      DatabaseExecutionContext dbContext,
      HttpExecutionContext httpContext,
      Provider<VersionRepository> versionRepositoryProvider,
      Provider<ApiKeyService> apiKeyService,
      Provider<AccountRepository> userRepositoryProvider,
      SettingsManifest settingsManifest) {
    this.dbContext = Preconditions.checkNotNull(dbContext);
    this.httpContext = Preconditions.checkNotNull(httpContext);
    this.versionRepositoryProvider = Preconditions.checkNotNull(versionRepositoryProvider);
    this.apiKeyService = Preconditions.checkNotNull(apiKeyService);
    this.userRepositoryProvider = Preconditions.checkNotNull(userRepositoryProvider);
    this.settingsManifest = Preconditions.checkNotNull(settingsManifest);
  }

  public CiviFormProfileData createNewApplicant() {
    return create(new Role[] {Role.ROLE_APPLICANT});
  }

  public CiviFormProfileData createNewAdmin() {
    return createNewAdmin(Optional.empty());
  }

  public CiviFormProfileData createNewFakeAdmin() {
    return createNewAdmin(Optional.of(generateFakeAdminAuthorityId()));
  }

  public CiviFormProfileData createNewAdmin(Optional<String> maybeAuthorityId) {
    CiviFormProfileData profileData = create(new Role[] {Role.ROLE_CIVIFORM_ADMIN});

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
    return new CiviFormProfile(dbContext, httpContext, p, settingsManifest);
  }

  /**
   * Retrieves an API key. API keys are effectively the profile (i.e. record of identity and
   * authority) for API requests.
   */
  public ApiKeyModel retrieveApiKey(String keyId) {
    Optional<ApiKeyModel> apiKey = apiKeyService.get().findByKeyIdWithCache(keyId);

    return apiKey.orElseThrow(() -> new AccountNonexistentException("API key does not exist"));
  }

  /* One admin can have multiple roles; they can be both a program admin and a civiform admin. */
  private CiviFormProfileData create(Role[] roleList) {
    CiviFormProfileData p = new CiviFormProfileData();
    p.init(dbContext);
    for (Role role : roleList) {
      p.addRole(role.toString());
    }
    return p;
  }

  @VisibleForTesting
  public CiviFormProfile wrap(AccountModel account) {
    return wrapProfileData(new CiviFormProfileData(account.id));
  }

  public CiviFormProfile wrap(ApplicantModel applicant) {
    return wrapProfileData(new CiviFormProfileData(applicant.getAccount().id));
  }

  public CiviFormProfileData createNewProgramAdmin() {
    return create(new Role[] {Role.ROLE_PROGRAM_ADMIN});
  }

  /**
   * This creates a program admin who is automatically the admin of all programs currently live,
   * with a fake email address.
   */
  public CiviFormProfileData createFakeProgramAdmin() {
    CiviFormProfileData p = create(new Role[] {Role.ROLE_PROGRAM_ADMIN});
    wrapProfileData(p)
        .getAccount()
        .thenAccept(
            account -> {
              versionRepositoryProvider
                  .get()
                  .getProgramsForVersion(versionRepositoryProvider.get().getActiveVersion())
                  .forEach(
                      program -> account.addAdministeredProgram(program.getProgramDefinition()));
              account.setEmailAddress(String.format("fake-local-admin-%d@example.com", account.id));
              account.setAuthorityId(generateFakeAdminAuthorityId());
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
    CiviFormProfileData p = create(new Role[] {Role.ROLE_PROGRAM_ADMIN, Role.ROLE_CIVIFORM_ADMIN});
    wrapProfileData(p)
        .getAccount()
        .thenAccept(
            account -> {
              account.setGlobalAdmin(true);
              account.setAuthorityId(generateFakeAdminAuthorityId());
              versionRepositoryProvider
                  .get()
                  .getProgramsForVersion(versionRepositoryProvider.get().getActiveVersion())
                  .forEach(
                      program -> account.addAdministeredProgram(program.getProgramDefinition()));
              account.setEmailAddress(String.format("fake-local-admin-%d@example.com", account.id));
              account.save();
            })
        .join();
    return p;
  }

  /** This creates a trusted intermediary. */
  public CiviFormProfileData createFakeTrustedIntermediary() {
    AccountRepository accountRepository = userRepositoryProvider.get();
    List<TrustedIntermediaryGroupModel> existingGroups =
        accountRepository.listTrustedIntermediaryGroups();
    TrustedIntermediaryGroupModel group;

    if (existingGroups.isEmpty()) {
      group =
          accountRepository.createNewTrustedIntermediaryGroup("Test group", "Created for testing");
    } else {
      group = existingGroups.get(0);
    }

    CiviFormProfileData tiProfileData = create(new Role[] {Role.ROLE_TI});
    // The email must be unique in order to insert into the database.
    // Email must be use example.com for TI browser tests to pass, see
    // ApplicantLayout.tiEmailForDisplay().
    String email = String.format("fake-trusted-intermediary-%s@example.com", tiProfileData.getId());
    tiProfileData.setEmail(email);

    CiviFormProfile tiProfile = wrapProfileData(tiProfileData);
    tiProfile
        .getAccount()
        .thenAccept(
            account -> {
              account.setAuthorityId(generateFakeAdminAuthorityId());
              account.setEmailAddress(email);
              account.save();
              accountRepository.addTrustedIntermediaryToGroup(group.id, email);
            })
        .join();

    ApplicantModel tiApplicant = tiProfile.getApplicant().join();
    // The name for a fake TI must not be unique so that screenshot tests stay consistent. Use an
    // underscore so that the name parser doesn't display "TI, Fake".
    tiApplicant.getApplicantData().setUserName("Fake_TI");
    accountRepository.updateApplicant(tiApplicant);

    return tiProfileData;
  }

  private static String generateFakeAdminAuthorityId() {
    return FAKE_ADMIN_AUTHORITY_ID
        + "-"
        + RandomStringUtils.random(12, /* letters= */ true, /* numbers= */ true);
  }
}
