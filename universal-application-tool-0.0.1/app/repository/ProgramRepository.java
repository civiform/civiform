package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import io.ebean.TxScope;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Provider;
import models.Account;
import models.LifecycleStage;
import models.Program;
import models.Version;
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
        () ->
            new ImmutableList.Builder<Program>()
                .addAll(versionRepository.get().getActiveVersion().getPrograms())
                .addAll(versionRepository.get().getDraftVersion().getPrograms())
                .build(),
        executionContext);
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
      try {
        ebeanServer.beginTransaction(TxScope.requiresNew());
        // Program -> builder -> back to program in order to clear any metadata stored
        // in the program (for example, version information).
        Program newDraft = existingProgram.getProgramDefinition().toBuilder().build().toProgram();
        newDraft = insertProgramSync(newDraft);
        newDraft.addVersion(draftVersion);
        newDraft.save();
        draftVersion.refresh();
        Preconditions.checkState(
            draftVersion.getPrograms().contains(newDraft),
            "Must have successfully added draft version.");
        Preconditions.checkState(
            draftVersion.getLifecycleStage().equals(LifecycleStage.DRAFT),
            "Draft version must remain a draft throughout this transaction.");
        Preconditions.checkState(
            draftVersion.getPrograms().stream()
                    .filter(
                        program ->
                            program
                                .getProgramDefinition()
                                .adminName()
                                .equals(existingProgram.getProgramDefinition().adminName()))
                    .count()
                == 1,
            "Must be exactly one program with this name in the draft.");
        versionRepository.get().updateQuestionVersions(newDraft);
        ebeanServer.commitTransaction();
        return newDraft;
      } catch (IllegalStateException e) {
        ebeanServer.rollbackTransaction();
        return createOrUpdateDraft(existingProgram);
      }
    }
  }

  public CompletableFuture<Program> getForSlug(String slug) {
    return supplyAsync(
        () -> {
          for (Program program :
              ebeanServer.find(Program.class).where().isNull("slug").findList()) {
            program.getSlug();
            program.save();
          }
          ImmutableList<Program> activePrograms =
              versionRepository.get().getActiveVersion().getPrograms();
          List<Program> programsMatchingSlug =
              ebeanServer.find(Program.class).where().eq("slug", slug).findList();
          Optional<Program> programMaybe =
              activePrograms.stream()
                  .filter(activeProgram -> programsMatchingSlug.contains(activeProgram))
                  .findFirst();
          if (programMaybe.isPresent()) {
            return programMaybe.get();
          }
          throw new RuntimeException(new ProgramNotFoundException(slug));
        },
        executionContext.current());
  }

  public ImmutableList<Account> getProgramAdministrators(long programId) throws ProgramNotFoundException {
    Optional<Program> program = ebeanServer.find(Program.class).setId(programId).findOneOrEmpty();
    if (program.isEmpty()) {
        throw new ProgramNotFoundException(programId);
    }
    String name = program.get().getProgramDefinition().adminName();
    return ImmutableList.copyOf(ebeanServer.find(Account.class).where().arrayContains("admin_of", name).findList());
  }
}
