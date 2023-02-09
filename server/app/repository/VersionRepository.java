package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.function.Predicate.not;

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
import java.util.Optional;
import java.util.function.Predicate;
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
import services.program.predicate.LeafAddressServiceAreaExpressionNode;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.OrNode;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;

/** A repository object for dealing with versioning of questions and programs. */
public final class VersionRepository {

  private static final Logger logger = LoggerFactory.getLogger(VersionRepository.class);
  private final Database database;
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
    publishNewSynchronizedVersion(PublishMode.PUBLISH_CHANGES);
  }

  /**
   * Simulates publishing a new version of all programs and questions. All DRAFT programs/questions
   * will become ACTIVE, and all ACTIVE programs/questions without a draft will be copied to the
   * next version. This method will not mutate the database and will return an updated Version
   * corresponding to what would be the new ACTIVE version.
   */
  public Version previewPublishNewSynchronizedVersion() {
    return publishNewSynchronizedVersion(PublishMode.DRY_RUN);
  }

  private enum PublishMode {
    DRY_RUN,
    PUBLISH_CHANGES,
  }

  private Version publishNewSynchronizedVersion(PublishMode publishMode) {
    try {
      // Regardless of whether changes are published or not, we still perform
      // this operation inside of a transaction in order to ensure we have
      // consistent reads.
      database.beginTransaction();
      Version draft = getDraftVersion();
      Version active = getActiveVersion();

      ImmutableSet<String> draftProgramsNames = draft.getProgramsNames();
      ImmutableSet<String> draftQuestionNames = draft.getQuestionNames();

      // Is a program being deleted in the draft version?
      Predicate<Program> programIsDeletedInDraft =
          program -> draft.programIsTombstoned(program.getProgramDefinition().adminName());
      // Is a question being deleted in the draft version?
      Predicate<Question> questionIsDeletedInDraft =
          question -> draft.questionIsTombstoned(question.getQuestionDefinition().getName());

      // Associate any active programs that aren't present in the draft with the draft.
      active.getPrograms().stream()
          // Exclude programs deleted in the draft.
          .filter(not(programIsDeletedInDraft))
          // Exclude programs that are in the draft already.
          .filter(
              activeProgram ->
                  !draftProgramsNames.contains(activeProgram.getProgramDefinition().adminName()))
          // For each active program not associated with the draft, associate it with the draft.
          // The relationship between Programs and Versions is many-to-may. When updating the
          // relationship, one of the EBean models needs to be saved. We update the Version
          // side of the relationship rather than the Program side in order to prevent the
          // save causing the "updated" timestamp to be changed for a Program. We intend for
          // that timestamp only to be updated for actual changes to the program.
          .forEach(draft::addProgram);

      // Associate any active questions that aren't present in the draft with the draft.
      active.getQuestions().stream()
          // Exclude questions deleted in the draft.
          .filter(not(questionIsDeletedInDraft))
          // Exclude questions that are in the draft already.
          .filter(
              activeQuestion ->
                  !draftQuestionNames.contains(activeQuestion.getQuestionDefinition().getName()))
          // For each active question not associated with the draft, associate it with the draft.
          // The relationship between Questions and Versions is many-to-may. When updating the
          // relationship, one of the EBean models needs to be saved. We update the Version
          // side of the relationship rather than the Question side in order to prevent the
          // save causing the "updated" timestamp to be changed for a Question. We intend for
          // that timestamp only to be updated for actual changes to the question.
          .forEach(draft::addQuestion);

      // Remove any questions / programs both added and archived in the current version.
      draft.getQuestions().stream()
          .filter(questionIsDeletedInDraft)
          .forEach(
              questionToDelete -> {
                draft.removeTombstoneForQuestion(questionToDelete);
                draft.removeQuestion(questionToDelete);
              });
      draft.getPrograms().stream()
          .filter(programIsDeletedInDraft)
          .forEach(
              programToDelete -> {
                draft.removeTombstoneForProgram(programToDelete);
                draft.removeProgram(programToDelete);
              });

      // Move forward the ACTIVE version.
      active.setLifecycleStage(LifecycleStage.OBSOLETE);
      draft.setLifecycleStage(LifecycleStage.ACTIVE);

      switch (publishMode) {
        case PUBLISH_CHANGES:
          Preconditions.checkState(
              draft.getPrograms().size() > 0, "Must have at least 1 program in the draft version.");
          draft.save();
          active.save();
          draft.refresh();
          active.refresh();
          break;
        case DRY_RUN:
          break;
        default:
          throw new RuntimeException(String.format("unrecognized publishMode: %s", publishMode));
      }
      database.commitTransaction();

      return draft;
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
    }
    // Suspends any existing thread-local transaction if one exists.
    // This method is often called by two portions of the same outer transaction, microseconds
    // apart.  It's extremely important that there only ever be one draft version, so we need the
    // highest transaction isolation level; `SERIALIZABLE` means that the two transactions run as if
    // each transaction was the only transaction running on the whole database.  That is, if any
    // other code accesses these rows or executes any query which would modify them, the transaction
    // is rolled back (a RollbackException is thrown).  We are forced to retry.  This is expensive
    // in relative terms, but new drafts are very rare.  It is unlikely this will represent a real
    // performance penalty for any applicant - or even any admin, really.
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

  public Version getActiveVersion() {
    return database
        .find(Version.class)
        .where()
        .eq("lifecycle_stage", LifecycleStage.ACTIVE)
        .findOne();
  }

  /**
   * Given any revision of a question, return the most recent conceptual version of it. Will return
   * the current DRAFT version if present then the current ACTIVE version.
   */
  public Optional<Question> getLatestVersionOfQuestion(long questionId) {
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
   * question, which is either DRAFT or ACTIVE. If it is not, update the pointer to the most
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
      logger.trace("Updating screen (block) {}.", block.id());
      updatedDefinition.addBlockDefinition(updateQuestionVersions(draftProgram.id, block));
    }
    draftProgram = new Program(updatedDefinition.build());
    logger.trace("Submitting update.");
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
    return isDraftProgram(program.id);
  }

  public boolean isDraftProgram(Long programId) {
    return getDraftVersion().getPrograms().stream()
        .anyMatch(draftProgram -> draftProgram.id.equals(programId));
  }

  private BlockDefinition updateQuestionVersions(long programDefinitionId, BlockDefinition block) {
    BlockDefinition.Builder updatedBlock =
        block.toBuilder().setProgramQuestionDefinitions(ImmutableList.of());
    // Update questions contained in this block.
    for (ProgramQuestionDefinition question : block.programQuestionDefinitions()) {
      Optional<Question> updatedQuestion = getLatestVersionOfQuestion(question.id());
      logger.trace(
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
              updatePredicateNodeVersions(oldPredicate.rootNode()), oldPredicate.action()));
    }
    if (block.optionalPredicate().isPresent()) {
      PredicateDefinition oldPredicate = block.optionalPredicate().get();
      updatedBlock.setOptionalPredicate(
          Optional.of(
              PredicateDefinition.create(
                  updatePredicateNodeVersions(oldPredicate.rootNode()), oldPredicate.action())));
    }
    return updatedBlock.build();
  }

  /**
   * Updates the referenced question IDs in all leaf nodes of {@code node} to the latest versions.
   *
   * <p>Since nodes are immutable, we recursively recreate the tree with updated leaf nodes and
   * return it.
   */
  @VisibleForTesting
  PredicateExpressionNode updatePredicateNodeVersions(PredicateExpressionNode node) {
    switch (node.getType()) {
      case AND:
        AndNode and = node.getAndNode();
        ImmutableList<PredicateExpressionNode> updatedAndChildren =
            and.children().stream()
                .map(this::updatePredicateNodeVersions)
                .collect(toImmutableList());
        return PredicateExpressionNode.create(AndNode.create(updatedAndChildren));
      case OR:
        OrNode or = node.getOrNode();
        ImmutableList<PredicateExpressionNode> updatedOrChildren =
            or.children().stream()
                .map(this::updatePredicateNodeVersions)
                .collect(toImmutableList());
        return PredicateExpressionNode.create(OrNode.create(updatedOrChildren));
      case LEAF_OPERATION:
        LeafOperationExpressionNode leaf = node.getLeafOperationNode();
        Optional<Question> updated = getLatestVersionOfQuestion(leaf.questionId());
        return PredicateExpressionNode.create(
            leaf.toBuilder().setQuestionId(updated.orElseThrow().id).build());
      case LEAF_ADDRESS_SERVICE_AREA:
        LeafAddressServiceAreaExpressionNode leafAddress = node.getLeafAddressNode();
        Optional<Question> updatedQuestion = getLatestVersionOfQuestion(leafAddress.questionId());
        return PredicateExpressionNode.create(
            leafAddress.toBuilder().setQuestionId(updatedQuestion.orElseThrow().id).build());
    }
    // ErrorProne will require the switch handle all types since there isn't a default, we should
    // never get here.
    throw new AssertionError(
        String.format("Predicate type is unhandled and must be: %s", node.getType()));
  }

  /**
   * Update all ACTIVE and DRAFT programs that refer to the question revision {@code oldId}, to
   * refer to the latest revision of all their questions.
   */
  public void updateProgramsThatReferenceQuestion(long oldQuestionId) {
    // Update all DRAFT program revisions that reference the question.
    getDraftVersion().getPrograms().stream()
        .filter(program -> program.getProgramDefinition().hasQuestion(oldQuestionId))
        .forEach(this::updateQuestionVersions);

    // Update any ACTIVE program without a DRAFT that references the question, a new DRAFT is
    // created.
    getActiveVersion().getPrograms().stream()
        .filter(program -> program.getProgramDefinition().hasQuestion(oldQuestionId))
        .filter(
            program ->
                getDraftVersion()
                    .getProgramByName(program.getProgramDefinition().adminName())
                    .isEmpty())
        .forEach(programRepository::createOrUpdateDraft);
  }
}
