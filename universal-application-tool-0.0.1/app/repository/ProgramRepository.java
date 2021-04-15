package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.collect.ImmutableList;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Provider;
import models.LifecycleStage;
import models.Program;
import play.db.ebean.EbeanConfig;
import services.program.ProgramNotFoundException;

public class ProgramRepository {

  private final EbeanServer ebeanServer;
  private final DatabaseExecutionContext executionContext;
  private final Provider<VersionRepository> versionRepository;

  @Inject
  public ProgramRepository(
      EbeanConfig ebeanConfig,
      DatabaseExecutionContext executionContext,
      Provider<VersionRepository> versionRepository) {
    this.ebeanServer = Ebean.getServer(checkNotNull(ebeanConfig).defaultServer());
    this.executionContext = checkNotNull(executionContext);
    this.versionRepository = checkNotNull(versionRepository);
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

  public Program createOrUpdateDraft(Program existingProgram) throws ProgramNotFoundException {
    Optional<Program> existingDraft =
        ebeanServer
            .find(Program.class)
            .where()
            .eq("lifecycle_stage", LifecycleStage.DRAFT.getValue())
            .eq("name", existingProgram.getProgramDefinition().name())
            .findOneOrEmpty();
    if (existingDraft.isPresent()) {
      Program updatedDraft =
          existingProgram.getProgramDefinition().toBuilder()
              .setId(existingDraft.get().id)
              .setLifecycleStage(LifecycleStage.DRAFT)
              .build()
              .toProgram();
      this.updateProgramSync(updatedDraft);
      return updatedDraft;
    } else {
      Program newDraft =
          existingProgram.getProgramDefinition().toBuilder()
              .setLifecycleStage(LifecycleStage.DRAFT)
              .build()
              .toProgram();
      insertProgramSync(newDraft);
      versionRepository.get().updateQuestionVersions(newDraft);
      return newDraft;
    }
  }
}
