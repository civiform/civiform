package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Provider;
import models.LifecycleStage;
import models.Program;
import models.Question;
import play.db.ebean.EbeanConfig;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinition;

public class ProgramRepository {

  private final EbeanServer ebeanServer;
  private final DatabaseExecutionContext executionContext;
  private final Provider<QuestionRepository> questionRepositoryProvider;

  @Inject
  public ProgramRepository(
      EbeanConfig ebeanConfig,
      DatabaseExecutionContext executionContext,
      Provider<QuestionRepository> questionRepositoryProvider) {
    this.ebeanServer = Ebean.getServer(checkNotNull(ebeanConfig).defaultServer());
    this.executionContext = checkNotNull(executionContext);
    this.questionRepositoryProvider = checkNotNull(questionRepositoryProvider);
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

  /**
   * For each question in this program, check whether it is the most up-to-date version of the
   * question which is either DRAFT or ACTIVE. If it is not, update the pointer to the most
   * up-to-date version of the question. This version of the method creates its own internal
   * transaction. This method can only be called on a draft program.
   */
  public void updateQuestionVersions(Program draftProgram) {
    Transaction transaction = ebeanServer.beginTransaction();
    try {
      updateQuestionVersions(draftProgram, transaction);
      ebeanServer.commitTransaction();
    } finally {
      ebeanServer.endTransaction();
    }
  }

  /**
   * For each question in this program, check whether it is the most up-to-date version of the
   * question which is either DRAFT or ACTIVE. If it is not, update the pointer to the most
   * up-to-date version of the question, using the given transaction. This method can only be called
   * on a draft program.
   */
  public void updateQuestionVersions(Program draftProgram, Transaction transaction) {
    Preconditions.checkArgument(
        draftProgram.getLifecycleStage().equals(LifecycleStage.DRAFT),
        "input program must be a DRAFT.");
    ProgramDefinition.Builder updatedDefinition =
        draftProgram.getProgramDefinition().toBuilder().setBlockDefinitions(ImmutableList.of());
    for (BlockDefinition block : draftProgram.getProgramDefinition().blockDefinitions()) {
      updatedDefinition.addBlockDefinition(updateQuestionVersions(block, transaction));
    }
    draftProgram = new Program(updatedDefinition.build());
    ebeanServer.update(draftProgram, transaction);
  }

  private BlockDefinition updateQuestionVersions(BlockDefinition block, Transaction transaction) {
    BlockDefinition.Builder updatedBlock =
        block.toBuilder().setProgramQuestionDefinitions(ImmutableList.of());
    for (ProgramQuestionDefinition question : block.programQuestionDefinitions()) {
      Optional<Question> updatedQuestion = getLatestVersionOfQuestion(transaction, question.id());
      updatedBlock.addQuestion(
          ProgramQuestionDefinition.create(updatedQuestion.orElseThrow().getQuestionDefinition()));
    }
    return updatedBlock.build();
  }

  private Optional<Question> getLatestVersionOfQuestion(Transaction transaction, long questionId) {
    return ebeanServer
        .find(Question.class)
        .usingTransaction(transaction)
        .where()
        .eq(
            "name",
            ebeanServer.find(Question.class).setId(questionId).select("name").findSingleAttribute())
        .or()
        .eq("lifecycle_stage", LifecycleStage.ACTIVE)
        .eq("lifecycle_stage", LifecycleStage.DRAFT)
        .endOr()
        .orderBy()
        .desc("version")
        .findList()
        .stream()
        .findFirst();
  }

  public CompletionStage<Void> publishProgramAsync(Program program) {
    return supplyAsync(
        () -> {
          try {
            ebeanServer.beginTransaction();
            // First, set this program to ACTIVE and set the existing ACTIVE program (if it exists)
            // to OBSOLETE.
            Optional<Program> oldProgramMaybe =
                ebeanServer
                    .find(Program.class)
                    .where()
                    .eq("name", program.getProgramDefinition().name())
                    .eq("lifecycle_stage", LifecycleStage.ACTIVE)
                    .not()
                    .eq("id", program.id)
                    .findOneOrEmpty();
            if (oldProgramMaybe.isPresent()) {
              Program oldProgram = oldProgramMaybe.get();
              oldProgram.setLifecycleStage(LifecycleStage.OBSOLETE);
              oldProgram.save();
            }

            program.setLifecycleStage(LifecycleStage.ACTIVE);
            program.save();

            /* Each block contains a list of question ids.  For each question id, we will check
            whether a question with the same name but a different id is already ACTIVE.  If it is, we
            will mark it obsolete.  We will always mark the question being pointed to as ACTIVE.
            This is safe because it is impossible to have a draft program which has a question
            that is not the most recent version, since a new draft program always gets the
            latest version, and a new version of a question updates all draft programs. */
            for (BlockDefinition block : program.getProgramDefinition().blockDefinitions()) {
              for (ProgramQuestionDefinition question : block.programQuestionDefinitions()) {
                questionRepositoryProvider.get().setQuestionLive(question.id());
              }
            }

            ebeanServer.commitTransaction();
            return null;
          } finally {
            ebeanServer.endTransaction();
          }
        },
        executionContext);
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
      updateQuestionVersions(newDraft);
      return newDraft;
    }
  }

  public void publishProgram(Program program) {
    this.publishProgramAsync(program).toCompletableFuture().join();
  }
}
