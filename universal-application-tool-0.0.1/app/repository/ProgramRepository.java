package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Provider;
import models.Program;
import models.Version;
import play.db.ebean.EbeanConfig;

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
    program.refresh();
    return program;
  }

  public Program updateProgramSync(Program program) {
    ebeanServer.update(program);
    return program;
  }

  public Program createOrUpdateDraft(Program existingProgram) {
    Version draftVersion = versionRepository.get().getDraftVersion();
    Optional<Program> existingDraft =
        draftVersion.getProgramByName(existingProgram.getProgramDefinition().adminName());
    if (existingDraft.isPresent()) {
      Program updatedDraft =
          existingProgram.getProgramDefinition().toBuilder()
              .setId(existingDraft.get().id)
              .build()
              .toProgram();
      this.updateProgramSync(updatedDraft);
      return updatedDraft;
    } else {
      // Program -> builder -> back to program in order to clear any metadata stored
      // in the program (for example, version information).
      Program newDraft = existingProgram.getProgramDefinition().toBuilder().build().toProgram();
      newDraft = insertProgramSync(newDraft);
      newDraft.addVersion(draftVersion);
      newDraft.save();
      draftVersion.refresh();
      Preconditions.checkState(draftVersion.getPrograms().contains(newDraft));
      versionRepository.get().updateQuestionVersions(newDraft);
      return newDraft;
    }
  }
}
