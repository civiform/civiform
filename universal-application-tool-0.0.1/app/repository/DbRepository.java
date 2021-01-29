package repository;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Applicant;
import models.Program;
import models.Question;
import play.db.ebean.EbeanConfig;

public class DbRepository {

  private final EbeanServer ebeanServer;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public DbRepository(EbeanConfig ebeanConfig, DatabaseExecutionContext executionContext) {
    this.ebeanServer = Ebean.getServer(ebeanConfig.defaultServer());
    this.executionContext = executionContext;
  }

  /** Return all programs in a set. */
  public CompletionStage<Set<Program>> listPrograms() {
    return supplyAsync(() -> ebeanServer.find(Program.class).findSet(), executionContext);
  }

  public CompletionStage<Optional<Program>> lookupProgram(String name, long version) {
    return supplyAsync(
        () ->
            Optional.ofNullable(
                ebeanServer
                    .find(Program.class)
                    .where()
                    .eq("name", name)
                    .eq("version", version)
                    .findOne()),
        executionContext);
  }

  public CompletionStage<Void> insertProgram(Program program) {
    return supplyAsync(
        () -> {
          ebeanServer.insert(program);
          return null;
        },
        executionContext);
  }

  public CompletionStage<Set<Applicant>> listApplicants() {
    return supplyAsync(() -> ebeanServer.find(Applicant.class).findSet(), executionContext);
  }

  public CompletionStage<Optional<Applicant>> lookupApplicant(long id) {
    return supplyAsync(
        () -> Optional.ofNullable(ebeanServer.find(Applicant.class).setId(id).findOne()),
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

  public CompletionStage<Set<Question>> listQuestions() {
    return supplyAsync(() -> ebeanServer.find(Question.class).findSet(), executionContext);
  }

  public CompletionStage<Optional<Question>> lookupQuestion(long id) {
    return supplyAsync(
        () -> Optional.ofNullable(ebeanServer.find(Question.class).setId(id).findOne()),
        executionContext);
  }

  public CompletionStage<Optional<Question>> lookupQuestion(String target) {
    return supplyAsync(
        () ->
            Optional.ofNullable(
                ebeanServer
                    .find(Question.class)
                    .where()
                    .jsonEqualTo("object", "target", target)
                    .findOne()),
        executionContext);
  }

  public CompletionStage<Void> insertQuestion(Question question) {
    return supplyAsync(
        () -> {
          ebeanServer.insert(question);
          return null;
        },
        executionContext);
  }

  public void flush() {
    ebeanServer.flush();
  }
}
