package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.function.Predicate.not;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.SerializableConflictException;
import io.ebean.Transaction;
import io.ebean.TxScope;
import io.ebean.annotation.TxIsolation;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
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
import services.program.CantPublishProgramWithSharedQuestionsException;
import services.program.EligibilityDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinition;
import services.program.predicate.AndNode;
import services.program.predicate.LeafAddressServiceAreaExpressionNode;
import services.program.predicate.LeafOperationExpressionNode;
import services.program.predicate.OrNode;
import services.program.predicate.PredicateDefinition;
import services.program.predicate.PredicateExpressionNode;
import services.question.types.QuestionDefinition;

/** A repository object for dealing with versioning of questions and programs. */
public final class VersionRepository {

  private static final Logger logger = LoggerFactory.getLogger(VersionRepository.class);
  private final Database database;
  private final ProgramRepository programRepository;
  private final DatabaseExecutionContext databaseExecutionContext;

  @Inject
  public VersionRepository(
      ProgramRepository programRepository, DatabaseExecutionContext databaseExecutionContext) {
    this.database = DB.getDefault();
    this.programRepository = checkNotNull(programRepository);
    this.databaseExecutionContext = checkNotNull(databaseExecutionContext);
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
              !draft.getPrograms().isEmpty() || !draft.getQuestions().isEmpty(),
              "Must have at least 1 program or question in the draft version.");
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

  /**
   * Publish the specified DRAFT program and its modified questions. No other programs/questions
   * will be published. The DRAFT program and its DRAFT questions will become ACTIVE. The ACTIVE
   * version of all other programs and questions will be copied to the new ACTIVE version. The DRAFT
   * version of all other programs and questions will be copied to a new DRAFT version.
   *
   * @throws CantPublishProgramWithSharedQuestionsException if any of the program's modified
   *     questions are referenced by other programs. In that case, this program can't be published
   *     individually because publishing its questions would affect other programs.
   * @throws ProgramNotFoundException if the specified program is not found in the DRAFT.
   */
  public void publishNewSynchronizedVersion(String programToPublishAdminName)
      throws CantPublishProgramWithSharedQuestionsException, ProgramNotFoundException {
    Transaction transaction =
        database.beginTransaction(TxScope.requiresNew().setIsolation(TxIsolation.SERIALIZABLE));
    try {
      Version existingDraft = getDraftVersion();
      Version active = getActiveVersion();
      // Any drafts not being published right now will be moved to newDraft.
      Version newDraft = new Version(LifecycleStage.DRAFT);
      database.insert(newDraft);

      Program programToPublish =
          existingDraft
              .getProgramByName(programToPublishAdminName)
              .orElseThrow(() -> new ProgramNotFoundException(programToPublishAdminName));

      ImmutableSet<String> questionsToPublishNames =
          getProgramQuestionNamesInVersion(programToPublish.getProgramDefinition(), existingDraft);

      // Check if any draft questions referenced by programToPublish are also referenced by other
      // programs. If so, publishing the program is disallowed.
      // We only need to look at draft programs because if a question has been modified, any
      // programs that reference it will have been added to the draft.
      if (anyQuestionIsShared(existingDraft, questionsToPublishNames)) {
        throw new CantPublishProgramWithSharedQuestionsException();
      }

      // Move everything we're not publishing right now to the new draft.
      existingDraft.getPrograms().stream()
          .filter(
              program ->
                  !program.getProgramDefinition().adminName().equals(programToPublishAdminName))
          .forEach(
              program -> {
                newDraft.addProgram(program);
                existingDraft.removeProgram(program);
              });
      existingDraft.getQuestions().stream()
          .filter(
              question ->
                  !questionsToPublishNames.contains(question.getQuestionDefinition().getName()))
          .forEach(
              question -> {
                newDraft.addQuestion(question);
                existingDraft.removeQuestion(question);
              });

      // Associate any active programs and questions that aren't present in the draft with the
      // draft.
      active.getPrograms().stream()
          .filter(
              activeProgram ->
                  !programToPublishAdminName.equals(
                      activeProgram.getProgramDefinition().adminName()))
          .forEach(existingDraft::addProgram);
      active.getQuestions().stream()
          .filter(
              activeQuestion ->
                  !questionsToPublishNames.contains(
                      activeQuestion.getQuestionDefinition().getName()))
          .forEach(existingDraft::addQuestion);

      // Move forward the ACTIVE version.
      active.setLifecycleStage(LifecycleStage.OBSOLETE);
      existingDraft.setLifecycleStage(LifecycleStage.ACTIVE);

      existingDraft.save();
      active.save();
      newDraft.save();
      transaction.commit();
    } catch (NonUniqueResultException | SerializableConflictException | RollbackException e) {
      transaction.rollback(e);
      // We must end the transaction here since we are going to recurse and try again.
      // We cannot have this transaction on the thread-local transaction stack when that
      // happens.
      transaction.end();
      // Since this is in a `SERIALIZABLE` transaction,  the transaction runs as if it is the
      // the only transaction running on the whole database. According to the docs "applications
      // using this level must be prepared to retry transactions due to serialization failures."
      // We recurse to retry here.
      // https://www.postgresql.org/docs/9.1/transaction-iso.html#XACT-SERIALIZABLE.
      publishNewSynchronizedVersion(programToPublishAdminName);
    } finally {
      // This may come after a prior call to `transaction.end` in the event of a
      // precondition failure - this is okay, since it a double-call to `end` on
      // a particular transaction.  Only double calls to database.endTransaction
      // must be avoided.
      transaction.end();
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

  /** Returns true if the program is a member of the current draft version. */
  public boolean isDraft(Program program) {
    return isDraftProgram(program.id);
  }

  /** Returns true if the program with the provided id is a member of the current draft version. */
  public boolean isDraftProgram(Long programId) {
    return getDraftVersion().getPrograms().stream()
        .anyMatch(draftProgram -> draftProgram.id.equals(programId));
  }

  /** Returns true if the program with the provided id is a member of the current draft version. */
  public CompletionStage<Boolean> isDraftProgramAsync(Long programId) {
    return CompletableFuture.supplyAsync(() -> isDraftProgram(programId), databaseExecutionContext);
  }

  /** Returns true if the program with the provided id is a member of the current active version. */
  public boolean isActiveProgram(Long programId) {
    return getActiveVersion().getPrograms().stream()
        .anyMatch(activeProgram -> activeProgram.id.equals(programId));
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
    if (block.eligibilityDefinition().isPresent()) {
      EligibilityDefinition eligibilityDefinition = block.eligibilityDefinition().get();
      PredicateDefinition oldPredicate = eligibilityDefinition.predicate();
      PredicateDefinition newPredicate =
          PredicateDefinition.create(
              updatePredicateNodeVersions(oldPredicate.rootNode()), oldPredicate.action());

      updatedBlock.setEligibilityDefinition(
          eligibilityDefinition.toBuilder().setPredicate(newPredicate).build());
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

  /**
   * Inspects the provided version and returns a map where the key is the question name and the
   * value is a set of programs that reference the given question in this version.
   */
  public static ImmutableMap<String, ImmutableSet<ProgramDefinition>> buildReferencingProgramsMap(
      Version version) {
    ImmutableMap<Long, String> questionIdToNameLookup = getQuestionIdToNameMap(version);
    Map<String, Set<ProgramDefinition>> result = Maps.newHashMap();
    for (Program program : version.getPrograms()) {
      ImmutableSet<String> programQuestionNames =
          getProgramQuestionNames(program.getProgramDefinition(), questionIdToNameLookup);
      for (String questionName : programQuestionNames) {
        if (!result.containsKey(questionName)) {
          result.put(questionName, Sets.newHashSet());
        }
        result.get(questionName).add(program.getProgramDefinition());
      }
    }
    return result.entrySet().stream()
        .collect(
            ImmutableMap.toImmutableMap(Entry::getKey, e -> ImmutableSet.copyOf(e.getValue())));
  }

  /** Returns the names of questions referenced by the program that are in the specified version. */
  public static ImmutableSet<String> getProgramQuestionNamesInVersion(
      ProgramDefinition program, Version version) {
    ImmutableMap<Long, String> questionIdToNameLookup = getQuestionIdToNameMap(version);
    return getProgramQuestionNames(program, questionIdToNameLookup);
  }

  /**
   * Returns true if any questions in the provided set are referenced by multiple programs in the
   * specified version.
   */
  private static boolean anyQuestionIsShared(Version version, ImmutableSet<String> questions) {
    ImmutableMap<String, ImmutableSet<ProgramDefinition>> referencingProgramsByQuestionName =
        buildReferencingProgramsMap(version);
    return questions.stream()
        .anyMatch(
            questionName ->
                referencingProgramsByQuestionName.containsKey(questionName)
                    && referencingProgramsByQuestionName.get(questionName).size() > 1);
  }

  /**
   * Returns a mapping from question ID to question name for questions in the provided version.
   * Different versions of a question can have distinct IDs. The name is an ID that is constant
   * across versions.
   */
  private static ImmutableMap<Long, String> getQuestionIdToNameMap(Version version) {
    return version.getQuestions().stream()
        .map(Question::getQuestionDefinition)
        .collect(
            ImmutableMap.toImmutableMap(QuestionDefinition::getId, QuestionDefinition::getName));
  }

  /**
   * Returns the names of questions referenced by the program, using the provided
   * questionIdToNameLookup to translate from question ID to name. If a question ID is missing from
   * the map, its name won't be included in the final result.
   */
  private static ImmutableSet<String> getProgramQuestionNames(
      ProgramDefinition program, ImmutableMap<Long, String> questionIdToNameLookup) {
    return program.blockDefinitions().stream()
        .map(BlockDefinition::programQuestionDefinitions)
        .flatMap(ImmutableList::stream)
        .map(ProgramQuestionDefinition::id)
        .filter(questionIdToNameLookup::containsKey)
        .map(questionIdToNameLookup::get)
        .collect(ImmutableSet.toImmutableSet());
  }
}
