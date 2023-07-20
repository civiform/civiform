package auth;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Provider;
import models.Account;
import models.ApiKey;
import models.Applicant;
import models.TrustedIntermediaryGroup;
import org.apache.commons.lang3.RandomStringUtils;
import play.libs.concurrent.HttpExecutionContext;
import repository.DatabaseExecutionContext;
import repository.UserRepository;
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
  private final Provider<UserRepository> userRepositoryProvider;
  private final SettingsManifest settingsManifest;

  @Inject
  public ProfileFactory(
      DatabaseExecutionContext dbContext,
      HttpExecutionContext httpContext,
      Provider<VersionRepository> versionRepositoryProvider,
      Provider<ApiKeyService> apiKeyService,
      Provider<UserRepository> userRepositoryProvider,
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
  public ApiKey retrieveApiKey(String keyId) {
    Optional<ApiKey> apiKey = apiKeyService.get().findByKeyIdWithCache(keyId);

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

  public CiviFormProfile wrap(Account account) {
    return wrapProfileData(new CiviFormProfileData(account.id));
  }

  public CiviFormProfile wrap(Applicant applicant) {
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
                  .getActiveVersion()
                  .getPrograms()
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

  /** This creates a trusted intermediary. */
  public CiviFormProfileData createFakeTrustedIntermediary() {
    UserRepository userRepository = userRepositoryProvider.get();
    List<TrustedIntermediaryGroup> existingGroups = userRepository.listTrustedIntermediaryGroups();
    TrustedIntermediaryGroup group;

    if (existingGroups.isEmpty()) {
      group = userRepository.createNewTrustedIntermediaryGroup("Test group", "Created for testing");
    } else {
      group = existingGroups.get(0);
    }

    CiviFormProfileData tiProfileData = create(new Role[] {Role.ROLE_TI});
    CiviFormProfile tiProfile = wrapProfileData(tiProfileData);
    tiProfile
        .getAccount()
        .thenAccept(
            account -> {
              account.setAuthorityId(generateFakeAdminAuthorityId());
              // The email must be unique in order to insert into the database.
              String email =
                  String.format("fake-trusted-intermediary-%s@example.com", tiProfile.getId());
              account.setEmailAddress(email);
              account.save();
              userRepository.addTrustedIntermediaryToGroup(group.id, email);
            })
        .join();

    Applicant tiApplicant = tiProfile.getApplicant().join();
    // The name for a fake TI must not be unique so that screenshot tests stay consistent. Use an
    // underscore so that the name parser doesn't display "TI, Fake".
    tiApplicant.getApplicantData().setUserName("Fake_TI");
    userRepository.updateApplicant(tiApplicant);

    return tiProfileData;
  }

  private static String generateFakeAdminAuthorityId() {
    return FAKE_ADMIN_AUTHORITY_ID
        + "-"
        + RandomStringUtils.random(12, /* letters= */ true, /* numbers= */ true);
  }
}
