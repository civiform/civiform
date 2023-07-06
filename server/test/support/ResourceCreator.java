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
import services.question.types.QuestionDefinitionConfig;
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
   * Create an API key with subnet of "8.8.8.8/32,1.1.1.1/32" and an expiration date one year in the
   * future.
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
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription("")
                .setQuestionText(LocalizedStrings.of())
                .setQuestionHelpText(LocalizedStrings.empty())
                .build());
    Question question = new Question(definition);
    question.save();
    return question;
  }

  public Question insertQuestion() {
    String name = UUID.randomUUID().toString();
    QuestionDefinition definition =
        new TextQuestionDefinition(
            QuestionDefinitionConfig.builder()
                .setName(name)
                .setDescription("")
                .setQuestionText(LocalizedStrings.of())
                .setQuestionHelpText(LocalizedStrings.empty())
                .build());
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

  /**
   * Inserts and Applicant and accompanying Account into the database.
   *
   * @param accountEmail an Optional representing the email address of the account. If empty, we
   *     also don't populate the authority ID, which makes this test user a guest.
   * @return the applicant
   */
  public Applicant insertApplicantWithAccount(Optional<String> accountEmail) {
    Applicant applicant = insertApplicant();
    Account account = insertAccount();

    accountEmail.ifPresent(account::setEmailAddress);
    // If the account has an email, it is an authorized user and should have an
    // authority ID.
    accountEmail.ifPresent(unused -> account.setAuthorityId(UUID.randomUUID().toString()));
    account.save();
    applicant.setAccount(account);
    applicant.save();

    return applicant;
  }

  /**
   * Inserts an Account with the given email address into the database. Sets an authority ID such
   * that the user will be "logged in".
   *
   * @param email the email address to use for the account
   * @return the account
   */
  public Account insertAccountWithEmail(String email) {
    Account account = new Account();
    account.setEmailAddress(email);
    // User is not a guest, so they should have an authority ID.
    account.setAuthorityId(UUID.randomUUID().toString());
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
