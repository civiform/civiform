package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.Transaction;
import io.ebean.TxScope;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Provider;
import models.Account;
import models.Application;
import models.LifecycleStage;
import models.Program;
import models.Version;
import services.program.ProgramNotFoundException;

/**
 * ProgramRepository performs complicated operations on {@link Program} that often involve other
 * EBean models or asynchronous handling.
 */
public class ProgramRepository {

  private final Database database;
  private final DatabaseExecutionContext executionContext;
  private final Provider<VersionRepository> versionRepository;

  @Inject
  public ProgramRepository(
      DatabaseExecutionContext executionContext, Provider<VersionRepository> versionRepository) {
    this.database = DB.getDefault();
    this.executionContext = checkNotNull(executionContext);
    this.versionRepository = checkNotNull(versionRepository);
  }

  public CompletionStage<Optional<Program>> lookupProgram(long id) {
    return supplyAsync(
        () -> database.find(Program.class).where().eq("id", id).findOneOrEmpty(), executionContext);
  }

  public Program insertProgramSync(Program program) {
    program.id = null;
    database.insert(program);
    program.refresh();
    return program;
  }

  public Program updateProgramSync(Program program) {
    database.update(program);
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
      // Inside a question update, this will be a savepoint rather than a
      // full transaction.  Otherwise it will be creating a new transaction.
      Transaction transaction = database.beginTransaction(TxScope.required());
      try {
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
        transaction.commit();
        return newDraft;
      } catch (IllegalStateException e) {
        transaction.rollback();
        // We must end the transaction here since we are going to recurse and try again.
        // We cannot have this transaction on the thread-local transaction stack when that
        // happens.
        transaction.end();
        return createOrUpdateDraft(existingProgram);
      } finally {
        // This may come after a prior call to `transaction.end` in the event of a
        // precondition failure - this is okay, since it a double-call to `end` on
        // a particular transaction.  Only double calls to database.endTransaction
        // must be avoided.
        transaction.end();
      }
    }
  }

  public CompletableFuture<Program> getForSlug(String slug) {
    return supplyAsync(
        () -> {
          for (Program program : database.find(Program.class).where().isNull("slug").findList()) {
            program.getSlug();
            program.save();
          }
          ImmutableList<Program> activePrograms =
              versionRepository.get().getActiveVersion().getPrograms();
          List<Program> programsMatchingSlug =
              database.find(Program.class).where().eq("slug", slug).findList();
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

  public ImmutableList<Account> getProgramAdministrators(String programName) {
    return ImmutableList.copyOf(
        database.find(Account.class).where().arrayContains("admin_of", programName).findList());
  }

  public ImmutableList<Account> getProgramAdministrators(long programId)
      throws ProgramNotFoundException {
    Optional<Program> program = database.find(Program.class).setId(programId).findOneOrEmpty();
    if (program.isEmpty()) {
      throw new ProgramNotFoundException(programId);
    }
    return getProgramAdministrators(program.get().getProgramDefinition().adminName());
  }

  public ImmutableList<Program> getAllProgramVersions(long programId) {
    return database
        .find(Program.class)
        .where()
        .eq(
            "name",
            database.find(Program.class).setId(programId).select("name").findSingleAttribute())
        .findList()
        .stream()
        .collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<Long> getAllProgramVersionIds(long programId) {
    return database
        .find(Program.class)
        .select("id")
        .where()
        .eq(
            "name",
            database.find(Program.class).setId(programId).select("name").findSingleAttribute())
        .findSingleAttributeList()
        .stream()
        .map((obj) -> (Long) obj)
        .collect(ImmutableList.toImmutableList());
  }

  public ImmutableList<Application> getApplicationsForVersions(ImmutableList<Long> versionIds) {
    return database
        .find(Application.class)
        .fetch("program")
        .where()
        .in("program_id", versionIds)
        .or()
        .eq("lifecycle_stage", LifecycleStage.ACTIVE)
        .eq("lifecycle_stage", LifecycleStage.OBSOLETE)
        .endOr()
        .findList()
        .stream()
        .collect(ImmutableList.toImmutableList());
  }
}
