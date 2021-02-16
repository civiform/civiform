package repository;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.Program;
import play.db.ebean.EbeanConfig;

public class ProgramRepository {

  private final EbeanServer ebeanServer;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public ProgramRepository(EbeanConfig ebeanConfig, DatabaseExecutionContext executionContext) {
    this.ebeanServer = Ebean.getServer(ebeanConfig.defaultServer());
    this.executionContext = executionContext;
  }

  /** Return all programs in a set. */
  public CompletionStage<List<Program>> listPrograms() {
    return supplyAsync(() -> ebeanServer.find(Program.class).findList(), executionContext);
  }

  public CompletionStage<Optional<Program>> lookupProgram(long id) {
    return supplyAsync(
        () -> Optional.ofNullable(ebeanServer.find(Program.class).where().eq("id", id).findOne()),
        executionContext);
  }

  public CompletionStage<Program> insertProgram(Program program) {
    return supplyAsync(
        () -> {
          ebeanServer.insert(program);
          return program;
        },
        executionContext);
  }

  public Program insertProgramSync(Program program) {
    ebeanServer.insert(program);
    return program;
  }

  public CompletionStage<Program> updateProgram(Program program) {
    return supplyAsync(
        () -> {
          ebeanServer.update(program);
          return program;
        },
        executionContext);
  }

  public Program updateProgramSync(Program program) {
    ebeanServer.update(program);
    return program;
  }

  public CompletionStage<Program> insertOrUpdateProgram(Program program) {
    return supplyAsync(
        () -> {
          ebeanServer.save(program);
          return program;
        },
        executionContext);
  }

  public Program insertOrUpdateProgramSync(Program program) {
    ebeanServer.save(program);
    return program;
  }
}
