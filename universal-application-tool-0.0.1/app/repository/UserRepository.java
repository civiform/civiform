package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.CiviFormProfile;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import forms.AddApplicantToTrustedIntermediaryGroupForm;
import io.ebean.DB;
import io.ebean.Database;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Provider;
import models.Account;
import models.Applicant;
import models.Application;
import models.DisplayMode;
import models.LifecycleStage;
import models.Program;
import models.TrustedIntermediaryGroup;
import services.CiviFormError;
import services.program.ProgramDefinition;
import services.ti.EmailAddressExistsException;
import services.ti.NoSuchTrustedIntermediaryError;
import services.ti.NoSuchTrustedIntermediaryGroupError;

/**
 * UserRepository performs complicated operations on {@link Account} and {@link Applicant} that
 * often involve other EBean models or asynchronous handling.
 */
public class UserRepository {

  private final Database database;
  private final DatabaseExecutionContext executionContext;
  private final Provider<VersionRepository> versionRepositoryProvider;

  @Inject
  public UserRepository(
      DatabaseExecutionContext executionContext,
      Provider<VersionRepository> versionRepositoryProvider) {
    this.database = DB.getDefault();
    this.executionContext = checkNotNull(executionContext);
    this.versionRepositoryProvider = checkNotNull(versionRepositoryProvider);
  }

  public CompletionStage<Set<Applicant>> listApplicants() {
    return supplyAsync(() -> database.find(Applicant.class).findSet(), executionContext);
  }

  public CompletionStage<Optional<Applicant>> lookupApplicant(long id) {
    return supplyAsync(
        () -> database.find(Applicant.class).setId(id).findOneOrEmpty(), executionContext);
  }

  /**
   * Returns all programs that are appropriate to serve to an applicant - which is any program where
   * they have an application in the draft stage, and any active program that is public.
   */
  public CompletionStage<ImmutableMap<LifecycleStage, ImmutableList<ProgramDefinition>>>
      programsForApplicant(long applicantId) {
    return supplyAsync(
        () -> {
          ImmutableList<ProgramDefinition> inProgressPrograms =
              database
                  .find(Program.class)
                  .alias("p")
                  .where()
                  .exists(
                      database
                          .find(Application.class)
                          .where()
                          .eq("applicant.id", applicantId)
                          .eq("lifecycle_stage", LifecycleStage.DRAFT)
                          .raw("program.id = p.id")
                          .query())
                  .endOr()
                  .findList()
                  .stream()
                  .map(Program::getProgramDefinition)
                  .collect(ImmutableList.toImmutableList());
          ImmutableList<ProgramDefinition> activePrograms =
              versionRepositoryProvider.get().getActiveVersion().getPrograms().stream()
                  .map(Program::getProgramDefinition)
                  .filter(pdef -> pdef.displayMode().equals(DisplayMode.PUBLIC))
                  .collect(ImmutableList.toImmutableList());
          return ImmutableMap.of(
              LifecycleStage.DRAFT, inProgressPrograms,
              LifecycleStage.ACTIVE, activePrograms);
        },
        executionContext.current());
  }

  public Optional<Account> lookupAccountByAuthorityId(String authorityId) {
    if (authorityId == null || authorityId.isEmpty()) {
      return Optional.empty();
    }
    return database.find(Account.class).where().eq("authority_id", authorityId).findOneOrEmpty();
  }

  public Optional<Account> lookupAccountByEmail(String emailAddress) {
    if (emailAddress == null || emailAddress.isEmpty()) {
      return Optional.empty();
    }
    return database.find(Account.class).where().eq("email_address", emailAddress).findOneOrEmpty();
  }
  /**
   * Returns the most recent Applicant identified by Account, creating one if necessary.
   *
   * <p>If no applicant exists, this is probably an account waiting for a trusted intermediary, so
   * we create one.
   */
  private Applicant getOrCreateApplicant(Account account) {
    Optional<Applicant> applicantMaybe =
        account.getApplicants().stream()
            .max(Comparator.comparing(compared -> compared.getWhenCreated()));
    if (applicantMaybe.isPresent()) {
      return applicantMaybe.get();
    }
    Applicant newApplicant = new Applicant().setAccount(account);
    newApplicant.save();
    return newApplicant;
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
          left.setAccount(account);
          left.save();
          right.setAccount(account);
          right.save();
          Applicant merged = mergeApplicants(left, right);
          merged.save();
          return merged;
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

  public void deleteTrustedIntermediaryGroup(long id) throws NoSuchTrustedIntermediaryGroupError {
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
  public void addTrustedIntermediaryToGroup(long id, String emailAddress)
      throws NoSuchTrustedIntermediaryGroupError {
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
    account.setMemberOfGroup(tiGroup.get());
    account.save();
  }

  public void removeTrustedIntermediaryFromGroup(long id, long accountId)
      throws NoSuchTrustedIntermediaryGroupError, NoSuchTrustedIntermediaryError {
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
      AddApplicantToTrustedIntermediaryGroupForm form, TrustedIntermediaryGroup tiGroup)
      throws EmailAddressExistsException {
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
    applicant
        .getApplicantData()
        .setUserName(form.getFirstName(), form.getMiddleName(), form.getLastName());
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
                  "%s does not have an admin account and cannot be added as a Program Admin.",
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
}
