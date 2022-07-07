package auth;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.persistence.EntityNotFoundException;
import models.Account;
import models.Applicant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.concurrent.HttpExecutionContext;
import repository.DatabaseExecutionContext;

/**
 * This is a "pure" wrapper of CiviFormProfileData. Since CiviFormProfileData is the serialized data
 * about a profile, this class should not store any data that should be serialized. It should
 * contain only server-local information, like execution contexts, database connections, etc.
 */
public class CiviFormProfile {

  private static final Logger logger = LoggerFactory.getLogger(OidcProfileAdapter.class);
  private DatabaseExecutionContext dbContext;
  private HttpExecutionContext httpContext;
  private CiviFormProfileData profileData;

  @Inject
  public CiviFormProfile(
      DatabaseExecutionContext dbContext,
      HttpExecutionContext httpContext,
      CiviFormProfileData profileData) {
    this.dbContext = Preconditions.checkNotNull(dbContext);
    this.httpContext = Preconditions.checkNotNull(httpContext);
    this.profileData = Preconditions.checkNotNull(profileData);
  }

  /** Get the latest {@link Applicant} associated with the profile. */
  public CompletableFuture<Applicant> getApplicant() {
    return this.getAccount()
        .thenApplyAsync(
            (a) ->
                a.getApplicants().stream()
                    .sorted(Comparator.comparing((applicant) -> applicant.getWhenCreated()))
                    .findFirst()
                    .orElseThrow(),
            httpContext.current());
  }

  /** Look up the {@link Account} associated with the profile from database. */
  public CompletableFuture<Account> getAccount() {
    return supplyAsync(
        () -> {
          Account account = new Account();
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
    return getRoles().contains(Roles.ROLE_TI.toString());
  }

  /** Returns true if the profile has CiviForm Admin role. */
  public boolean isCiviFormAdmin() {
    return profileData.getRoles().contains(Roles.ROLE_CIVIFORM_ADMIN.toString());
  }

  /** Returns true if the profile has Program Admin role. */
  public boolean isProgramAdmin() {
    return this.getRoles().contains(Roles.ROLE_PROGRAM_ADMIN.toString());
  }

  /** Returns the account ID associated with the profile. */
  public String getId() {
    return profileData.getId();
  }

  /**
   * Sets the authority id for the associated {@link Account} if none is set.
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
                var e =
                    new ProfileMergeConflictException(
                        String.format(
                            "Profile already contains an authority ID: %s - which is different from"
                                + " the new authority ID address %s.",
                            existingAuthorityId, authorityId));
                logger.error(e.getMessage());
                throw e;
              }

              a.setAuthorityId(authorityId);
              a.save();
              return null;
            },
            dbContext);
  }

  /**
   * Set email address for the associated {@link Account} if none is set.
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
                var e = new ProfileMergeConflictException(
                    String.format(
                        "Profile already contains an email address: %s - which is different from"
                            + " the new email address %s.",
                        existingEmail, emailAddress));
                logger.error(e.getMessage());
                throw e;
              }
              a.setEmailAddress(emailAddress);
              a.save();
              return null;
            },
            dbContext);
  }

  /** Returns the authority id from the {@link Account} associated with the profile. */
  public CompletableFuture<String> getAuthorityId() {
    return this.getAccount().thenApplyAsync(Account::getAuthorityId, httpContext.current());
  }

  /**
   * Get the email address from the {@link Account} associated with the profile.
   *
   * <p>This value could be null.
   *
   * @return the future of the address to be retrieved.
   */
  public CompletableFuture<String> getEmailAddress() {
    return this.getAccount().thenApplyAsync(Account::getEmailAddress, httpContext.current());
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
                    .map(Account::ownedApplicantIds)
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
  public CompletableFuture<Void> checkProgramAuthorization(String programName) {
    return this.getAccount()
        .thenApply(
            account -> {
              if (account.getAdministeredProgramNames().stream()
                  .anyMatch(program -> program.equals(programName))) {
                return null;
              }
              throw new SecurityException(
                  String.format(
                      "Account %s is not authorized to access program %s.", getId(), programName));
            });
  }
}
