package auth;

import static java.util.Comparator.comparing;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.controllers.MissingOptionalException;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import javax.persistence.EntityNotFoundException;
import models.AccountModel;
import models.ApplicantModel;
import play.libs.concurrent.ClassLoaderExecutionContext;
import play.mvc.Http.Request;
import repository.AccountRepository;
import repository.DatabaseExecutionContext;
import services.settings.SettingsManifest;

// NON_ABSTRACT_CLASS_ALLOWS_SUBCLASSING CiviFormProfile

/**
 * This is a "pure" wrapper of CiviFormProfileData. Since CiviFormProfileData is the serialized data
 * about a profile, this class should not store any data that should be serialized. It should
 * contain only server-local information, like execution contexts, database connections, etc.
 */
public class CiviFormProfile {
  private final DatabaseExecutionContext dbContext;
  private final ClassLoaderExecutionContext classLoaderExecutionContext;
  private final CiviFormProfileData profileData;
  private final SettingsManifest settingsManifest;
  private final AccountRepository accountRepository;

  public CiviFormProfile(
      DatabaseExecutionContext dbContext,
      ClassLoaderExecutionContext classLoaderExecutionContext,
      CiviFormProfileData profileData,
      SettingsManifest settingsManifest,
      AccountRepository accountRepository) {
    this.dbContext = Preconditions.checkNotNull(dbContext);
    this.classLoaderExecutionContext = Preconditions.checkNotNull(classLoaderExecutionContext);
    this.profileData = Preconditions.checkNotNull(profileData);
    this.settingsManifest = Preconditions.checkNotNull(settingsManifest);
    this.accountRepository = Preconditions.checkNotNull(accountRepository);
  }

  /** Get the latest {@link ApplicantModel} associated with the profile. */
  public CompletableFuture<ApplicantModel> getApplicant() {
    if (profileData.containsAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME)) {
      long applicantId =
          profileData.getAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, Long.class);
      return accountRepository
          .lookupApplicant(applicantId)
          .thenApply(
              optionalApplicant -> {
                return optionalApplicant.orElseThrow(
                    () -> new MissingOptionalException(ApplicantModel.class));
              })
          .toCompletableFuture();
    }

    // If the applicant id has not yet been stored in the profile, then get it from the account,
    // which requires an extra db fetch.
    return this.getAccount()
        .thenApplyAsync(
            (account) ->
                getApplicantForAccount(account)
                    .orElseThrow(() -> new MissingOptionalException(ApplicantModel.class)),
            classLoaderExecutionContext.current());
  }

  private Optional<ApplicantModel> getApplicantForAccount(AccountModel account) {
    // Accounts (should) correspond to a single applicant.
    return account.getApplicants().stream().min(comparing(ApplicantModel::getWhenCreated));
  }

  /** Look up the {@link AccountModel} associated with the profile from database. */
  public CompletableFuture<AccountModel> getAccount() {
    return supplyAsync(
        () -> {
          AccountModel account = new AccountModel();
          account.id = Long.valueOf(this.profileData.getId());
          try {
            account.refresh();
          } catch (EntityNotFoundException e) {
            throw new AccountNonexistentException(e.getMessage());
          }
          return account;
        },
        dbContext);
  }

  /**
   * Get the client name associated with the profile.
   *
   * <p>This is the class name of pac4j client used to create this profile.
   */
  public String getClientName() {
    return profileData.getClientName();
  }

  /**
   * Get the roles associated with the profile.
   *
   * <p>The roles are decided on sign-in and not persisted in database.
   */
  public Set<String> getRoles() {
    return profileData.getRoles();
  }

  /** Returns true if the profile has TI role. */
  public boolean isTrustedIntermediary() {
    return getRoles().contains(Role.ROLE_TI.toString());
  }

  /** Returns true if the profile has CiviForm Admin role. */
  public boolean isCiviFormAdmin() {
    return profileData.getRoles().contains(Role.ROLE_CIVIFORM_ADMIN.toString());
  }

  /** Returns true if the profile has Program Admin role. */
  public boolean isProgramAdmin() {
    return this.getRoles().contains(Role.ROLE_PROGRAM_ADMIN.toString());
  }

  /** Returns true if the profile has the Program Admin role AND NOT the CiviForm admin role. */
  public boolean isOnlyProgramAdmin() {
    return isProgramAdmin() && !isCiviFormAdmin();
  }

  /** Returns the account ID associated with the profile. */
  public String getId() {
    return profileData.getId();
  }

  /**
   * Sets the authority id for the associated {@link AccountModel} if none is set.
   *
   * <p>If an id is already present this may only be called with the same exact ID.
   *
   * @param authorityId ID that uniquely identifies the user of the Account.
   * @return the future of the database operation.
   */
  public CompletableFuture<Void> setAuthorityId(String authorityId) {
    return this.getAccount()
        .thenApplyAsync(
            a -> {
              Optional<String> existingAuthorityId = Optional.ofNullable(a.getAuthorityId());
              // The authority id can never change once set.
              if (existingAuthorityId.isPresent()
                  && !existingAuthorityId.get().equals(authorityId)) {
                throw new ProfileMergeConflictException(
                    String.format(
                        "Profile already contains an authority ID: %s - which is different from the"
                            + " new authority ID address %s. If the 'iss' values of the authority"
                            + " ID are different it is likely that the email address was used to"
                            + " create an account using one authentication system (applicant/admin)"
                            + " and the new authority was created in the other (applicant/admin)."
                            + " See https://docs.civiform.us/it-manual/sre-playbook/troubleshooting-production#errors-related-to-authority-id",
                        existingAuthorityId, authorityId));
              }
              a.setAuthorityId(authorityId);
              a.save();
              return null;
            },
            dbContext);
  }

  /**
   * Set email address for the associated {@link AccountModel} if none is set.
   *
   * <p>If email address is present and different from the address to be set, a
   * `CompletionException` is thrown caused by a `ProfileMergeConflictException`.
   *
   * @param emailAddress email address to be set for the account
   * @return the future of the database operation
   */
  public CompletableFuture<Void> setEmailAddress(String emailAddress) {
    return this.getAccount()
        .thenApplyAsync(
            a -> {
              String existingEmail = a.getEmailAddress();

              if (existingEmail != null && !existingEmail.equals(emailAddress)) {
                throw new ProfileMergeConflictException(
                    String.format(
                        "Profile already contains an email address: %s - which is different from"
                            + " the new email address %s.",
                        existingEmail, emailAddress));
              }

              a.setEmailAddress(emailAddress);
              a.save();
              profileData.setEmail(emailAddress);

              return null;
            },
            dbContext);
  }

  /** Returns the authority id from the {@link AccountModel} associated with the profile. */
  public CompletableFuture<String> getAuthorityId() {
    return this.getAccount()
        .thenApplyAsync(AccountModel::getAuthorityId, classLoaderExecutionContext.current());
  }

  /**
   * Get the email address from the session's {@link CiviFormProfileData} if present, otherwise get
   * it from the {@link AccountModel} associated with the profile.
   *
   * <p>This value could be null.
   *
   * @return the future of the address to be retrieved.
   */
  public CompletableFuture<String> getEmailAddress() {
    // Email address should be present in the profile for authenticated users
    if (profileData.hasCanonicalEmail()) {
      return completedFuture(profileData.getEmail());
    }

    // If it's not present i.e. if user is a guest, fall back to the address in the database
    return this.getAccount()
        .thenApplyAsync(AccountModel::getEmailAddress, classLoaderExecutionContext.current());
  }

  /** Get the profile data. */
  public CiviFormProfileData getProfileData() {
    return this.profileData;
  }

  /**
   * Check if the profile is authorized to access the applicant's data.
   *
   * <p>If the check does not pass, a `CompletionException` is thrown caused by a
   * `SecurityException`.
   *
   * @param applicantId id of the applicant whose data is requested to be accessed
   * @return the future of the check
   */
  public CompletableFuture<Void> checkAuthorization(long applicantId) {
    return getAccount()
        .thenApplyAsync(
            account ->
                Stream.concat(
                        account
                            .getMemberOfGroup()
                            .flatMap(tiGroup -> Optional.of(tiGroup.getManagedAccounts().stream()))
                            .orElse(Stream.of()),
                        Stream.of(account))
                    .map(AccountModel::ownedApplicantIds)
                    .reduce(
                        ImmutableList.of(),
                        (one, two) ->
                            new ImmutableList.Builder<Long>().addAll(one).addAll(two).build()))
        .thenApplyAsync(
            idList -> {
              if (!idList.contains(applicantId)) {
                throw new SecurityException(
                    String.format(
                        "Account %s is not authorized to access applicant %d",
                        getId(), applicantId));
              }
              return null;
            });
  }

  /**
   * Check if the profile is authorized to access all applications of the program.
   *
   * <p>If the check does not pass, a `CompletionException` is thrown caused by a
   * `SecurityException`.
   *
   * @param programName name of the program whose data is requested to be accessed
   * @return the future of the check
   */
  public CompletableFuture<Void> checkProgramAuthorization(String programName, Request request) {
    return this.getAccount()
        .thenApply(
            account -> {
              if (account.getGlobalAdmin()
                  && settingsManifest.getAllowCiviformAdminAccessPrograms(request)) {
                return null;
              }
              if (account.getAdministeredProgramNames().stream()
                  .anyMatch(program -> program.equals(programName))) {
                return null;
              }
              throw new SecurityException(
                  String.format(
                      "Account %s is not authorized to access program %s.", getId(), programName));
            });
  }

  /**
   * Stores applicant id in user profile.
   *
   * <p>This allows us to know the applicant id instead of having to specify it in the URL path, or
   * looking up the account each time and finding the corresponding applicant id.
   */
  void storeApplicantIdInProfile(Long applicantId) {
    if (!profileData.containsAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME)) {
      profileData.addAttribute(ProfileFactory.APPLICANT_ID_ATTRIBUTE_NAME, applicantId);
    }
  }

  /**
   * Stores applicant id corresponding to this account in user profile.
   *
   * <p>This allows us to know the applicant id instead of having to specify it in the URL path, or
   * looking up the account each time and finding the corresponding applicant id.
   */
  void storeApplicantIdInProfile(AccountModel account) {
    Long applicantId =
        getApplicantForAccount(account)
            .orElseThrow(() -> new MissingOptionalException(ApplicantModel.class))
            .id;
    storeApplicantIdInProfile(applicantId);
  }
}
