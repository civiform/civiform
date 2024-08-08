package repository;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.CiviFormProfile;
import auth.oidc.IdTokens;
import auth.oidc.IdTokensFactory;
import auth.oidc.SerializedIdTokens;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import durablejobs.jobs.MigratePrimaryApplicantInfoJob;
import forms.TiClientInfoForm;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Query;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.AccountModel;
import models.ApplicantModel;
import models.TrustedIntermediaryGroupModel;
import org.pac4j.oidc.profile.OidcProfile;
import services.CiviFormError;
import services.applicant.ApplicantData;
import services.program.ProgramDefinition;
import services.ti.EmailAddressExistsException;
import services.ti.NoSuchTrustedIntermediaryError;
import services.ti.NoSuchTrustedIntermediaryGroupError;
import services.ti.NotEligibleToBecomeTiError;

/**
 * AccountRepository contains database interactions for {@link AccountModel} and {@link
 * ApplicantModel}.
 */
public final class AccountRepository {
  private static final QueryProfileLocationBuilder queryProfileLocationBuilder =
      new QueryProfileLocationBuilder("AccountRepository");

  private final Database database;
  private final DatabaseExecutionContext executionContext;
  private final IdTokensFactory idTokensFactory;

  @Inject
  public AccountRepository(
      DatabaseExecutionContext executionContext, IdTokensFactory idTokensFactory) {
    this.database = DB.getDefault();
    this.executionContext = checkNotNull(executionContext);
    this.idTokensFactory = checkNotNull(idTokensFactory);
  }

  public CompletionStage<Set<ApplicantModel>> listApplicants() {
    return supplyAsync(
        () ->
            database
                .find(ApplicantModel.class)
                .setLabel("ApplicantModel.findSet")
                .setProfileLocation(queryProfileLocationBuilder.create("listApplicants"))
                .findSet(),
        executionContext);
  }

  public CompletionStage<Optional<ApplicantModel>> lookupApplicant(long id) {
    return supplyAsync(
        () ->
            database
                .find(ApplicantModel.class)
                .setId(id)
                .setLabel("ApplicantModel.findById")
                .setProfileLocation(queryProfileLocationBuilder.create("lookupApplicant"))
                .findOneOrEmpty(),
        executionContext);
  }

  public Optional<AccountModel> lookupAccountByAuthorityId(String authorityId) {
    checkNotNull(authorityId);
    checkArgument(!authorityId.isEmpty());
    return database
        .find(AccountModel.class)
        .where()
        .eq("authority_id", authorityId)
        .setLabel("AccountModel.findById")
        .setProfileLocation(queryProfileLocationBuilder.create("lookupAccountByAuthorityId"))
        .findOneOrEmpty();
  }

  public Optional<AccountModel> lookupAccountByEmail(String emailAddress) {
    checkNotNull(emailAddress);
    checkArgument(!emailAddress.isEmpty());
    return database
        .find(AccountModel.class)
        .where()
        .eq("email_address", emailAddress)
        .setLabel("AccountModel.findByEmail")
        .setProfileLocation(queryProfileLocationBuilder.create("lookupAccountByEmail"))
        .findOneOrEmpty();
  }

  public CompletionStage<Optional<AccountModel>> lookupAccountByEmailAsync(String emailAddress) {
    if (emailAddress == null) {
      return CompletableFuture.failedStage(new NullPointerException());
    }
    if (emailAddress.isEmpty()) {
      return CompletableFuture.failedStage(new IllegalArgumentException());
    }
    return supplyAsync(
        () ->
            database
                .find(AccountModel.class)
                .where()
                .eq("email_address", emailAddress)
                .setLabel("AccountModel.findByEmail")
                .setProfileLocation(queryProfileLocationBuilder.create("lookupAccountByEmailAsync"))
                .findOneOrEmpty(),
        executionContext);
  }

  /**
   * Returns the most recent Applicant identified by Account, creating one if necessary.
   *
   * <p>If no applicant exists, this is probably an account waiting for a trusted intermediary, so
   * we create one.
   */
  private ApplicantModel getOrCreateApplicant(AccountModel account) {
    Optional<ApplicantModel> applicantOpt =
        account.getApplicants().stream().max(Comparator.comparing(ApplicantModel::getWhenCreated));
    return applicantOpt.orElseGet(() -> new ApplicantModel().setAccount(account).saveAndReturn());
  }

  public CompletionStage<Optional<ApplicantModel>> lookupApplicantByAuthorityId(
      String authorityId) {
    return supplyAsync(
        () -> lookupAccountByAuthorityId(authorityId).map(this::getOrCreateApplicant),
        executionContext);
  }

  public CompletionStage<Optional<ApplicantModel>> lookupApplicantByEmail(String emailAddress) {
    return supplyAsync(
        () -> lookupAccountByEmail(emailAddress).map(this::getOrCreateApplicant), executionContext);
  }

  public CompletionStage<Void> insertApplicant(ApplicantModel applicant) {
    return supplyAsync(
        () -> {
          database.insert(applicant);
          return null;
        },
        executionContext);
  }

  public CompletionStage<Void> updateApplicant(ApplicantModel applicant) {
    return supplyAsync(
        () -> {
          database.update(applicant);
          return null;
        },
        executionContext);
  }

  public void updateTiClient(
      AccountModel account,
      ApplicantModel applicant,
      String firstName,
      String middleName,
      String lastName,
      String phoneNumber,
      String tiNote,
      String email,
      String newDob) {

    try (Transaction transaction = database.beginTransaction(TxIsolation.SERIALIZABLE)) {
      transaction.setBatchMode(true);
      // When storing empty email addresses, DB stores them as nulls.
      // But the default in forms for empty emails addresses are empty strings and not nulls
      // hence this conversion is necessary to set null emails to empty string
      String currentEmail =
          Strings.isNullOrEmpty(account.getEmailAddress()) ? "" : account.getEmailAddress();
      // new email should different from the current email
      if (!email.equals(currentEmail)) {
        if (!Strings.isNullOrEmpty(email) && lookupAccountByEmail(email).isPresent()) {
          throw new EmailAddressExistsException();
        }
        account.setEmailAddress(email);
        applicant.setEmailAddress(email);
      }
      account.setTiNote(tiNote);
      applicant.getApplicantData().setPhoneNumber(phoneNumber);
      applicant
          .getApplicantData()
          .setUserName(firstName, Optional.ofNullable(middleName), Optional.ofNullable(lastName));
      applicant.getApplicantData().setDateOfBirth(newDob);
      account.save();
      applicant.save();
      database.saveAll(account, applicant);
      transaction.commit();
    }
  }

  public Optional<ApplicantModel> lookupApplicantSync(long id) {
    return database
        .find(ApplicantModel.class)
        .setId(id)
        .setLabel("ApplicantModel.findById")
        .setProfileLocation(queryProfileLocationBuilder.create("lookupApplicantSync"))
        .findOneOrEmpty();
  }

  /** Merge the older applicant data into the newer applicant, and set both to the given account. */
  public CompletionStage<ApplicantModel> mergeApplicants(
      ApplicantModel left, ApplicantModel right, AccountModel account) {
    return supplyAsync(
        () -> {
          left.setAccount(account).save();
          right.setAccount(account).save();
          return mergeApplicants(left, right).saveAndReturn();
        },
        executionContext);
  }

  /** Merge the applicant data from older applicant into the newer applicant. */
  private ApplicantModel mergeApplicants(ApplicantModel left, ApplicantModel right) {
    if (left.getWhenCreated().isAfter(right.getWhenCreated())) {
      ApplicantModel tmp = left;
      left = right;
      right = tmp;
    }
    // At this point, "left" is older, "right" is newer, we will merge "left" into "right", because
    // the newer applicant is always preferred when more than one applicant matches an account.
    right.getApplicantData().mergeFrom(left.getApplicantData());
    return right;
  }

  public List<TrustedIntermediaryGroupModel> listTrustedIntermediaryGroups() {
    return database
        .find(TrustedIntermediaryGroupModel.class)
        .orderBy("name asc")
        .setLabel("TrustedIntermediaryGroup.findList")
        .setProfileLocation(queryProfileLocationBuilder.create("listTrustedIntermediaryGroups"))
        .findList();
  }

  public TrustedIntermediaryGroupModel createNewTrustedIntermediaryGroup(
      String name, String description) {
    TrustedIntermediaryGroupModel tiGroup = new TrustedIntermediaryGroupModel(name, description);
    tiGroup.save();
    return tiGroup;
  }

  public void deleteTrustedIntermediaryGroup(long id) {
    Optional<TrustedIntermediaryGroupModel> tiGroup = getTrustedIntermediaryGroup(id);
    if (tiGroup.isEmpty()) {
      throw new NoSuchTrustedIntermediaryGroupError();
    }
    database.delete(tiGroup.get());
  }

  public Optional<TrustedIntermediaryGroupModel> getTrustedIntermediaryGroup(long id) {
    return database
        .find(TrustedIntermediaryGroupModel.class)
        .setId(id)
        .setLabel("TrustedIntermediaryGroup.findById")
        .setProfileLocation(queryProfileLocationBuilder.create("getTrustedIntermediaryGroup"))
        .findOneOrEmpty();
  }

  /**
   * Adds the given email address to the TI group. If the email address does not correspond to an
   * existing account, then create an account and associate it, so it will be ready when the TI
   * signs in for the first time.
   */
  public void addTrustedIntermediaryToGroup(long id, String emailAddress) {
    Optional<TrustedIntermediaryGroupModel> tiGroup = getTrustedIntermediaryGroup(id);
    if (tiGroup.isEmpty()) {
      throw new NoSuchTrustedIntermediaryGroupError();
    }
    Optional<AccountModel> accountMaybe = lookupAccountByEmail(emailAddress);
    AccountModel account =
        accountMaybe.orElseGet(
            () -> {
              AccountModel a = new AccountModel();
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
    Optional<TrustedIntermediaryGroupModel> tiGroup = getTrustedIntermediaryGroup(id);
    if (tiGroup.isEmpty()) {
      throw new NoSuchTrustedIntermediaryGroupError();
    }
    Optional<AccountModel> accountMaybe = lookupAccount(accountId);
    if (accountMaybe.isEmpty()) {
      throw new NoSuchTrustedIntermediaryError();
    }
    AccountModel account = accountMaybe.get();
    if (account.getMemberOfGroup().isPresent()
        && account.getMemberOfGroup().get().equals(tiGroup.get())) {
      account.setMemberOfGroup(null);
      account.save();
    } else {
      throw new NoSuchTrustedIntermediaryError();
    }
  }

  public Optional<AccountModel> lookupAccount(long accountId) {
    return database
        .find(AccountModel.class)
        .setId(accountId)
        .setLabel("AccountModel.findById")
        .setProfileLocation(queryProfileLocationBuilder.create("lookupAccount"))
        .findOneOrEmpty();
  }

  public Optional<TrustedIntermediaryGroupModel> getTrustedIntermediaryGroup(
      CiviFormProfile civiformProfile) {
    return civiformProfile.getAccount().join().getMemberOfGroup();
  }

  /**
   * Create an applicant and add it to the provided trusted intermediary group. Associate it with an
   * email address if one is provided, but if one is not provided, use an anonymous (guest-style)
   * account.
   *
   * @return optional applicantId of the newly created client
   * @throws EmailAddressExistsException if the provided email address already exists.
   */
  public Long createNewApplicantForTrustedIntermediaryGroup(
      TiClientInfoForm form, TrustedIntermediaryGroupModel tiGroup) {
    AccountModel newAccount = new AccountModel();
    if (!Strings.isNullOrEmpty(form.getEmailAddress())) {
      if (lookupAccountByEmail(form.getEmailAddress()).isPresent()) {
        throw new EmailAddressExistsException();
      }
      newAccount.setEmailAddress(form.getEmailAddress());
    }
    newAccount.setManagedByGroup(tiGroup);
    newAccount.setTiNote(form.getTiNote());
    newAccount.save();
    ApplicantModel applicant = new ApplicantModel();
    applicant.setAccount(newAccount);
    ApplicantData applicantData = applicant.getApplicantData();
    applicantData.setUserName(
        form.getFirstName(),
        Optional.ofNullable(form.getMiddleName()),
        Optional.ofNullable(form.getLastName()));
    applicantData.setDateOfBirth(form.getDob());
    applicant.setEmailAddress(form.getEmailAddress());
    applicant.setPhoneNumber(form.getPhoneNumber());
    applicant.save();
    return applicant.id;
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

    Optional<AccountModel> maybeAccount = lookupAccountByEmail(accountEmail);
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
    Optional<AccountModel> maybeAccount = lookupAccountByEmail(accountEmail);
    maybeAccount.ifPresent(
        account -> {
          account.removeAdministeredProgram(program);
          account.save();
        });
  }

  public ImmutableSet<AccountModel> getGlobalAdmins() {
    return ImmutableSet.copyOf(
        database
            .find(AccountModel.class)
            .where()
            .eq("global_admin", true)
            .setLabel("AccountModel.findList")
            .setProfileLocation(queryProfileLocationBuilder.create("getGlobalAdmins"))
            .findList());
  }

  @VisibleForTesting
  public ImmutableSet<AccountModel> listAccounts() {
    return ImmutableSet.copyOf(
        database
            .find(AccountModel.class)
            .setLabel("AccountModel.findSet")
            .setProfileLocation(queryProfileLocationBuilder.create("listAccounts"))
            .findSet());
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

  /**
   * For use in {@link MigratePrimaryApplicantInfoJob}. Will return all applicants that have data in
   * well known paths.
   */
  public Query<ApplicantModel> findApplicantsNeedingPrimaryApplicantInfoDataMigration() {
    String sql =
        "WITH temp AS (SELECT * , ((object#>>'{}')::jsonb)::json AS parsed FROM applicants) SELECT"
            + " temp.* FROM temp LEFT JOIN accounts ON accounts.id = temp.account_id WHERE"
            + " (temp.parsed#>'{applicant,name}' IS NOT NULL) OR (temp.email_address IS NULL and"
            + " accounts.email_address IS NOT NULL) OR"
            + " (temp.parsed#>'{applicant,applicant_phone_number}' IS NOT NULL) OR"
            + " (temp.parsed#>'{applicant,applicant_date_of_birth}' IS NOT NULL)";
    return database.findNative(ApplicantModel.class, sql);
  }

  /**
   * Associates the ID token from the profile with the provided session id and persists this
   * association to the provided account.
   *
   * <p>Also purges any expired ID tokens as a side effect.
   */
  public void updateSerializedIdTokens(
      AccountModel account, String sessionId, OidcProfile oidcProfile) {
    SerializedIdTokens serializedIdTokens = account.getSerializedIdTokens();
    if (serializedIdTokens == null) {
      serializedIdTokens = new SerializedIdTokens();
      account.setSerializedIdTokens(serializedIdTokens);
    }
    IdTokens idTokens = idTokensFactory.create(serializedIdTokens);
    idTokens.purgeExpiredIdTokens();
    idTokens.storeIdToken(sessionId, oidcProfile.getIdTokenString());
    account.save();
  }
}
