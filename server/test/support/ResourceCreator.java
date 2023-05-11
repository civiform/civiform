package support;

import io.ebean.DB;
import io.ebean.Database;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import models.Account;
import models.ApiKey;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import models.Models;
import models.Program;
import models.Question;
import models.TrustedIntermediaryGroup;
import org.apache.commons.lang3.tuple.Pair;
import play.inject.Injector;
import services.LocalizedStrings;
import services.apikey.ApiKeyService;
import services.question.types.QuestionDefinition;
import services.question.types.TextQuestionDefinition;

public class ResourceCreator {

  private final Database database;
  private final Injector injector;
  private static final int SECONDS_PER_YEAR = 31536000;

  public ResourceCreator(Injector injector) {
    this.database = DB.getDefault();
    this.injector = injector;
    ProgramBuilder.setInjector(injector);
  }

  /**
   * Create an API key with subnet of "1.1.1.1/32" and an expiration date one year in the future.
   */
  public ApiKey createActiveApiKey(String name, String keyId, String keySecret) {
    ApiKey apiKey =
        new ApiKey()
            .setName(name)
            .setKeyId(keyId)
            .setExpiration(Instant.now().plusSeconds(SECONDS_PER_YEAR))
            .setSubnet("8.8.8.8/32,1.1.1.1/32")
            .setSaltedKeySecret(injector.instanceOf(ApiKeyService.class).salt(keySecret))
            .setCreatedBy("test");

    apiKey.save();

    return apiKey;
  }

  public void truncateTables() {
    Models.truncate(database);
  }

  public void publishNewSynchronizedVersion() {
    injector.instanceOf(repository.VersionRepository.class).publishNewSynchronizedVersion();
  }

  public Question insertQuestion(String name) {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            name, Optional.empty(), "", LocalizedStrings.of(), LocalizedStrings.empty());
    Question question = new Question(definition);
    question.save();
    return question;
  }

  public Question insertQuestion() {
    String name = UUID.randomUUID().toString();
    QuestionDefinition definition =
        new TextQuestionDefinition(
            name, Optional.empty(), "", LocalizedStrings.of(), LocalizedStrings.empty());
    Question question = new Question(definition);
    question.save();
    return question;
  }

  public Program insertActiveProgram(String name) {
    return ProgramBuilder.newActiveProgram(name, "description").build();
  }

  public Program insertActiveProgram(Locale locale, String name) {
    return ProgramBuilder.newActiveProgram().withLocalizedName(locale, name).build();
  }

  public Program insertActiveCommonIntakeForm(String name) {
    return ProgramBuilder.newActiveCommonIntakeForm(name).build();
  }

  public Program insertDraftProgram(String name) {
    return ProgramBuilder.newDraftProgram(name, "description").build();
  }

  public Application insertActiveApplication(Applicant applicant, Program program) {
    return Application.create(applicant, program, LifecycleStage.ACTIVE);
  }

  public Application insertDraftApplication(Applicant applicant, Program program) {
    return Application.create(applicant, program, LifecycleStage.DRAFT);
  }

  public Application insertApplication(
      Applicant applicant, Program program, LifecycleStage lifecycleStage) {
    return Application.create(applicant, program, lifecycleStage);
  }

  public Applicant insertGuestApplicant() {
    Applicant applicant = new Applicant();
    applicant.save();
    return applicant;
  }

  public Account insertGuestAccount() {
    Account account = new Account();
    account.save();
    return account;
  }

  public Applicant insertGuestApplicantWithAccount() {
    return insertAccountAndApplicant(/* isLoggedIn*/ false, Optional.empty()).getRight();
  }

  public Applicant insertLoggedInApplicantWithAccount(String accountEmail) {
    return insertAccountAndApplicant(/* isLoggedIn*/ true, Optional.of(accountEmail)).getRight();
  }

  public Account insertLoggedInAccountWithEmail(String email) {
    return insertAccountAndApplicant(/* isLoggedIn*/ true, Optional.of(email)).getLeft();
  }

  private Pair<Account, Applicant> insertAccountAndApplicant(
      boolean isLoggedIn, Optional<String> email) {
    if (!isLoggedIn && email.isPresent()) {
      throw new RuntimeException("Can't be guest and have an email address");
    }

    Account account = new Account();
    if (isLoggedIn) {
      account.setAuthorityId(UUID.randomUUID().toString());
      account.setEmailAddress(email.get());
    }
    account.save();

    Applicant applicant = new Applicant();
    applicant.save();

    applicant.setAccount(account);
    applicant.save();

    return Pair.of(account, applicant);
  }

  public TrustedIntermediaryGroup insertTrustedIntermediaryGroup() {
    return insertTrustedIntermediaryGroup("");
  }

  public TrustedIntermediaryGroup insertTrustedIntermediaryGroup(String name) {
    TrustedIntermediaryGroup group = new TrustedIntermediaryGroup(name, "description");
    group.save();
    return group;
  }
}
