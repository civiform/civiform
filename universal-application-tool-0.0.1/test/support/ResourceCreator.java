package support;

import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import models.Account;
import models.Applicant;
import models.Models;
import models.Program;
import models.Question;
import models.TrustedIntermediaryGroup;
import play.db.ebean.EbeanConfig;
import play.inject.Injector;
import services.LocalizedStrings;
import services.question.types.QuestionDefinition;
import services.question.types.TextQuestionDefinition;

public class ResourceCreator {

  private final EbeanServer ebeanServer;

  public ResourceCreator(Injector injector) {
    this.ebeanServer = Ebean.getServer(injector.instanceOf(EbeanConfig.class).defaultServer());
    ProgramBuilder.setInjector(injector);
  }

  public void truncateTables() {
    Models.truncate(ebeanServer);
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
