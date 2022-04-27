package support;

import io.ebean.DB;
import io.ebean.Database;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import models.Account;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import models.Models;
import models.Program;
import models.Question;
import models.TrustedIntermediaryGroup;
import play.inject.Injector;
import services.LocalizedStrings;
import services.question.types.QuestionDefinition;
import services.question.types.TextQuestionDefinition;

public class ResourceCreator {

  private final Database database;
  private final Injector injector;

  public ResourceCreator(Injector injector) {
    this.database = DB.getDefault();
    this.injector = injector;
    ProgramBuilder.setInjector(injector);
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

  public Program insertDraftProgram(String name) {
    return ProgramBuilder.newDraftProgram(name, "description").build();
  }

  public Application insertActiveApplication(Applicant applicant, Program program) {
    return Application.create(applicant, program, LifecycleStage.ACTIVE);
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
    Applicant applicant = insertApplicant();
    Account account = insertAccount();

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
