package support;

import com.google.common.collect.ImmutableMap;
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
import play.db.ebean.EbeanConfig;
import play.inject.Injector;
import services.LocalizedStrings;
import services.Path;
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

  public Question insertQuestion(String pathString) {
    String name = UUID.randomUUID().toString();
    return insertQuestion(pathString, name);
  }

  public Question insertQuestion(String pathString, String name) {
    QuestionDefinition definition =
        new TextQuestionDefinition(
            name,
            Path.create(pathString),
            Optional.empty(),
            "",
            LocalizedStrings.create(ImmutableMap.of()),
            LocalizedStrings.of());
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
}
