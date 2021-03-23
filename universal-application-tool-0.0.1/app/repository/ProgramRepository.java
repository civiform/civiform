package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.collect.ImmutableList;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import models.LifecycleStage;
import models.Program;
import play.db.ebean.EbeanConfig;

public class ProgramRepository {

  private final EbeanServer ebeanServer;
  private final DatabaseExecutionContext executionContext;

  @Inject
  public ProgramRepository(EbeanConfig ebeanConfig, DatabaseExecutionContext executionContext) {
    this.ebeanServer = Ebean.getServer(checkNotNull(ebeanConfig).defaultServer());
    this.executionContext = checkNotNull(executionContext);
  }

  /** Return all programs in a list. */
  public CompletionStage<ImmutableList<Program>> listPrograms() {
    return supplyAsync(
        () -> ImmutableList.copyOf(ebeanServer.find(Program.class).findList()), executionContext);
  }

  public CompletionStage<Optional<Program>> lookupProgram(long id) {
    return supplyAsync(
        () -> ebeanServer.find(Program.class).where().eq("id", id).findOneOrEmpty(),
        executionContext);
  }

  public Program insertProgramSync(Program program) {
    program.id = null;
    ebeanServer.insert(program);
    return program;
  }

  public Program updateProgramSync(Program program) {
    ebeanServer.update(program);
    return program;
  }

  public CompletionStage<Void> publishProgramAsync(Program program) {
    return supplyAsync(
        () -> {
          try {
            ebeanServer.beginTransaction();
            program.setLifecycleStage(LifecycleStage.ACTIVE);
            List<Program> allOldPrograms =
                ebeanServer
                    .find(Program.class)
                    .where()
                    .eq("name", program.getProgramDefinition().name())
                    .eq("lifecycle_stage", LifecycleStage.ACTIVE)
                    .not()
                    .eq("id", program.id)
                    .findList();
            for (Program oldProgram : allOldPrograms) {
              oldProgram.setLifecycleStage(LifecycleStage.OBSOLETE);
              oldProgram.save();
            }
            program.save();
            ebeanServer.commitTransaction();
            return null;
          } finally {
            ebeanServer.endTransaction();
          }
        });
  }

  public void publishProgram(Program program) {
    this.publishProgramAsync(program).toCompletableFuture().join();
  }
}
