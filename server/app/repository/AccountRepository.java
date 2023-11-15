package repository;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.CiviFormProfile;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import forms.AddApplicantToTrustedIntermediaryGroupForm;
import forms.EditTiClientInfoForm;
import io.ebean.DB;
import io.ebean.Database;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Account;
import models.Applicant;
import models.TrustedIntermediaryGroup;
import play.api.data.Form;
import services.CiviFormError;
import services.applicant.ApplicantData;
import services.program.ProgramDefinition;
import services.ti.EmailAddressExistsException;
import services.ti.NoSuchTrustedIntermediaryError;
import services.ti.NoSuchTrustedIntermediaryGroupError;
import services.ti.NotEligibleToBecomeTiError;

/** AccountRepository contains database interactions for {@link Account} and {@link Applicant}. */
public final class AccountRepository {

  private final Database database;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public AccountRepository(DatabaseExecutionContext executionContext) {
    this.database = DB.getDefault();
    this.executionContext = checkNotNull(executionContext);
  }

  public CompletionStage<Set<Applicant>> listApplicants() {
    return supplyAsync(() -> database.find(Applicant.class).findSet(), executionContext);
  }

  public CompletionStage<Optional<Applicant>> lookupApplicant(long id) {
    return supplyAsync(
        () -> database.find(Applicant.class).setId(id).findOneOrEmpty(), executionContext);
  }

  public Optional<Account> lookupAccountByAuthorityId(String authorityId) {
    checkNotNull(authorityId);
    checkArgument(!authorityId.isEmpty());
    return database.find(Account.class).where().eq("authority_id", authorityId).findOneOrEmpty();
  }

  public Optional<Account> lookupAccountByEmail(String emailAddress) {
    checkNotNull(emailAddress);
    checkArgument(!emailAddress.isEmpty());
    return database.find(Account.class).where().eq("email_address", emailAddress).findOneOrEmpty();
  }

  public CompletionStage<Optional<Account>> lookupAccountByEmailAsync(String emailAddress) {
    if (emailAddress == null) {
      return CompletableFuture.failedStage(new NullPointerException());
    }
    if (emailAddress.isEmpty()) {
      return CompletableFuture.failedStage(new IllegalArgumentException());
    }
    return supplyAsync(
        () ->
            database.find(Account.class).where().eq("email_address", emailAddress).findOneOrEmpty(),
        executionContext);
  }

  /**
   * Returns the most recent Applicant identified by Account, creating one if necessary.
   *
   * <p>If no applicant exists, this is probably an account waiting for a trusted intermediary, so
   * we create one.
   */
  private Applicant getOrCreateApplicant(Account account) {
    Optional<Applicant> applicantOpt =
        account.getApplicants().stream().max(Comparator.comparing(Applicant::getWhenCreated));
    return applicantOpt.orElseGet(() -> new Applicant().setAccount(account).saveAndReturn());
  }

  public CompletionStage<Optional<Applicant>> lookupApplicantByAuthorityId(String authorityId) {
    return supplyAsync(
        () -> lookupAccountByAuthorityId(authorityId).map(this::getOrCreateApplicant),
        executionContext);
  }

  public CompletionStage<Optional<Applicant>> lookupApplicantByEmail(String emailAddress) {
    return supplyAsync(
        () -> lookupAccountByEmail(emailAddress).map(this::getOrCreateApplicant), executionContext);
  }

  public CompletionStage<Void> insertApplicant(Applicant applicant) {
    return supplyAsync(
        () -> {
          database.insert(applicant);
          return null;
        },
        executionContext);
  }

  public CompletionStage<Void> updateApplicant(Applicant applicant) {
    return supplyAsync(
        () -> {
          database.update(applicant);
          return null;
        },
        executionContext);
  }

  public Optional<Applicant> lookupApplicantSync(long id) {
    return database.find(Applicant.class).setId(id).findOneOrEmpty();
  }

  /** Merge the older applicant data into the newer applicant, and set both to the given account. */
  public CompletionStage<Applicant> mergeApplicants(
      Applicant left, Applicant right, Account account) {
    return supplyAsync(
        () -> {
          left.setAccount(account).save();
          right.setAccount(account).save();
          return mergeApplicants(left, right).saveAndReturn();
        },
        executionContext);
  }

  /** Merge the applicant data from older applicant into the newer applicant. */
  private Applicant mergeApplicants(Applicant left, Applicant right) {
    if (left.getWhenCreated().isAfter(right.getWhenCreated())) {
      Applicant tmp = left;
      left = right;
      right = tmp;
    }
    // At this point, "left" is older, "right" is newer, we will merge "left" into "right", because
    // the newer applicant is always preferred when more than one applicant matches an account.
    right.getApplicantData().mergeFrom(left.getApplicantData());
    return right;
  }

  public List<TrustedIntermediaryGroup> listTrustedIntermediaryGroups() {
    return database.find(TrustedIntermediaryGroup.class).findList();
  }

  public TrustedIntermediaryGroup createNewTrustedIntermediaryGroup(
      String name, String description) {
    TrustedIntermediaryGroup tiGroup = new TrustedIntermediaryGroup(name, description);
    tiGroup.save();
    return tiGroup;
  }

  public void deleteTrustedIntermediaryGroup(long id) {
    Optional<TrustedIntermediaryGroup> tiGroup = getTrustedIntermediaryGroup(id);
    if (tiGroup.isEmpty()) {
      throw new NoSuchTrustedIntermediaryGroupError();
    }
    database.delete(tiGroup.get());
  }

  public Optional<TrustedIntermediaryGroup> getTrustedIntermediaryGroup(long id) {
    return database.find(TrustedIntermediaryGroup.class).setId(id).findOneOrEmpty();
  }

  /**
   * Adds the given email address to the TI group. If the email address does not correspond to an
   * existing account, then create an account and associate it, so it will be ready when the TI
   * signs in for the first time.
   */
  public void addTrustedIntermediaryToGroup(long id, String emailAddress) {
    Optional<TrustedIntermediaryGroup> tiGroup = getTrustedIntermediaryGroup(id);
    if (tiGroup.isEmpty()) {
      throw new NoSuchTrustedIntermediaryGroupError();
    }
    Optional<Account> accountMaybe = lookupAccountByEmail(emailAddress);
    Account account =
        accountMaybe.orElseGet(
            () -> {
              Account a = new Account();
              a.setEmailAddress(emailAddress);
              a.save();
              return a;
            });

    if (account.getGlobalAdmin() || !account.getAdministeredProgramNames().isEmpty()) {
      throw new NotEligibleToBecomeTiError();
    }

    account.setMemberOfGroup(tiGroup.get());
    account.save();
  }

  public void removeTrustedIntermediaryFromGroup(long id, long accountId) {
    Optional<TrustedIntermediaryGroup> tiGroup = getTrustedIntermediaryGroup(id);
    if (tiGroup.isEmpty()) {
      throw new NoSuchTrustedIntermediaryGroupError();
    }
    Optional<Account> accountMaybe = lookupAccount(accountId);
    if (accountMaybe.isEmpty()) {
      throw new NoSuchTrustedIntermediaryError();
    }
    Account account = accountMaybe.get();
    if (account.getMemberOfGroup().isPresent()
        && account.getMemberOfGroup().get().equals(tiGroup.get())) {
      account.setMemberOfGroup(null);
      account.save();
    } else {
      throw new NoSuchTrustedIntermediaryError();
    }
  }

  private Optional<Account> lookupAccount(long accountId) {
    return database.find(Account.class).setId(accountId).findOneOrEmpty();
  }

  public Optional<TrustedIntermediaryGroup> getTrustedIntermediaryGroup(
      CiviFormProfile civiformProfile) {
    return civiformProfile.getAccount().join().getMemberOfGroup();
  }

  /**
   * Create an applicant and add it to the provided trusted intermediary group. Associate it with an
   * email address if one is provided, but if one is not provided, use an anonymous (guest-style)
   * account.
   *
   * @throws EmailAddressExistsException if the provided email address already exists.
   */
  public void createNewApplicantForTrustedIntermediaryGroup(
      AddApplicantToTrustedIntermediaryGroupForm form, TrustedIntermediaryGroup tiGroup) {
    Account newAccount = new Account();
    if (!Strings.isNullOrEmpty(form.getEmailAddress())) {
      if (lookupAccountByEmail(form.getEmailAddress()).isPresent()) {
        throw new EmailAddressExistsException();
      }
      newAccount.setEmailAddress(form.getEmailAddress());
    }
    newAccount.setManagedByGroup(tiGroup);
    newAccount.save();
    Applicant applicant = new Applicant();
    applicant.setAccount(newAccount);
    ApplicantData applicantData = applicant.getApplicantData();
    applicantData.setUserName(form.getFirstName(), form.getMiddleName(), form.getLastName());
    applicantData.setDateOfBirth(form.getDob());
    applicant.save();
  }

  public void updateApplicantInfoForTrustedIntermediaryGroup(Form<EditTiClientInfoForm> form, Applicant applicant) {
    EditTiClientInfoForm theForm = form.get();
    ApplicantData applicantData = applicant.getApplicantData();
    applicantData.setUserName(theForm.getFirstName(), theForm.getMiddleName(), theForm.getLastName());
    applicantData.setDateOfBirth(theForm.getDob());
    applicantData.setNote(theForm.getNote());
    applicant.save();
  }


  /**
   * Adds the given program as an administered program by the given account. If the account does not
   * exist, this will return an error.
   *
   * @param accountEmail the email of the account that will administer the given program
   * @param program the {@link ProgramDefinition} to add to this given account
   */
  public Optional<CiviFormError> addAdministeredProgram(
      String accountEmail, ProgramDefinition program) {
    if (accountEmail.isBlank()) {
      return Optional.empty();
    }

    Optional<Account> maybeAccount = lookupAccountByEmail(accountEmail);
    if (maybeAccount.isEmpty()) {
      return Optional.of(
          CiviFormError.of(
              String.format(
                  "Cannot add %s as a Program Admin because they do not have an admin account."
                      + " Have the user log in as admin on the home page, then they can be added"
                      + " as a Program Admin.",
                  accountEmail)));
    } else {
      maybeAccount.ifPresent(
          account -> {
            account.addAdministeredProgram(program);
            account.save();
          });
      return Optional.empty();
    }
  }

  /**
   * If the account identified by the given email administers the given program, remove the program
   * from the list of programs this account administers.
   *
   * @param accountEmail the email of the account
   * @param program the {@link ProgramDefinition} to remove from the given account
   */
  public void removeAdministeredProgram(String accountEmail, ProgramDefinition program) {
    Optional<Account> maybeAccount = lookupAccountByEmail(accountEmail);
    maybeAccount.ifPresent(
        account -> {
          account.removeAdministeredProgram(program);
          account.save();
        });
  }

  public ImmutableSet<Account> getGlobalAdmins() {
    return ImmutableSet.copyOf(
        database.find(Account.class).where().eq("global_admin", true).findList());
  }

  @VisibleForTesting
  public ImmutableSet<Account> listAccounts() {
    return ImmutableSet.copyOf(database.find(Account.class).findSet());
  }

  /** Delete guest accounts that have no data and were created before the provided maximum age. */
  public int deleteUnusedGuestAccounts(int minAgeInDays) {
    String sql =
        "WITH unused_accounts AS ( "
            + "  SELECT applicants.account_id AS account_id, applicants.id AS applicant_id "
            + "  FROM applicants"
            + "  LEFT JOIN applications ON applicants.id = applications.applicant_id "
            + "  LEFT JOIN accounts ON accounts.id = applicants.account_id "
            + "  WHERE applications.applicant_id IS NULL "
            + "  AND accounts.authority_id IS NULL "
            + "  AND applicants.when_created < CURRENT_DATE - INTERVAL '"
            + minAgeInDays
            + " days' "
            + "), "
            + "applicants_deleted AS ("
            + "  DELETE FROM applicants"
            + "  WHERE applicants.id IN (SELECT applicant_id FROM unused_accounts) "
            + ") "
            + "DELETE FROM accounts "
            + "WHERE accounts.id IN (SELECT account_id FROM unused_accounts);";

    return database.sqlUpdate(sql).execute();
  }
}
