package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.SerializableConflictException;
import io.ebean.Transaction;
import io.ebean.TxScope;
import io.ebean.annotation.TxIsolation;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.persistence.NonUniqueResultException;
import javax.persistence.RollbackException;
import models.LifecycleStage;
import models.Program;
import models.Question;
import models.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramQuestionDefinition;
import services.program.predicate.AndNode;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.OrNode;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;

/** A repository object for dealing with versioning of questions and programs. */
public class VersionRepository {

  private final Database database;
  private final Logger LOG = LoggerFactory.getLogger(VersionRepository.class);
  private final ProgramRepository programRepository;

  @Inject
  public VersionRepository(ProgramRepository programRepository) {
    this.database = DB.getDefault();
    this.programRepository = checkNotNull(programRepository);
  }

  /**
   * Publish a new version of all programs and questions. All DRAFT programs/questions will become
   * ACTIVE, and all ACTIVE programs/questions without a draft will be copied to the next version.
   */
  public void publishNewSynchronizedVersion() {
    try {
      database.beginTransaction();
      Version draft = getDraftVersion();
      Version active = getActiveVersion();
      Preconditions.checkState(
          draft.getPrograms().size() > 0, "Must have at least 1 program in the draft version.");
      // Associate any active programs that aren't present in the draft with the draft.
      active.getPrograms().stream()
          // Exclude programs deleted in the draft.
          .filter(
              activeProgram ->
                  !draft.programIsTombstoned(activeProgram.getProgramDefinition().adminName()))
          // Exclude programs that are in the draft already.
          .filter(
              activeProgram ->
                  draft.getPrograms().stream()
                      .noneMatch(
                          draftProgram ->
                              activeProgram
                                  .getProgramDefinition()
                                  .adminName()
                                  .equals(draftProgram.getProgramDefinition().adminName())))
          // For each active program not associated with the draft, associate it with the draft.
          .forEach(
              activeProgramNotInDraft -> {
                activeProgramNotInDraft.addVersion(draft);
                activeProgramNotInDraft.save();
              });

      // Associate any active questions that aren't present in the draft with the draft.
      active.getQuestions().stream()
          // Exclude questions deleted in the draft.
          .filter(
              activeQuestion ->
                  !draft.questionIsTombstoned(activeQuestion.getQuestionDefinition().getName()))
          // Exclude questions that are in the draft already.
          .filter(
              activeQuestion ->
                  draft.getQuestions().stream()
                      .noneMatch(
                          draftQuestion ->
                              activeQuestion
                                  .getQuestionDefinition()
                                  .getName()
                                  .equals(draftQuestion.getQuestionDefinition().getName())))
          // For each active question not associated with the draft, associate it with the draft.
          .forEach(
              activeQuestionNotInDraft -> {
                activeQuestionNotInDraft.addVersion(draft);
                activeQuestionNotInDraft.save();
              });
      // Move forward the ACTIVE version.
      active.setLifecycleStage(LifecycleStage.OBSOLETE);
      draft.setLifecycleStage(LifecycleStage.ACTIVE);
      active.save();
      draft.save();
      draft.refresh();
      database.commitTransaction();
    } finally {
      database.endTransaction();
    }
  }

  /** Get the current draft version. Creates it if one does not exist. */
  public Version getDraftVersion() {
    Optional<Version> version =
        database
            .find(Version.class)
            .where()
            .eq("lifecycle_stage", LifecycleStage.DRAFT)
            .findOneOrEmpty();
    if (version.isPresent()) {
      return version.get();
    } else {
      // Suspends any existing thread-local transaction if one exists.
      // This method is often called by two portions of the same outer transaction,
      // microseconds apart.  It's extremely important that there only ever be one
      // draft version, so we need the highest transaction isolation level -
      // `SERIALIZABLE` means that the two transactions run as if each transaction
      // was the only transaction running on the whole database.  That is, if any
      // other code accesses these rows or executes any query which would modify them,
      // the transaction is rolled back (a RollbackException is thrown).  We
      // are forced to retry.  This is expensive in relative terms, but new drafts
      // are very rare.  It is unlikely this will represent a real performance penalty
      // for any applicant - or even any admin, really.
      Transaction transaction =
          database.beginTransaction(TxScope.requiresNew().setIsolation(TxIsolation.SERIALIZABLE));
      try {
        Version newDraftVersion = new Version(LifecycleStage.DRAFT);
        database.insert(newDraftVersion);
        database
            .find(Version.class)
            .forUpdate()
            .where()
            .eq("lifecycle_stage", LifecycleStage.DRAFT)
            .findOne();
        transaction.commit();
        return newDraftVersion;
      } catch (NonUniqueResultException | SerializableConflictException | RollbackException e) {
        transaction.rollback(e);
        // We must end the transaction here since we are going to recurse and try again.
        // We cannot have this transaction on the thread-local transaction stack when that
        // happens.
        transaction.end();
        return getDraftVersion();
      } finally {
        // This may come after a prior call to `transaction.end` in the event of a
        // precondition failure - this is okay, since it a double-call to `end` on
        // a particular transaction.  Only double calls to database.endTransaction
        // must be avoided.
        transaction.end();
      }
    }
  }

  public Version getActiveVersion() {
    return database
        .find(Version.class)
        .where()
        .eq("lifecycle_stage", LifecycleStage.ACTIVE)
        .findOne();
  }

  private Optional<Question> getLatestVersionOfQuestion(long questionId) {
    String questionName =
        database.find(Question.class).setId(questionId).select("name").findSingleAttribute();
    Optional<Question> draftQuestion =
        getDraftVersion().getQuestions().stream()
            .filter(question -> question.getQuestionDefinition().getName().equals(questionName))
            .findFirst();
    if (draftQuestion.isPresent()) {
      return draftQuestion;
    }
    return getActiveVersion().getQuestions().stream()
        .filter(question -> question.getQuestionDefinition().getName().equals(questionName))
        .findFirst();
  }

  /**
   * For each question in this program, check whether it is the most up-to-date version of the
   * question which is either DRAFT or ACTIVE. If it is not, update the pointer to the most
   * up-to-date version of the question, using the given transaction. This method can only be called
   * on a draft program.
   */
  public void updateQuestionVersions(Program draftProgram) {
    Preconditions.checkArgument(isInactive(draftProgram), "input program must not be active.");
    Preconditions.checkArgument(
        isDraft(draftProgram), "input program must be in the current draft version.");
    ProgramDefinition.Builder updatedDefinition =
        draftProgram.getProgramDefinition().toBuilder().setBlockDefinitions(ImmutableList.of());
    for (BlockDefinition block : draftProgram.getProgramDefinition().blockDefinitions()) {
      LOG.trace("Updating screen (block) {}.", block.id());
      updatedDefinition.addBlockDefinition(updateQuestionVersions(draftProgram.id, block));
    }
    draftProgram = new Program(updatedDefinition.build());
    LOG.trace("Submitting update.");
    database.update(draftProgram);
    draftProgram.refresh();
  }

  public boolean isInactive(Question question) {
    return !getActiveVersion().getQuestions().stream()
        .anyMatch(activeQuestion -> activeQuestion.id.equals(question.id));
  }

  public boolean isInactive(Program program) {
    return !getActiveVersion().getPrograms().stream()
        .anyMatch(activeProgram -> activeProgram.id.equals(program.id));
  }

  public boolean isDraft(Question question) {
    return getDraftVersion().getQuestions().stream()
        .anyMatch(draftQuestion -> draftQuestion.id.equals(question.id));
  }

  public boolean isDraft(Program program) {
    return getDraftVersion().getPrograms().stream()
        .anyMatch(draftProgram -> draftProgram.id.equals(program.id));
  }

  private BlockDefinition updateQuestionVersions(long programDefinitionId, BlockDefinition block) {
    BlockDefinition.Builder updatedBlock =
        block.toBuilder().setProgramQuestionDefinitions(ImmutableList.of());
    // Update questions contained in this block.
    for (ProgramQuestionDefinition question : block.programQuestionDefinitions()) {
      Optional<Question> updatedQuestion = getLatestVersionOfQuestion(question.id());
      LOG.trace(
          "Updating question ID {} to new ID {}.", question.id(), updatedQuestion.orElseThrow().id);
      updatedBlock.addQuestion(
          question.loadCompletely(
              programDefinitionId, updatedQuestion.orElseThrow().getQuestionDefinition()));
    }
    // Update questions referenced in this block's predicate(s)
    if (block.visibilityPredicate().isPresent()) {
      PredicateDefinition oldPredicate = block.visibilityPredicate().get();
      updatedBlock.setVisibilityPredicate(
          PredicateDefinition.create(
              updatePredicateNode(oldPredicate.rootNode()), oldPredicate.action()));
    }
    if (block.optionalPredicate().isPresent()) {
      PredicateDefinition oldPredicate = block.optionalPredicate().get();
      updatedBlock.setOptionalPredicate(
          Optional.of(
              PredicateDefinition.create(
                  updatePredicateNode(oldPredicate.rootNode()), oldPredicate.action())));
    }
    return updatedBlock.build();
  }

  // Update the referenced question IDs in all leaf nodes. Since nodes are immutable, we
  // recursively recreate the tree with updated leaf nodes.
  @VisibleForTesting
  protected PredicateExpressionNode updatePredicateNode(PredicateExpressionNode current) {
    switch (current.getType()) {
      case AND:
        AndNode and = current.getAndNode();
        ImmutableSet<PredicateExpressionNode> updatedAndChildren =
            and.children().stream().map(this::updatePredicateNode).collect(toImmutableSet());
        return PredicateExpressionNode.create(AndNode.create(updatedAndChildren));
      case OR:
        OrNode or = current.getOrNode();
        ImmutableSet<PredicateExpressionNode> updatedOrChildren =
            or.children().stream().map(this::updatePredicateNode).collect(toImmutableSet());
        return PredicateExpressionNode.create(OrNode.create(updatedOrChildren));
      case LEAF_OPERATION:
        LeafOperationExpressionNode leaf = current.getLeafNode();
        Optional<Question> updated = getLatestVersionOfQuestion(leaf.questionId());
        return PredicateExpressionNode.create(
            leaf.toBuilder().setQuestionId(updated.orElseThrow().id).build());
      default:
        return current;
    }
  }

  public void updateProgramsForNewDraftQuestion(long oldId) {
    getDraftVersion().getPrograms().stream()
        .filter(program -> program.getProgramDefinition().hasQuestion(oldId))
        .forEach(program -> updateQuestionVersions(program));

    getActiveVersion().getPrograms().stream()
        .filter(program -> program.getProgramDefinition().hasQuestion(oldId))
        .filter(
            program ->
                getDraftVersion()
                    .getProgramByName(program.getProgramDefinition().adminName())
                    .isEmpty())
        .forEach(program -> programRepository.createOrUpdateDraft(program));
  }

  public List<Version> listAllVersions() {
    return database.find(Version.class).findList();
  }

  public void setLive(long versionId) {
    Version draftVersion = getDraftVersion();
    Version activeVersion = getActiveVersion();
    Version newActiveVersion = database.find(Version.class).setId(versionId).findOne();
    newActiveVersion.setLifecycleStage(LifecycleStage.ACTIVE);
    newActiveVersion.save();
    activeVersion.setLifecycleStage(LifecycleStage.OBSOLETE);
    activeVersion.save();
    draftVersion.setLifecycleStage(LifecycleStage.DELETED);
    draftVersion.save();
  }
}
