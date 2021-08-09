package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import auth.CiviFormProfile;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import forms.AddApplicantToTrustedIntermediaryGroupForm;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
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
import models.LifecycleStage;
import models.Program;
import models.TrustedIntermediaryGroup;
import play.db.ebean.EbeanConfig;
import services.program.ProgramDefinition;
import services.ti.EmailAddressExistsException;
import services.ti.NoSuchTrustedIntermediaryError;
import services.ti.NoSuchTrustedIntermediaryGroupError;

/**
 * UserRepository performs complicated operations on {@link Account} and {@link Applicant} that
 * often involve other EBean models or asynchronous handling.
 */
public class UserRepository {

  private final EbeanServer ebeanServer;
  private final DatabaseExecutionContext executionContext;
  private final Provider<VersionRepository> versionRepositoryProvider;

  @Inject
  public UserRepository(
      EbeanConfig ebeanConfig,
      DatabaseExecutionContext executionContext,
      Provider<VersionRepository> versionRepositoryProvider) {
    this.ebeanServer = Ebean.getServer(checkNotNull(ebeanConfig).defaultServer());
    this.executionContext = checkNotNull(executionContext);
    this.versionRepositoryProvider = checkNotNull(versionRepositoryProvider);
  }

  public CompletionStage<Set<Applicant>> listApplicants() {
    return supplyAsync(() -> ebeanServer.find(Applicant.class).findSet(), executionContext);
  }

  public CompletionStage<Optional<Applicant>> lookupApplicant(long id) {
    return supplyAsync(
        () -> ebeanServer.find(Applicant.class).setId(id).findOneOrEmpty(), executionContext);
  }

  /**
   * Returns all programs that are appropriate to serve to an applicant - which is any program
   * program where they have an application in the draft stage, and any active program.
   */
  public CompletionStage<ImmutableMap<LifecycleStage, ImmutableList<ProgramDefinition>>>
      programsForApplicant(long applicantId) {
    return supplyAsync(
        () -> {
          ImmutableList<ProgramDefinition> inProgressPrograms =
              ebeanServer
                  .find(Program.class)
                  .alias("p")
                  .where()
                  .exists(
                      ebeanServer
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
                  .collect(ImmutableList.toImmutableList());
          return ImmutableMap.of(
              LifecycleStage.DRAFT, inProgressPrograms,
              LifecycleStage.ACTIVE, activePrograms);
        },
        executionContext.current());
  }

  public Optional<Account> lookupAccount(String emailAddress) {
    if (emailAddress == null || emailAddress.isEmpty()) {
      return Optional.empty();
    }
    return ebeanServer
        .find(Account.class)
        .where()
        .eq("email_address", emailAddress)
        .findOneOrEmpty();
  }

  public CompletionStage<Optional<Applicant>> lookupApplicant(String emailAddress) {
    return supplyAsync(
        () -> {
          Optional<Account> accountMaybe = lookupAccount(emailAddress);
          // Return the applicant which was most recently created.
          // If no applicant exists, this is probably an account waiting for
          // a trusted intermediary - create one.
          if (accountMaybe.isEmpty()) {
            return Optional.empty();
          }
          Optional<Applicant> applicantMaybe =
              accountMaybe.flatMap(
                  account ->
                      account.getApplicants().stream()
                          .max(Comparator.comparing(compared -> compared.getWhenCreated())));
          if (applicantMaybe.isPresent()) {
            return applicantMaybe;
          }
          Applicant newApplicant = new Applicant();
          newApplicant.setAccount(accountMaybe.get());
          newApplicant.save();
          return Optional.of(newApplicant);
        },
        executionContext);
  }

  public CompletionStage<Void> insertApplicant(Applicant applicant) {
    return supplyAsync(
        () -> {
          ebeanServer.insert(applicant);
          return null;
        },
        executionContext);
  }

  public CompletionStage<Void> updateApplicant(Applicant applicant) {
    return supplyAsync(
        () -> {
          ebeanServer.update(applicant);
          return null;
        },
        executionContext);
  }

  public Optional<Applicant> lookupApplicantSync(long id) {
    return ebeanServer.find(Applicant.class).setId(id).findOneOrEmpty();
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
    return ebeanServer.find(TrustedIntermediaryGroup.class).findList();
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
    ebeanServer.delete(tiGroup.get());
  }

  public Optional<TrustedIntermediaryGroup> getTrustedIntermediaryGroup(long id) {
    return ebeanServer.find(TrustedIntermediaryGroup.class).setId(id).findOneOrEmpty();
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
    Optional<Account> accountMaybe = lookupAccount(emailAddress);
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
    return ebeanServer.find(Account.class).setId(accountId).findOneOrEmpty();
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
      if (lookupAccount(form.getEmailAddress()).isPresent()) {
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
   * exist, this will create a new account for the given email, so that when a user with that email
   * signs in for the first time they will be a program admin.
   *
   * @param accountEmail the email of the account that will administer the given program
   * @param program the {@link ProgramDefinition} to add to this given account
   */
  public void addAdministeredProgram(String accountEmail, ProgramDefinition program) {
    if (accountEmail.isBlank()) {
      return;
    }

    Optional<Account> maybeAccount = lookupAccount(accountEmail);
    Account account =
        maybeAccount.orElseGet(
            () -> {
              Account a = new Account();
              a.setEmailAddress(accountEmail);
              return a;
            });
    account.addAdministeredProgram(program);
    account.save();
  }

  /**
   * If the account identified by the given email administers the given program, remove the program
   * from the list of programs this account administers.
   *
   * @param accountEmail the email of the account
   * @param program the {@link ProgramDefinition} to remove from the given account
   */
  public void removeAdministeredProgram(String accountEmail, ProgramDefinition program) {
    Optional<Account> maybeAccount = lookupAccount(accountEmail);
    maybeAccount.ifPresent(
        account -> {
          account.removeAdministeredProgram(program);
          account.save();
        });
  }

  public ImmutableSet<Account> getGlobalAdmins() {
    return ImmutableSet.copyOf(
        ebeanServer.find(Account.class).where().eq("global_admin", true).findList());
  }
}
