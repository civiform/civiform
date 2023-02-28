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
            .setSubnet("1.1.1.1/32")
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

  public Applicant insertApplicant() {
    Applicant applicant = new Applicant();
    applicant.save();
    return applicant;
  }

  public Account insertAccount() {
    Account account = new Account();
    account.save();
    return account;
  }

  public Applicant insertApplicantWithAccount() {
    return insertApplicantWithAccount(/* accountEmail= */ Optional.empty());
  }

  public Applicant insertApplicantWithAccount(Optional<String> accountEmail) {
    Applicant applicant = insertApplicant();
    Account account = insertAccount();

    accountEmail.ifPresent(account::setEmailAddress);
    account.save();
    applicant.setAccount(account);
    applicant.save();

    return applicant;
  }

  public Account insertAccountWithEmail(String email) {
    Account account = new Account();
    account.setEmailAddress(email);
    account.save();
    return account;
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
