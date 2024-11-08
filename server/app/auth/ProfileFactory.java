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
import play.libs.concurrent.ClassLoaderExecutionContext;
import repository.AccountRepository;
import repository.DatabaseExecutionContext;
import repository.ProgramRepository;
import repository.VersionRepository;
import services.RandomStringUtils;
import services.apikey.ApiKeyService;
import services.settings.SettingsManifest;

/**
 * This class helps create {@link CiviFormProfile} and {@link CiviFormProfileData} objects for
 * existing and new accounts. New accounts are persisted in database.
 */
public final class ProfileFactory {

  public static final String FAKE_ADMIN_AUTHORITY_ID = "fake-admin";
  public static final String APPLICANT_ID_ATTRIBUTE_NAME = "applicant_id";
  private final DatabaseExecutionContext dbContext;
  private final ClassLoaderExecutionContext classLoaderExecutionContext;
  private final Provider<VersionRepository> versionRepositoryProvider;
  private final Provider<ProgramRepository> programRepositoryProvider;
  private final Provider<ApiKeyService> apiKeyService;
  private final Provider<AccountRepository> accountRepositoryProvider;
  private final SettingsManifest settingsManifest;

  @Inject
  public ProfileFactory(
      DatabaseExecutionContext dbContext,
      ClassLoaderExecutionContext classLoaderExecutionContext,
      Provider<VersionRepository> versionRepositoryProvider,
      Provider<ProgramRepository> programRepositoryProvider,
      Provider<ApiKeyService> apiKeyService,
      Provider<AccountRepository> accountRepositoryProvider,
      SettingsManifest settingsManifest) {
    this.dbContext = Preconditions.checkNotNull(dbContext);
    this.classLoaderExecutionContext = Preconditions.checkNotNull(classLoaderExecutionContext);
    this.versionRepositoryProvider = Preconditions.checkNotNull(versionRepositoryProvider);
    this.programRepositoryProvider = Preconditions.checkNotNull(programRepositoryProvider);
    this.apiKeyService = Preconditions.checkNotNull(apiKeyService);
    this.accountRepositoryProvider = Preconditions.checkNotNull(accountRepositoryProvider);
    this.settingsManifest = Preconditions.checkNotNull(settingsManifest);
  }

  public CiviFormProfileData createNewApplicant() {
    CiviFormProfileData profileData = create(new Role[] {Role.ROLE_APPLICANT});

    // Store the applicant id in the profile.
    //
    // The profile ID corresponds to the *account* id, but controllers need the applicant id. We
    // store it in the profile for easy retrieval without a db lookup.
    CiviFormProfile profile = wrapProfileData(profileData);
    profile.getAccount().thenAccept(account -> profile.storeApplicantIdInProfile(account)).join();

    return profileData;
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
    return new CiviFormProfile(
        dbContext,
        classLoaderExecutionContext,
        p,
        settingsManifest,
        accountRepositoryProvider.get());
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
    CiviFormProfileData profileData = new CiviFormProfileData(applicant.getAccount().id);
    CiviFormProfile profile = wrapProfileData(profileData);
    profile.getAccount().thenAccept(account -> profile.storeApplicantIdInProfile(account)).join();
    return profile;
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
                      program ->
                          account.addAdministeredProgram(
                              programRepositoryProvider
                                  .get()
                                  .getShallowProgramDefinition(program)));
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
                      program ->
                          account.addAdministeredProgram(
                              programRepositoryProvider
                                  .get()
                                  .getShallowProgramDefinition(program)));
              account.setEmailAddress(String.format("fake-local-admin-%d@example.com", account.id));
              account.save();
            })
        .join();
    return p;
  }

  /** This creates a trusted intermediary. */
  public CiviFormProfileData createFakeTrustedIntermediary() {
    AccountRepository accountRepository = accountRepositoryProvider.get();
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
    return FAKE_ADMIN_AUTHORITY_ID + "-" + RandomStringUtils.randomAlphanumeric(12);
  }
}
