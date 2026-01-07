package durablejobs.jobs;

import com.google.api.client.util.Preconditions;
import com.google.common.collect.ImmutableList;
import durablejobs.DurableJob;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.QueryIterator;
import io.ebean.Transaction;
import io.ebean.annotation.TxIsolation;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import models.PersistedDurableJobModel;
import models.ProgramModel;
import models.QuestionModel;
import play.cache.AsyncCacheApi;
import repository.ProgramRepository;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinition;
import services.settings.SettingsManifest;

/**
 * This is a one-time job that sets the new isEnumerator property to true on existing program block
 * definitions that have an enumerator question.
 */
@Slf4j
public class SetIsEnumeratorOnBlocksWithEnumeratorQuestionJob extends DurableJob {

  private final PersistedDurableJobModel persistedDurableJob;
  private final ProgramRepository programRepository;
  private final Database database;
  private final AsyncCacheApi programCache;
  private final SettingsManifest settingsManifest;

  public SetIsEnumeratorOnBlocksWithEnumeratorQuestionJob(
      PersistedDurableJobModel persistedDurableJob,
      ProgramRepository programRepository,
      AsyncCacheApi programCache,
      SettingsManifest settingsManifest) {
    this.persistedDurableJob = Preconditions.checkNotNull(persistedDurableJob);
    this.programRepository = Preconditions.checkNotNull(programRepository);
    this.database = DB.getDefault();
    this.programCache = Preconditions.checkNotNull(programCache);
    this.settingsManifest = Preconditions.checkNotNull(settingsManifest);
  }

  @Override
  public PersistedDurableJobModel getPersistedDurableJob() {
    return persistedDurableJob;
  }

  @Override
  public void run() {
    log.info(
        "Starting SetIsEnumeratorOnBlocksWithEnumeratorQuestionJob for job id: '{}'",
        persistedDurableJob.id);
    Transaction jobTransaction = database.beginTransaction(TxIsolation.SERIALIZABLE);
    jobTransaction.setBatchMode(true);
    jobTransaction.setBatchSize(10);

    try (jobTransaction) {
      // Get the ids of all questions that have an enumerator type
      ImmutableList<Long> enumeratorQuestionIds = getAllQuestionIdsWithEnumeratorType();

      if (enumeratorQuestionIds.isEmpty()) {
        log.info("No enumerator questions found. Exiting job.");
        return;
      }

      // Find any program where the block_definitions's questionDefinitions includes any of the
      // enumerator question ids
      try (var programModelIterator =
          getQueryForAllProgramsWithAnEnumeratorQuestion(enumeratorQuestionIds)) {
        while (programModelIterator.hasNext()) {
          ProgramModel programModel = programModelIterator.next();
          ProgramDefinition programDefinition = programModel.getProgramDefinition();

          // For each program with an enumerator question, find the blocks that have an enumerator
          // question and update those block definitions to set isEnumerator = true
          ImmutableList<BlockDefinition> updatedBlockDefinitions =
              programDefinition.blockDefinitions().stream()
                  .map(
                      blockDefinition ->
                          updateBlockIfEnumerator(blockDefinition, enumeratorQuestionIds))
                  .collect(ImmutableList.toImmutableList());

          ProgramDefinition updatedProgramDefinition =
              programDefinition.toBuilder().setBlockDefinitions(updatedBlockDefinitions).build();

          // Save the updated program
          programRepository.updateProgramSync(updatedProgramDefinition.toProgram());
        }
      }

      if (settingsManifest.getProgramCacheEnabled()) {
        programCache.removeAll().toCompletableFuture().join();
      }

      jobTransaction.commit();
      log.info(
          "Successfully completed SetIsEnumeratorOnBlocksWithEnumeratorQuestionJob for job id:"
              + " '{}'",
          persistedDurableJob.id);
    } catch (ProgramNotFoundException e) {
      log.error("Program not found during job execution: " + e.getMessage(), e);
      jobTransaction.rollback();
      log.info(
          "SetIsEnumeratorOnBlocksWithEnumeratorQuestionJob for job id: '{}' rolled back due to"
              + " error.",
          persistedDurableJob.id);
    } catch (RuntimeException e) {
      log.error(e.getMessage(), e);
      jobTransaction.rollback();
      log.info(
          "SetIsEnumeratorOnBlocksWithEnumeratorQuestionJob for job id: '{}' rolled back due to"
              + " error.",
          persistedDurableJob.id);
    }
  }

  /** Get the ids for all enumerator questions. */
  private ImmutableList<Long> getAllQuestionIdsWithEnumeratorType() {
    return database
        .find(QuestionModel.class)
        .where()
        .eq("questionType", "ENUMERATOR")
        .findList()
        .stream()
        .map(q -> q.id)
        .collect(ImmutableList.toImmutableList());
  }

  /* Find any program where the block_definitions's questionDefinitions includes
  an item with an id that is in the given enumerator question id list. */
  private QueryIterator<ProgramModel> getQueryForAllProgramsWithAnEnumeratorQuestion(
      ImmutableList<Long> enumeratorQuestionIds) {

    // Build a SQL WHERE clause to check if any of the enumeratorQuestionIds exist in the
    // block_definitions JSON.
    StringBuilder whereClause = new StringBuilder("(");
    for (int i = 0; i < enumeratorQuestionIds.size(); i++) {
      if (i > 0) {
        whereClause.append(" OR ");
      }

      whereClause
          .append("block_definitions::jsonb @> ")
          .append("'[{\"questionDefinitions\":[{\"id\":")
          .append(enumeratorQuestionIds.get(i))
          .append("}]}]'");
    }
    whereClause.append(")");

    return database.find(ProgramModel.class).where().raw(whereClause.toString()).findIterate();
  }

  private BlockDefinition updateBlockIfEnumerator(
      BlockDefinition blockDefinition, ImmutableList<Long> enumeratorQuestionIds) {
    boolean hasEnumerator =
        blockDefinition.programQuestionDefinitions().stream()
            .map(ProgramQuestionDefinition::id)
            .anyMatch(enumeratorQuestionIds::contains);
    if (hasEnumerator) {
      return blockDefinition.toBuilder().setIsEnumerator(Optional.of(true)).build();
    }
    return blockDefinition;
  }
}
