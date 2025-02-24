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
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.RollbackException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import javax.inject.Inject;
import models.DisplayMode;
import models.LifecycleStage;
import models.ProgramModel;
import models.QuestionModel;
import models.VersionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.cache.NamedCache;
import play.cache.SyncCacheApi;
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
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionDefinition;
import services.settings.SettingsManifest;

/** A repository object for dealing with versioning of questions and programs. */
public final class VersionRepository {

  private static final Logger logger = LoggerFactory.getLogger(VersionRepository.class);
  private static final QueryProfileLocationBuilder profileLocationBuilder =
      new QueryProfileLocationBuilder("VersionRepository");
  private final Database database;
  private final ProgramRepository programRepository;
  private final QuestionRepository questionRepository;
  private final DatabaseExecutionContext databaseExecutionContext;
  private final SettingsManifest settingsManifest;
  private final SyncCacheApi questionsByVersionCache;
  private final SyncCacheApi programsByVersionCache;

  @Inject
  public VersionRepository(
      ProgramRepository programRepository,
      QuestionRepository questionRepository,
      DatabaseExecutionContext databaseExecutionContext,
      SettingsManifest settingsManifest,
      @NamedCache("version-questions") SyncCacheApi questionsByVersionCache,
      @NamedCache("version-programs") SyncCacheApi programsByVersionCache) {
    this.database = DB.getDefault();
    this.programRepository = checkNotNull(programRepository);
    this.questionRepository = checkNotNull(questionRepository);
    this.databaseExecutionContext = checkNotNull(databaseExecutionContext);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.questionsByVersionCache = checkNotNull(questionsByVersionCache);
    this.programsByVersionCache = checkNotNull(programsByVersionCache);
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
  public VersionModel previewPublishNewSynchronizedVersion() {
    return publishNewSynchronizedVersion(PublishMode.DRY_RUN);
  }

  private enum PublishMode {
    DRY_RUN,
    PUBLISH_CHANGES,
  }

  private VersionModel publishNewSynchronizedVersion(PublishMode publishMode) {
    // Regardless of whether changes are published or not, we still perform
    // this operation inside of a transaction in order to ensure we have
    // consistent reads.
    Transaction transaction =
        database.beginTransaction(TxScope.required().setIsolation(TxIsolation.SERIALIZABLE));
    try {
      VersionModel draft = getDraftVersionOrCreate();
      VersionModel active = getActiveVersion();

      ImmutableSet<String> draftProgramsNames = getProgramNamesForVersion(draft);
      ImmutableSet<String> draftQuestionNames = getQuestionNamesForVersion(draft);

      // Is a program being deleted in the draft version?
      Predicate<ProgramModel> programIsDeletedInDraft =
          program ->
              draft.programIsTombstoned(
                  programRepository.getShallowProgramDefinition(program).adminName());
      // Is a question being deleted in the draft version?
      Predicate<QuestionModel> questionIsDeletedInDraft =
          question ->
              draft.questionIsTombstoned(
                  questionRepository.getQuestionDefinition(question).getName());

      // Associate any active programs that aren't present in the draft with the draft.
      getProgramsForVersionWithoutCache(active).stream()
          // Exclude programs deleted in the draft.
          .filter(not(programIsDeletedInDraft))
          // Exclude programs that are in the draft already.
          .filter(
              activeProgram ->
                  !draftProgramsNames.contains(
                      programRepository.getShallowProgramDefinition(activeProgram).adminName()))
          // For each active program not associated with the draft, associate it with the draft.
          // The relationship between Programs and Versions is many-to-may. When updating the
          // relationship, one of the EBean models needs to be saved. We update the Version
          // side of the relationship rather than the Program side in order to prevent the
          // save causing the "updated" timestamp to be changed for a Program. We intend for
          // that timestamp only to be updated for actual changes to the program.
          .forEach(
              program -> {
                draft.addProgram(program);
              });

      // Associate any active questions that aren't present in the draft with the draft.
      getQuestionsForVersionWithoutCache(active).stream()
          // Exclude questions deleted in the draft.
          .filter(not(questionIsDeletedInDraft))
          // Exclude questions that are in the draft already.
          .filter(
              activeQuestion ->
                  !draftQuestionNames.contains(
                      questionRepository.getQuestionDefinition(activeQuestion).getName()))
          // For each active question not associated with the draft, associate it with the draft.
          // The relationship between Questions and Versions is many-to-may. When updating the
          // relationship, one of the EBean models needs to be saved. We update the Version
          // side of the relationship rather than the Question side in order to prevent the
          // save causing the "updated" timestamp to be changed for a Question. We intend for
          // that timestamp only to be updated for actual changes to the question.
          .forEach(draft::addQuestion);

      // Remove any questions / programs both added and archived in the current version.
      getQuestionsForVersion(draft).stream()
          .filter(questionIsDeletedInDraft)
          .forEach(
              questionToDelete -> {
                draft.removeTombstoneForQuestion(questionToDelete);
                draft.removeQuestion(questionToDelete);
              });
      getProgramsForVersion(draft).stream()
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
              !getProgramsForVersion(draft).isEmpty() || !getQuestionsForVersion(draft).isEmpty(),
              "Must have at least 1 program or question in the draft version.");
          draft.save();
          active.save();
          draft.refresh();
          active.refresh();
          validateProgramQuestionState();
          break;
        case DRY_RUN:
          break;
        default:
          throw new RuntimeException(String.format("unrecognized publishMode: %s", publishMode));
      }
      transaction.commit();
      return draft;
    } finally {
      transaction.end();
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
        database.beginTransaction(TxScope.required().setIsolation(TxIsolation.SERIALIZABLE));
    try {
      VersionModel existingDraft = getDraftVersionOrCreate();
      VersionModel active = getActiveVersion();

      // Any drafts not being published right now will be moved to newDraft.
      VersionModel newDraft = new VersionModel(LifecycleStage.DRAFT);
      database.insert(newDraft);

      ProgramModel programToPublish =
          getProgramByNameForVersion(programToPublishAdminName, existingDraft)
              .orElseThrow(() -> new ProgramNotFoundException(programToPublishAdminName));

      ImmutableSet<String> questionsToPublishNames =
          getProgramQuestionNamesInVersion(
              programRepository.getShallowProgramDefinition(programToPublish), existingDraft);

      // Check if any draft questions referenced by programToPublish are also referenced by other
      // programs. If so, publishing the program is disallowed.
      // We only need to look at draft programs because if a question has been modified, any
      // programs that reference it will have been added to the draft.
      if (anyQuestionIsShared(existingDraft, questionsToPublishNames)) {
        throw new CantPublishProgramWithSharedQuestionsException();
      }

      // Move everything we're not publishing right now to the new draft.
      getProgramsForVersionWithoutCache(existingDraft).stream()
          .filter(
              program ->
                  !programRepository
                      .getShallowProgramDefinition(program)
                      .adminName()
                      .equals(programToPublishAdminName))
          .forEach(
              program -> {
                newDraft.addProgram(program);
                existingDraft.removeProgram(program);
              });
      getQuestionsForVersionWithoutCache(existingDraft).stream()
          .filter(
              question ->
                  !questionsToPublishNames.contains(
                      questionRepository.getQuestionDefinition(question).getName()))
          .forEach(
              question -> {
                newDraft.addQuestion(question);
                existingDraft.removeQuestion(question);
              });

      // Associate any active programs and questions that aren't present in the draft with the
      // draft.
      getProgramsForVersion(active).stream()
          .filter(
              activeProgram ->
                  !programToPublishAdminName.equals(
                      programRepository.getShallowProgramDefinition(activeProgram).adminName()))
          .forEach(
              program -> {
                existingDraft.addProgram(program);
              });
      getQuestionsForVersion(active).stream()
          .filter(
              activeQuestion ->
                  !questionsToPublishNames.contains(
                      questionRepository.getQuestionDefinition(activeQuestion).getName()))
          .forEach(existingDraft::addQuestion);

      // Move forward the ACTIVE version.
      active.setLifecycleStage(LifecycleStage.OBSOLETE);
      existingDraft.setLifecycleStage(LifecycleStage.ACTIVE);

      existingDraft.save();
      active.save();
      newDraft.save();
      active.refresh();
      newDraft.refresh();
      validateProgramQuestionState();
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

  /** Get the current draft version. Empty optional if not available. */
  public Optional<VersionModel> getDraftVersion() {
    return database
        .find(VersionModel.class)
        .where()
        .eq("lifecycle_stage", LifecycleStage.DRAFT)
        .setLabel("VersionModel.findDraft")
        .setProfileLocation(profileLocationBuilder.create("getDraftVersion"))
        .findOneOrEmpty();
  }

  /** Get the current draft version. Creates it if one does not exist. */
  public VersionModel getDraftVersionOrCreate() {
    Optional<VersionModel> version = getDraftVersion();

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
        database.beginTransaction(TxScope.required().setIsolation(TxIsolation.SERIALIZABLE));
    try {
      VersionModel newDraftVersion = new VersionModel(LifecycleStage.DRAFT);
      database.insert(newDraftVersion);
      database
          .find(VersionModel.class)
          .forUpdate()
          .where()
          .eq("lifecycle_stage", LifecycleStage.DRAFT)
          .setLabel("VersionModel.findDraft")
          .setProfileLocation(profileLocationBuilder.create("getDraftVersionOrCreate"))
          .findOne();
      transaction.commit();
      return newDraftVersion;
    } catch (NonUniqueResultException | SerializableConflictException | RollbackException e) {
      // If we are in a nested transaction any serialization exceptions will be thrown when the
      // outer transaction is committed, so here we only handle the case where this is the only
      // transaction.
      transaction.rollback(e);
      // We must end the transaction here since we are going to recurse and try again.
      // We cannot have this transaction on the thread-local transaction stack when that
      // happens.
      transaction.end();
      return getDraftVersionOrCreate();
    } finally {
      // This may come after a prior call to `transaction.end` in the event of a
      // precondition failure - this is okay, since it a double-call to `end` on
      // a particular transaction.  Only double calls to database.endTransaction
      // must be avoided. Additionally, if we're in a nested transaction, `transaction.end()` is a
      // no-op.
      transaction.end();
    }
  }

  public VersionModel getActiveVersion() {
    return database
        .find(VersionModel.class)
        .where()
        .eq("lifecycle_stage", LifecycleStage.ACTIVE)
        .setLabel("VersionModel.findActive")
        .setProfileLocation(profileLocationBuilder.create("getActiveVersion"))
        .findOne();
  }

  public CompletionStage<VersionModel> getActiveVersionAsync() {
    return CompletableFuture.supplyAsync(
        () -> {
          return getActiveVersion();
        },
        databaseExecutionContext);
  }

  /**
   * Returns the previous version to the one passed in. If there is only one version there isn't a
   * previous version so the Optional result will be empty.
   *
   * <p>This can return active or obsolete versions. Versions flagged as deleted are excluded.
   */
  public Optional<VersionModel> getPreviousVersion(VersionModel version) {
    VersionModel previousVersion =
        database
            .find(VersionModel.class)
            .where()
            .lt("id", version.id)
            .not()
            .eq("lifecycle_stage", LifecycleStage.DELETED)
            .orderBy()
            .desc("id")
            .setMaxRows(1)
            .setLabel("VersionModel.findPrevious")
            .setProfileLocation(profileLocationBuilder.create("getPreviousVersion"))
            .findOne();

    return Optional.ofNullable(previousVersion);
  }

  /**
   * Attempts to mark the provided question of a particular version as not eligible for copying to
   * the next version.
   *
   * @return true if the question was successfully marked as tombstoned, false otherwise.
   * @throws QuestionNotFoundException if the question cannot be found in this version.
   */
  public boolean addTombstoneForQuestionInVersion(QuestionModel question, VersionModel version)
      throws QuestionNotFoundException {
    String name = questionRepository.getQuestionDefinition(question).getName();
    if (!getQuestionNamesForVersion(version).contains(name)) {
      throw new QuestionNotFoundException(
          questionRepository.getQuestionDefinition(question).getId());
    }
    return version.addTombstoneForQuestion(name);
  }

  /** Returns the definitions of all the questions for a particular version. */
  public ImmutableList<QuestionDefinition> getQuestionDefinitionsForVersion(VersionModel version) {
    return getQuestionsForVersion(version).stream()
        .map(q -> questionRepository.getQuestionDefinition(q))
        .collect(ImmutableList.toImmutableList());
  }

  /** Returns the definitions of all the questions for an optional version. */
  public ImmutableList<QuestionDefinition> getQuestionDefinitionsForVersion(
      Optional<VersionModel> maybeVersion) {
    return getQuestionsForVersion(maybeVersion).stream()
        .map(q -> questionRepository.getQuestionDefinition(q))
        .collect(ImmutableList.toImmutableList());
  }

  /** Returns the names of all the questions for a particular version. */
  public ImmutableSet<String> getQuestionNamesForVersion(VersionModel version) {
    return getQuestionsForVersion(version).stream()
        .map(q -> questionRepository.getQuestionDefinition(q))
        .map(QuestionDefinition::getName)
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * If a question by the given name exists, return it. A maximum of one question by a given name
   * can exist in a version.
   */
  public Optional<QuestionModel> getQuestionByNameForVersion(String name, VersionModel version) {
    return getQuestionsForVersion(version).stream()
        .filter(q -> questionRepository.getQuestionDefinition(q).getName().equals(name))
        .findAny();
  }

  /** Return the number of programs that exist in a given version. */
  public Long getProgramCountForVersion(VersionModel version) {
    String sql =
        """
        SELECT count(1) FROM versions_programs WHERE versions_programs.versions_id = :versionId
        """;
    return database
        .sqlQuery(sql)
        .setLabel("VersionModel.getProgramCount")
        .setParameter("versionId", version.id)
        .mapToScalar(Long.class)
        .findOneOrEmpty()
        .orElse(0L);
  }

  /** Return the number of questions that exist in a given version. */
  public Long getQuestionCountForVersion(VersionModel version) {
    String sql =
        """
        SELECT count(1) FROM versions_questions WHERE versions_questions.versions_id = :versionId
        """;
    return database
        .sqlQuery(sql)
        .setLabel("VersionModel.getQuestionCount")
        .setParameter("versionId", version.id)
        .mapToScalar(Long.class)
        .findOneOrEmpty()
        .orElse(0L);
  }

  /**
   * Returns the questions for a version.
   *
   * <p>If the cache is enabled, we will get the data from the cache and set it if it is not
   * present.
   */
  public ImmutableList<QuestionModel> getQuestionsForVersion(VersionModel version) {
    // Only set the version cache for active and obsolete versions
    if (settingsManifest.getVersionCacheEnabled() && version.id <= getActiveVersion().id) {
      return questionsByVersionCache.getOrElseUpdate(
          String.valueOf(version.id), () -> version.getQuestions());
    }
    return getQuestionsForVersionWithoutCache(version);
  }

  /** Returns the questions for a version if the version is present. */
  public ImmutableList<QuestionModel> getQuestionsForVersion(Optional<VersionModel> maybeVersion) {
    return maybeVersion.isPresent()
        ? getQuestionsForVersion(maybeVersion.get())
        : ImmutableList.of();
  }

  /** Returns the questions for a version without using the cache. */
  public ImmutableList<QuestionModel> getQuestionsForVersionWithoutCache(VersionModel version) {
    return version.getQuestions();
  }

  /**
   * If a program by the given name exists, return it. A maximum of one program by a given name can
   * exist in a version.
   */
  public Optional<ProgramModel> getProgramByNameForVersion(String name, VersionModel version) {
    return getProgramsForVersion(version).stream()
        .filter(p -> programRepository.getShallowProgramDefinition(p).adminName().equals(name))
        .findAny();
  }

  public Optional<ProgramModel> getProgramByNameForVersion(
      String name, Optional<VersionModel> maybeVersion) {
    return getProgramsForVersion(maybeVersion).stream()
        .filter(p -> programRepository.getShallowProgramDefinition(p).adminName().equals(name))
        .findAny();
  }

  public boolean anyDisabledPrograms() {
    return anyDisabledPrograms(Optional.of(getActiveVersion()))
        || anyDisabledPrograms(getDraftVersion());
  }

  private boolean anyDisabledPrograms(Optional<VersionModel> maybeVersion) {
    return getProgramsForVersion(maybeVersion).stream()
        .anyMatch(
            p ->
                programRepository.getShallowProgramDefinition(p).displayMode()
                    == DisplayMode.DISABLED);
  }

  /** Returns the names of all the programs. */
  public ImmutableSet<String> getProgramNamesForVersion(VersionModel version) {
    return getProgramsForVersion(version).stream()
        .map(p -> programRepository.getShallowProgramDefinition(p))
        .map(ProgramDefinition::adminName)
        .collect(ImmutableSet.toImmutableSet());
  }

  /**
   * Returns the programs for a version.
   *
   * <p>If the cache is enabled, we will get the data from the cache and set it if it is not
   * present.
   */
  public ImmutableList<ProgramModel> getProgramsForVersion(VersionModel version) {
    // Only set the version cache for active and obsolete versions
    if (settingsManifest.getVersionCacheEnabled() && version.id <= getActiveVersion().id) {
      return programsByVersionCache.getOrElseUpdate(
          String.valueOf(version.id), () -> version.getPrograms());
    }
    return getProgramsForVersionWithoutCache(version);
  }

  /** Returns the programs for a version if the version is present. */
  public ImmutableList<ProgramModel> getProgramsForVersion(Optional<VersionModel> version) {
    return version.isPresent() ? getProgramsForVersion(version.get()) : ImmutableList.of();
  }

  /** Returns the programs for a version without using the cache. */
  public ImmutableList<ProgramModel> getProgramsForVersionWithoutCache(VersionModel version) {
    return version.getPrograms();
  }

  /**
   * Given any revision of a question, return the most recent conceptual version of it. Will return
   * the current DRAFT version if present then the current ACTIVE version.
   */
  public Optional<QuestionModel> getLatestVersionOfQuestion(long questionId) {
    String questionName =
        database
            .find(QuestionModel.class)
            .setId(questionId)
            .select("name")
            .setLabel("QuestionModel.findLatest")
            .setProfileLocation(profileLocationBuilder.create("getLatestVersionOfQuestion"))
            .findSingleAttribute();
    Optional<QuestionModel> draftQuestion =
        getQuestionsForVersion(getDraftVersion()).stream()
            .filter(
                question ->
                    questionRepository
                        .getQuestionDefinition(question)
                        .getName()
                        .equals(questionName))
            .findFirst();
    if (draftQuestion.isPresent()) {
      return draftQuestion;
    }
    return getQuestionsForVersion(getActiveVersion()).stream()
        .filter(
            question ->
                questionRepository.getQuestionDefinition(question).getName().equals(questionName))
        .findFirst();
  }

  /**
   * For each question in this program, check whether it is the most up-to-date version of the
   * question, which is either DRAFT or ACTIVE. If it is not, update the pointer to the most
   * up-to-date version of the question, using the given transaction. This method can only be called
   * on a draft program.
   */
  public void updateQuestionVersions(ProgramModel draftProgram) {
    Preconditions.checkArgument(isInactive(draftProgram), "input program must not be active.");
    Preconditions.checkArgument(
        isDraft(draftProgram), "input program must be in the current draft version.");
    ProgramDefinition.Builder updatedDefinition =
        programRepository.getShallowProgramDefinition(draftProgram).toBuilder()
            .setBlockDefinitions(ImmutableList.of());
    for (BlockDefinition block :
        programRepository.getShallowProgramDefinition(draftProgram).blockDefinitions()) {
      logger.trace("Updating screen (block) {}.", block.id());
      updatedDefinition.addBlockDefinition(updateQuestionVersions(draftProgram.id, block));
    }
    draftProgram = new ProgramModel(updatedDefinition.build());
    logger.trace("Submitting update.");
    database.update(draftProgram);
    draftProgram.refresh();
  }

  public boolean isInactive(QuestionModel question) {
    return !getQuestionsForVersion(getActiveVersion()).stream()
        .anyMatch(activeQuestion -> activeQuestion.id.equals(question.id));
  }

  public boolean isInactive(ProgramModel program) {
    return !getProgramsForVersion(getActiveVersion()).stream()
        .anyMatch(activeProgram -> activeProgram.id.equals(program.id));
  }

  public boolean isDraft(QuestionModel question) {
    return getQuestionsForVersion(getDraftVersion()).stream()
        .anyMatch(draftQuestion -> draftQuestion.id.equals(question.id));
  }

  /** Returns true if the program is a member of the current draft version. */
  public boolean isDraft(ProgramModel program) {
    return isDraftProgram(program.id);
  }

  /** Returns true if the program with the provided id is a member of the current draft version. */
  public boolean isDraftProgram(Long programId) {
    return getProgramsForVersion(getDraftVersion()).stream()
        .anyMatch(draftProgram -> draftProgram.id.equals(programId));
  }

  /** Returns true if the program with the provided id is a member of the current draft version. */
  public CompletionStage<Boolean> isDraftProgramAsync(Long programId) {
    return CompletableFuture.supplyAsync(() -> isDraftProgram(programId), databaseExecutionContext);
  }

  /** Returns true if the program with the provided id is a member of the current active version. */
  public boolean isActiveProgram(Long programId) {
    return getProgramsForVersion(getActiveVersion()).stream()
        .anyMatch(activeProgram -> activeProgram.id.equals(programId));
  }

  /** Validate all programs have associated questions. */
  private void validateProgramQuestionState() {
    VersionModel activeVersion = getActiveVersion();
    ImmutableList<QuestionDefinition> newActiveQuestions =
        getQuestionsForVersionWithoutCache(activeVersion).stream()
            .map(question -> questionRepository.getQuestionDefinition(question))
            .collect(ImmutableList.toImmutableList());
    // Check there aren't any duplicate questions in the new active version
    validateNoDuplicateQuestions(newActiveQuestions);
    ImmutableSet<Long> missingQuestionIds =
        getProgramsForVersionWithoutCache(activeVersion).stream()
            .map(
                program ->
                    programRepository
                        .getShallowProgramDefinition(program)
                        .getQuestionIdsInProgram())
            .flatMap(Collection::stream)
            .filter(
                questionId ->
                    !newActiveQuestions.stream()
                        .map(questionDefinition -> questionDefinition.getId())
                        .collect(ImmutableSet.toImmutableSet())
                        .contains(questionId))
            .collect(ImmutableSet.toImmutableSet());
    if (!missingQuestionIds.isEmpty()) {
      ImmutableSet<Long> programIdsMissingQuestions =
          getProgramsForVersionWithoutCache(activeVersion).stream()
              .filter(
                  program ->
                      programRepository
                          .getShallowProgramDefinition(program)
                          .getQuestionIdsInProgram()
                          .stream()
                          .anyMatch(id -> missingQuestionIds.contains(id)))
              .map(program -> program.id)
              .collect(ImmutableSet.toImmutableSet());
      throw new IllegalStateException(
          String.format(
              "Illegal state encountered when attempting to publish a new version. Question IDs"
                  + " %s found in program definitions %s not found in new active version.",
              missingQuestionIds, programIdsMissingQuestions));
    }
  }

  /** Validate there are no duplicate question names. */
  @VisibleForTesting
  void validateNoDuplicateQuestions(ImmutableList<QuestionDefinition> questionList) {
    Set<String> uniqueActiveQuestionNames = new HashSet<>();
    for (QuestionDefinition question : questionList) {
      if (!uniqueActiveQuestionNames.add(question.getName())) {
        throw new IllegalStateException(
            String.format(
                "Illegal state encountered when attempting to publish a new version. Question"
                    + " %s found more than once in the new active version.",
                question.getName()));
      }
    }
  }

  private BlockDefinition updateQuestionVersions(long programDefinitionId, BlockDefinition block) {
    BlockDefinition.Builder updatedBlock =
        block.toBuilder().setProgramQuestionDefinitions(ImmutableList.of());
    // Update questions contained in this block.
    for (ProgramQuestionDefinition question : block.programQuestionDefinitions()) {
      Optional<QuestionModel> updatedQuestion = getLatestVersionOfQuestion(question.id());
      logger.trace(
          "Updating question ID {} to new ID {}.", question.id(), updatedQuestion.orElseThrow().id);
      updatedBlock.addQuestion(
          question.loadCompletely(
              programDefinitionId,
              questionRepository.getQuestionDefinition(updatedQuestion.orElseThrow())));
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
        Optional<QuestionModel> updated = getLatestVersionOfQuestion(leaf.questionId());
        return PredicateExpressionNode.create(
            leaf.toBuilder().setQuestionId(updated.orElseThrow().id).build());
      case LEAF_ADDRESS_SERVICE_AREA:
        LeafAddressServiceAreaExpressionNode leafAddress = node.getLeafAddressNode();
        Optional<QuestionModel> updatedQuestion =
            getLatestVersionOfQuestion(leafAddress.questionId());
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
    getProgramsForVersion(getDraftVersion()).stream()
        .filter(
            program ->
                programRepository.getShallowProgramDefinition(program).hasQuestion(oldQuestionId))
        .forEach(this::updateQuestionVersions);

    // Update any ACTIVE program without a DRAFT that references the question, a new DRAFT is
    // created.
    getProgramsForVersion(getActiveVersion()).stream()
        .filter(
            program ->
                programRepository.getShallowProgramDefinition(program).hasQuestion(oldQuestionId))
        .filter(
            program ->
                getProgramByNameForVersion(
                        programRepository.getShallowProgramDefinition(program).adminName(),
                        getDraftVersion())
                    .isEmpty())
        .forEach(programRepository::createOrUpdateDraft);
  }

  /**
   * Inspects the provided version and returns a map where the key is the question name and the
   * value is a set of programs that reference the given question in this version.
   */
  public ImmutableMap<String, ImmutableSet<ProgramDefinition>> buildReferencingProgramsMap(
      VersionModel version) {
    ImmutableMap<Long, String> questionIdToNameLookup = getQuestionIdToNameMap(version);
    Map<String, Set<ProgramDefinition>> result = Maps.newHashMap();
    for (ProgramModel program : getProgramsForVersion(version)) {
      ImmutableSet<String> programQuestionNames =
          getProgramQuestionNames(
              programRepository.getShallowProgramDefinition(program), questionIdToNameLookup);
      for (String questionName : programQuestionNames) {
        if (!result.containsKey(questionName)) {
          result.put(questionName, Sets.newHashSet());
        }
        result.get(questionName).add(programRepository.getShallowProgramDefinition(program));
      }
    }
    return result.entrySet().stream()
        .collect(
            ImmutableMap.toImmutableMap(Entry::getKey, e -> ImmutableSet.copyOf(e.getValue())));
  }

  /** Returns the names of questions referenced by the program that are in the specified version. */
  public ImmutableSet<String> getProgramQuestionNamesInVersion(
      ProgramDefinition program, VersionModel version) {
    ImmutableMap<Long, String> questionIdToNameLookup = getQuestionIdToNameMap(version);
    return getProgramQuestionNames(program, questionIdToNameLookup);
  }

  /**
   * Returns true if any questions in the provided set are referenced by multiple programs in the
   * specified version.
   */
  private boolean anyQuestionIsShared(VersionModel version, ImmutableSet<String> questions) {
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
  private ImmutableMap<Long, String> getQuestionIdToNameMap(VersionModel version) {
    return getQuestionsForVersion(version).stream()
        .map(q -> questionRepository.getQuestionDefinition(q))
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
    return program.getQuestionIdsInProgram().stream()
        .filter(questionIdToNameLookup::containsKey)
        .map(questionIdToNameLookup::get)
        .collect(ImmutableSet.toImmutableSet());
  }
}
