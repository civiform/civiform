package repository;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import io.ebean.Ebean;
import io.ebean.EbeanServer;
import io.ebean.Transaction;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;
import javax.inject.Inject;
import models.LifecycleStage;
import models.Program;
import models.Question;
import play.db.ebean.EbeanConfig;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;

public class VersionRepository {

  private final EbeanServer ebeanServer;
  private final ProgramRepository programRepository;

  @Inject
  public VersionRepository(EbeanConfig ebeanConfig, ProgramRepository programRepository) {
    this.ebeanServer = Ebean.getServer(checkNotNull(ebeanConfig).defaultServer());
    this.programRepository = checkNotNull(programRepository);
  }

  public void publishNewSynchronizedVersion() {
    try {
      Transaction transaction = ebeanServer.beginTransaction();
      // For each unedited active program which does not have a draft program of the
      // same name, set the version and go.
      long version = getNextVersion();
      for (Program uneditedActive :
          ebeanServer
              .find(Program.class)
              .alias("active")
              .where()
              .eq("lifecycle_stage", LifecycleStage.ACTIVE)
              .notExists(
                  ebeanServer
                      .find(Program.class)
                      .where()
                      .raw("active.name = name")
                      .eq("lifecycle_stage", LifecycleStage.DRAFT)
                      .query())
              .findList()) {
        uneditedActive.setVersion(version);
        this.programRepository.insertProgramSync(uneditedActive);
      }
      // Then, for each program, if a draft exists, publish the draft.
      for (Program draft :
          ebeanServer
              .find(Program.class)
              .where()
              .eq("lifecycle_stage", LifecycleStage.DRAFT)
              .findList()) {
        draft.setVersion(version);
        this.publishProgramAsync(draft, transaction);
      }
      ebeanServer.commitTransaction();
    } finally {
      ebeanServer.endTransaction();
    }
  }

  public long getNextVersion() {
    Optional<Program> draft =
        ebeanServer
            .find(Program.class)
            .where()
            .eq("lifecycle_stage", LifecycleStage.DRAFT)
            .findOneOrEmpty();
    if (draft.isPresent()) {
      return draft.get().getVersion();
    }
    // If there are no drafts, add one to any active program
    Optional<Program> active =
        ebeanServer
            .find(Program.class)
            .where()
            .eq("lifecycle_stage", LifecycleStage.ACTIVE)
            .findOneOrEmpty();
    if (active.isPresent()) {
      return active.get().getVersion() + 1;
    }
    return 1L;
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

  private CompletionStage<Void> publishProgramAsync(Program program, Transaction transaction) {
    // First, set this program to ACTIVE and set the existing ACTIVE program (if it exists)
    // to OBSOLETE.
    Optional<Program> oldProgramMaybe =
        ebeanServer
            .find(Program.class)
            .usingTransaction(transaction)
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
        setQuestionLive(question.id());
      }
    }

    return null;
  }

  /**
   * Set this question to ACTIVE state, and set any existing ACTIVE question with the same name to
   * the OBSOLETE state.
   */
  public void setQuestionLive(long questionId) {
    Question published = ebeanServer.find(Question.class, questionId);
    Optional<Question> existingMaybe =
        findActiveVersionOfQuestion(questionId, published.getQuestionDefinition().getName());
    if (existingMaybe.isPresent()) {
      Question existing = existingMaybe.get();
      existing.setLifecycleStage(LifecycleStage.OBSOLETE);
      existing.save();
    }
    published.setLifecycleStage(LifecycleStage.ACTIVE);
    published.save();
  }

  /** Find an ACTIVE version of the question specified, other than the given ID. */
  public Optional<Question> findActiveVersionOfQuestion(long questionId, String questionName) {
    return ebeanServer
        .find(Question.class)
        .where()
        .eq("lifecycle_stage", LifecycleStage.ACTIVE.getValue())
        .eq("name", questionName)
        .ne("id", questionId)
        .findOneOrEmpty();
  }

  public void updateProgramsForNewDraftQuestion(
      Question newDraft, long oldId, Transaction transaction) {
    // Get all existing draft programs.
    Stream<Program> draftStream =
        ebeanServer
            .find(Program.class)
            .where()
            .eq("lifecycle_stage", LifecycleStage.DRAFT)
            .findList()
            .stream();

    // Get all programs where there is no draft but where the active version references
    // this question, and create a draft.
    Stream<Program> newDraftsStream =
        ebeanServer
            .find(Program.class)
            .alias("programs")
            .where()
            .eq("lifecycle_stage", LifecycleStage.ACTIVE)
            .notExists(
                ebeanServer
                    .find(Program.class)
                    .where()
                    .raw("programs.name = name")
                    .eq("lifecycle_stage", LifecycleStage.DRAFT)
                    .query())
            .findList()
            .stream()
            .filter(
                program -> {
                  for (BlockDefinition blockDefinition :
                      program.getProgramDefinition().blockDefinitions()) {
                    for (ProgramQuestionDefinition question :
                        blockDefinition.programQuestionDefinitions()) {
                      if (question.id() == oldId) {
                        return true;
                      }
                    }
                  }
                  return false;
                })
            .map(
                program -> {
                  ProgramDefinition.Builder newDraftProgram =
                      program.getProgramDefinition().toBuilder()
                          .setLifecycleStage(LifecycleStage.DRAFT);
                  Program programBean = newDraftProgram.build().toProgram();
                  programBean.setVersion(getNextVersion());
                  programRepository.insertProgramSync(programBean);
                  return programBean;
                });

    Streams.concat(draftStream, newDraftsStream)
        .forEach(
            program -> {
              // Create a draft version of each of those programs.
              updateQuestionVersions(program, transaction);
            });
  }
}
