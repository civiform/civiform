package services.question;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import models.QuestionModel;
import models.QuestionTag;
import models.VersionModel;
import repository.QuestionRepository;
import repository.TransactionManager;
import repository.VersionRepository;
import services.CiviFormError;
import services.DeletionStatus;
import services.ErrorAnd;
import services.Path;
import services.export.CsvExporterService;
import services.question.exceptions.InvalidUpdateException;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionDefinition;

/**
 * The service responsible for accessing the Question resource. Admins create {@link
 * QuestionDefinition}s which are consumed by {@link services.program.ProgramService} to define
 * program-specific applications and {@link services.applicant.ApplicantService} for storing
 * applicants' answers to questions. The full set of questions at a given version defines the data
 * that can be collected for a given applicant across all programs.
 */
public final class QuestionService {

  private final QuestionRepository questionRepository;
  private final Provider<VersionRepository> versionRepositoryProvider;

  @Inject
  public QuestionService(
      QuestionRepository questionRepository,
      Provider<VersionRepository> versionRepositoryProvider) {
    this.questionRepository = checkNotNull(questionRepository);
    this.versionRepositoryProvider = checkNotNull(versionRepositoryProvider);
  }

  /**
   * Creates a new Question Definition. Returns a QuestionDefinition object on success and {@link
   * Optional#empty} on failure.
   *
   * <p>This will fail if the path provided already resolves to a QuestionDefinition or Scalar.
   *
   * <p>NOTE: This does not update the version.
   */
  public ErrorAnd<QuestionDefinition, CiviFormError> create(QuestionDefinition questionDefinition) {
    ImmutableSet<CiviFormError> validationErrors = questionDefinition.validate();

    return TransactionManager.executeInTransaction(
        () -> {
          ImmutableSet<CiviFormError> conflictErrors = checkConflicts(questionDefinition);
          ImmutableSet<CiviFormError> errors =
              ImmutableSet.<CiviFormError>builder()
                  .addAll(validationErrors)
                  .addAll(conflictErrors)
                  .build();
          if (!errors.isEmpty()) {
            return ErrorAnd.error(errors);
          }      // atlee allow me to force a race condition
          if (questionDefinition.getName().contains("Pause")) {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(20));
          }
          QuestionModel question = new QuestionModel(questionDefinition);
          question.addVersion(versionRepositoryProvider.get().getDraftVersionOrCreate());
          questionRepository.insertQuestionSync(question);
          return ErrorAnd.of(question.getQuestionDefinition());
        },
        () ->
            ErrorAnd.error(
                ImmutableSet.of(
                    CiviFormError.of(
                        "A question with a conflicting admin name was created at the same time."
                            + " Please try again."))));
  }

  /**
   * Retrieves the latest question definition for all questions identified by the given set of
   * question names. If a question with the given name does not exist, it is not a key in the
   * resulting map.
   */
  public ImmutableMap<String, QuestionDefinition> getExistingQuestions(
      ImmutableSet<String> questionNames) {
    return questionRepository.getExistingQuestions(questionNames);
  }

  /**
   * Get a {@link ReadOnlyQuestionService} which implements synchronous, in-memory read behavior for
   * questions in current active and draft versions.
   */
  public ReadOnlyQuestionService getReadOnlyQuestionServiceSync() {
    return readOnlyQuestionService();
  }

  /**
   * Get a {@link ReadOnlyQuestionService} which implements synchronous, in-memory read behavior for
   * questions in current active and draft versions.
   */
  public CompletionStage<ReadOnlyQuestionService> getReadOnlyQuestionService() {
    return CompletableFuture.completedStage(readOnlyQuestionService());
  }

  private ReadOnlyQuestionService readOnlyQuestionService() {
    return new ReadOnlyCurrentQuestionServiceImpl(versionRepositoryProvider.get());
  }

  /**
   * Get a {@link ReadOnlyQuestionService} which implements synchronous, in-memory read behavior for
   * questions in a particular version.
   */
  public ReadOnlyQuestionService getReadOnlyVersionedQuestionService(
      VersionModel version, VersionRepository versionRepository) {
    return new ReadOnlyVersionedQuestionServiceImpl(version, versionRepository);
  }

  /**
   * Overload of {@code update()} that defaults {@code previousDefinition} to empty.
   *
   * <p>See {@link #update(Optional, QuestionDefinition)} for the implications of not providing a
   * {@code previousDefinition} when updating.
   *
   * @param updatedDefinition the {@link QuestionDefinition} to update
   * @return the updated {@link QuestionDefinition} or a {@link CiviFormError}
   */
  public ErrorAnd<QuestionDefinition, CiviFormError> update(QuestionDefinition updatedDefinition)
      throws InvalidUpdateException {
    return update(Optional.empty(), updatedDefinition);
  }

  /**
   * Destructive overwrite of a question at a given path.
   *
   * <p>The {@code previousDefinition} is used during validation. See {@link
   * QuestionDefinition#validate(Optional)} for the implications of not including a {@code
   * previousDefinition} when validating.
   *
   * <p>The write will fail if:
   * <li>The QuestionDefinition is not persisted yet.
   * <li>The path is different from the original path.
   * <li>NOTE: This does not update the version.
   *
   * @param previousDefinition the previous version of the {@link QuestionDefinition}, defaults to
   *     empty.
   * @param updatedDefinition the {@link QuestionDefinition} to update
   * @return the updated {@link QuestionDefinition} or a {@link CiviFormError}
   * @throws InvalidUpdateException if a question with the provided ID does not already exist
   */
  public ErrorAnd<QuestionDefinition, CiviFormError> update(
      Optional<QuestionDefinition> previousDefinition, QuestionDefinition updatedDefinition)
      throws InvalidUpdateException {
    if (!updatedDefinition.isPersisted()) {
      throw new InvalidUpdateException("question definition is not persisted");
    }
    ImmutableSet<CiviFormError> validationErrors = updatedDefinition.validate(previousDefinition);

    Optional<QuestionModel> maybeQuestion =
        questionRepository.lookupQuestion(updatedDefinition.getId()).toCompletableFuture().join();
    if (maybeQuestion.isEmpty()) {
      throw new InvalidUpdateException(
          String.format("question with id %d does not exist", updatedDefinition.getId()));
    }
    QuestionModel question = maybeQuestion.get();
    ImmutableSet<CiviFormError> immutableMemberErrors =
        validateQuestionImmutableMembers(
            questionRepository.getQuestionDefinition(question), updatedDefinition);

    ImmutableSet<CiviFormError> errors =
        ImmutableSet.<CiviFormError>builder()
            .addAll(validationErrors)
            .addAll(immutableMemberErrors)
            .build();
    if (!errors.isEmpty()) {
      return ErrorAnd.error(errors);
    }

    question = questionRepository.createOrUpdateDraft(updatedDefinition);
    return ErrorAnd.of(questionRepository.getQuestionDefinition(question));
  }

  /** If this question is archived but a new version has not been published yet, un-archive it. */
  public void restoreQuestion(Long id) throws InvalidUpdateException {
    Optional<QuestionModel> question =
        questionRepository.lookupQuestion(id).toCompletableFuture().join();
    if (question.isEmpty()) {
      throw new InvalidUpdateException("Did not find question.");
    }
    ActiveAndDraftQuestions activeQuestions =
        readOnlyQuestionService().getActiveAndDraftQuestions();
    if (!activeQuestions
        .getDeletionStatus(questionRepository.getQuestionDefinition(question.get()).getName())
        .equals(DeletionStatus.PENDING_DELETION)) {
      throw new InvalidUpdateException("Question is not restorable.");
    }
    VersionModel draftVersion = versionRepositoryProvider.get().getDraftVersionOrCreate();
    if (!draftVersion.removeTombstoneForQuestion(question.get())) {
      throw new InvalidUpdateException("Not tombstoned.");
    }
    draftVersion.save();
  }

  /** If this question is not used in any program, archive it. */
  public void archiveQuestion(Long id) throws InvalidUpdateException {
    Optional<QuestionModel> question =
        questionRepository.lookupQuestion(id).toCompletableFuture().join();
    if (question.isEmpty()) {
      throw new InvalidUpdateException("Did not find question.");
    }
    ActiveAndDraftQuestions activeQuestions =
        readOnlyQuestionService().getActiveAndDraftQuestions();
    if (!activeQuestions
        .getDeletionStatus(questionRepository.getQuestionDefinition(question.get()).getName())
        .equals(DeletionStatus.DELETABLE)) {
      throw new InvalidUpdateException("Question is not archivable.");
    }

    QuestionModel draftQuestion =
        questionRepository.createOrUpdateDraft(
            questionRepository.getQuestionDefinition(question.get()));
    VersionModel draftVersion = versionRepositoryProvider.get().getDraftVersionOrCreate();
    try {
      if (!versionRepositoryProvider
          .get()
          .addTombstoneForQuestionInVersion(draftQuestion, draftVersion)) {
        throw new InvalidUpdateException("Already tombstoned.");
      }
    } catch (QuestionNotFoundException e) {
      // Shouldn't happen because we call createOrUpdateDraft before archiving.
      throw new RuntimeException(e);
    }
    draftVersion.save();
  }

  /** If this is a draft question, remove it from the draft version and update all programs. */
  public void discardDraft(Long draftId) throws InvalidUpdateException {
    QuestionModel question =
        questionRepository
            .lookupQuestion(draftId)
            .toCompletableFuture()
            .join()
            .orElseThrow(() -> new InvalidUpdateException("Did not find question."));

    // Find the Active version.
    VersionModel activeVersion = versionRepositoryProvider.get().getActiveVersion();
    Long activeId =
        versionRepositoryProvider
            .get()
            .getQuestionByNameForVersion(
                questionRepository.getQuestionDefinition(question).getName(), activeVersion)
            // TODO: If nothing depends on this question then it could be removed.
            .orElseThrow(
                () ->
                    new InvalidUpdateException(
                        "Deleting the first version of a question is not supported."))
            .id;
    Preconditions.checkArgument(
        !draftId.equals(activeId),
        "Draft and Active IDs are the same (%s) for Question %s, this should not be possible.",
        draftId,
        questionRepository.getQuestionDefinition(question).getName());

    VersionModel draftVersion = versionRepositoryProvider.get().getDraftVersionOrCreate();
    if (!question.removeVersion(draftVersion)) {
      throw new InvalidUpdateException("Did not find question in draft version.");
    }
    question.save();

    // Note: The above section removed the question from the draft version and saved it, so that the
    // enmasse program update below sees the relevant latest version of the question.  However if
    // there's an error below, Those pertinent configurations are left invalid which will break the
    // site.
    // TODO(#2047): Address errors that occur after this point so that program/question state isn't
    // left invalid.

    // Update any repeated questions that may have referenced the discarded question.
    questionRepository.updateAllRepeatedQuestions(
        /* newEnumeratorId= */ activeId, /* oldEnumeratorId= */ draftId);

    // Update any programs that reference the discarded question to the latest revision for all of
    // its referenced questions.
    versionRepositoryProvider.get().updateProgramsThatReferenceQuestion(draftId);
  }

  /** Return all active questions which have the given tag. */
  public ImmutableList<QuestionDefinition> getQuestionsForTag(QuestionTag tag) {
    return questionRepository.getAllQuestionsForTag(tag);
  }

  /** Set the export state of the question provided. */
  public void setExportState(QuestionDefinition questionDefinition, QuestionTag questionExportState)
      throws QuestionNotFoundException, InvalidUpdateException {
    Optional<QuestionModel> questionMaybe =
        questionRepository.lookupQuestion(questionDefinition.getId()).toCompletableFuture().join();
    if (questionMaybe.isEmpty()) {
      throw new QuestionNotFoundException(questionDefinition.getId());
    }
    QuestionModel question = questionMaybe.get();
    if (CsvExporterService.NON_EXPORTED_QUESTION_TYPES.contains(
        questionDefinition.getQuestionType())) {
      question.removeTag(QuestionTag.DEMOGRAPHIC_PII);
      question.removeTag(QuestionTag.NON_DEMOGRAPHIC);
      question.removeTag(QuestionTag.DEMOGRAPHIC);
    }

    switch (questionExportState) {
      case DEMOGRAPHIC:
        question.removeTag(QuestionTag.DEMOGRAPHIC_PII);
        question.removeTag(QuestionTag.NON_DEMOGRAPHIC);
        question.addTag(QuestionTag.DEMOGRAPHIC);
        break;
      case DEMOGRAPHIC_PII:
        question.removeTag(QuestionTag.DEMOGRAPHIC);
        question.removeTag(QuestionTag.NON_DEMOGRAPHIC);
        question.addTag(QuestionTag.DEMOGRAPHIC_PII);
        break;
      case NON_DEMOGRAPHIC:
        question.removeTag(QuestionTag.DEMOGRAPHIC_PII);
        question.removeTag(QuestionTag.DEMOGRAPHIC);
        question.addTag(QuestionTag.NON_DEMOGRAPHIC);
        break;
      default:
        throw new InvalidUpdateException(
            String.format("Unknown question export state: %s", questionExportState));
    }
    question.save();
  }

  /**
   * Check for conflicts with other questions. This is to be only used with new questions because
   * questions being updated will likely conflict with themselves, and new versions of previous
   * questions will conflict with their previous versions.
   *
   * <p>Questions conflict if they have the same enumerator id reference and the same question path
   * segment.
   */
  private ImmutableSet<CiviFormError> checkConflicts(QuestionDefinition questionDefinition) {
    Optional<QuestionModel> maybeConflict =
        questionRepository.findConflictingQuestion(questionDefinition);
    if (maybeConflict.isPresent()) {
      QuestionModel conflict = maybeConflict.get();
      String errorMessage;
      errorMessage =
          String.format(
              "Administrative identifier '%s' generates JSON path '%s' which would conflict with"
                  + " the existing question with admin ID '%s'",
              questionDefinition.getName(),
              Path.create(questionDefinition.getQuestionNameKey()).toString(),
              questionRepository.getQuestionDefinition(conflict).getName());
      return ImmutableSet.of(CiviFormError.of(errorMessage));
    }
    return ImmutableSet.of();
  }

  /**
   * Validates that a question's updates do not change its immutable members.
   *
   * <p>Question immutable members are: name, enumerator id, and path.
   */
  private ImmutableSet<CiviFormError> validateQuestionImmutableMembers(
      QuestionDefinition questionDefinition, QuestionDefinition toUpdate) {
    ImmutableSet.Builder<CiviFormError> errors = new ImmutableSet.Builder<>();

    if (!questionDefinition.getName().equals(toUpdate.getName())) {
      errors.add(
          CiviFormError.of(
              String.format(
                  "question names mismatch: %s does not match %s",
                  questionDefinition.getName(), toUpdate.getName())));
    }

    if (!questionDefinition.getEnumeratorId().equals(toUpdate.getEnumeratorId())) {
      errors.add(
          CiviFormError.of(
              String.format(
                  "question enumerator ids mismatch: %s does not match %s",
                  questionDefinition
                      .getEnumeratorId()
                      .map(String::valueOf)
                      .orElse("[no enumerator]"),
                  toUpdate.getEnumeratorId().map(String::valueOf).orElse("[no enumerator]"))));
    }

    if (!questionDefinition.getQuestionPathSegment().equals(toUpdate.getQuestionPathSegment())) {
      errors.add(
          CiviFormError.of(
              String.format(
                  "question path segment mismatch: %s does not match %s",
                  questionDefinition.getQuestionPathSegment(), toUpdate.getQuestionPathSegment())));
    }

    return errors.build();
  }

  /**
   * Returns all questions for the specified version. For example passing in version of id 2, would
   * return questions for version of id 1. Will return a list for draft, active, or obsolete
   * versions.
   *
   * <p>If there is no previous version an empty list is returned.
   *
   * @param version The version used to lookup the previous version
   * @return Populated list of Question Definitions or an empty list
   */
  public ImmutableList<QuestionDefinition> getAllPreviousVersionQuestions(VersionModel version) {
    Optional<VersionModel> optionalPreviousVersion =
        versionRepositoryProvider.get().getPreviousVersion(version);

    // This should only happen if we only have one version in the system
    // such as in a fresh install
    if (optionalPreviousVersion.isEmpty()) {
      return ImmutableList.of();
    }

    return getReadOnlyVersionedQuestionService(
            optionalPreviousVersion.get(), versionRepositoryProvider.get())
        .getAllQuestions();
  }
}
