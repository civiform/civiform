package services.question;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import models.Question;
import models.QuestionTag;
import models.Version;
import repository.QuestionRepository;
import repository.VersionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.export.ExporterService;
import services.question.exceptions.InvalidUpdateException;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionDefinition;

/** Implementation class for {@link QuestionService} interface. */
public final class QuestionServiceImpl implements QuestionService {

  private final QuestionRepository questionRepository;
  private final Provider<VersionRepository> versionRepositoryProvider;

  @Inject
  public QuestionServiceImpl(
      QuestionRepository questionRepository,
      Provider<VersionRepository> versionRepositoryProvider) {
    this.questionRepository = checkNotNull(questionRepository);
    this.versionRepositoryProvider = checkNotNull(versionRepositoryProvider);
  }

  @Override
  public ErrorAnd<QuestionDefinition, CiviFormError> create(QuestionDefinition questionDefinition) {
    ImmutableSet<CiviFormError> validationErrors = questionDefinition.validate();
    ImmutableSet<CiviFormError> conflictErrors = checkConflicts(questionDefinition);
    ImmutableSet<CiviFormError> errors =
        ImmutableSet.<CiviFormError>builder()
            .addAll(validationErrors)
            .addAll(conflictErrors)
            .build();
    if (!errors.isEmpty()) {
      return ErrorAnd.error(errors);
    }
    Question question = new Question(questionDefinition);
    question.addVersion(versionRepositoryProvider.get().getDraftVersion());
    questionRepository.insertQuestionSync(question);
    return ErrorAnd.of(question.getQuestionDefinition());
  }

  @Override
  public CompletionStage<ReadOnlyQuestionService> getReadOnlyQuestionService() {
    return CompletableFuture.completedStage(
        new ReadOnlyCurrentQuestionServiceImpl(
            versionRepositoryProvider.get().getActiveVersion(),
            versionRepositoryProvider.get().getDraftVersion()));
  }

  @Override
  public ReadOnlyQuestionService getReadOnlyVersionedQuestionService(Version version) {
    return new ReadOnlyVersionedQuestionServiceImpl(version);
  }

  @Override
  public ErrorAnd<QuestionDefinition, CiviFormError> update(QuestionDefinition questionDefinition)
      throws InvalidUpdateException {
    if (!questionDefinition.isPersisted()) {
      throw new InvalidUpdateException("question definition is not persisted");
    }
    ImmutableSet<CiviFormError> validationErrors = questionDefinition.validate();

    Optional<Question> maybeQuestion =
        questionRepository.lookupQuestion(questionDefinition.getId()).toCompletableFuture().join();
    if (maybeQuestion.isEmpty()) {
      throw new InvalidUpdateException(
          String.format("question with id %d does not exist", questionDefinition.getId()));
    }
    Question question = maybeQuestion.get();
    ImmutableSet<CiviFormError> immutableMemberErrors =
        validateQuestionImmutableMembers(question.getQuestionDefinition(), questionDefinition);

    ImmutableSet<CiviFormError> errors =
        ImmutableSet.<CiviFormError>builder()
            .addAll(validationErrors)
            .addAll(immutableMemberErrors)
            .build();
    if (!errors.isEmpty()) {
      return ErrorAnd.error(errors);
    }

    question = questionRepository.updateOrCreateDraft(questionDefinition);
    return ErrorAnd.of(question.getQuestionDefinition());
  }

  @Override
  public void restoreQuestion(Long id) throws InvalidUpdateException {
    Optional<Question> question =
        questionRepository.lookupQuestion(id).toCompletableFuture().join();
    if (question.isEmpty()) {
      throw new InvalidUpdateException("Did not find question.");
    }
    Version draftVersion = versionRepositoryProvider.get().getDraftVersion();
    if (!draftVersion.removeTombstoneForQuestion(question.get())) {
      throw new InvalidUpdateException("Not tombstoned.");
    }
    draftVersion.save();
  }

  @Override
  public void archiveQuestion(Long id) throws InvalidUpdateException {
    Optional<Question> question =
        questionRepository.lookupQuestion(id).toCompletableFuture().join();
    if (question.isEmpty()) {
      throw new InvalidUpdateException("Did not find question.");
    }
    Version draftVersion = versionRepositoryProvider.get().getDraftVersion();
    if (!draftVersion.addTombstoneForQuestion(question.get())) {
      throw new InvalidUpdateException("Already tombstoned.");
    }
    draftVersion.save();
  }

  @Override
  public void discardDraft(Long draftId) throws InvalidUpdateException {
    Question question =
        questionRepository
            .lookupQuestion(draftId)
            .toCompletableFuture()
            .join()
            .orElseThrow(() -> new InvalidUpdateException("Did not find question."));

    // Find the Active version.
    Version activeVersion = versionRepositoryProvider.get().getActiveVersion();
    Long activeId =
        activeVersion
            .getQuestionByName(question.getQuestionDefinition().getName())
            // TODO: If nothing depends on this question then it could be removed.
            .orElseThrow(
                () ->
                    new InvalidUpdateException(
                        "Deleting the first version of a question is not supported."))
            .id;
    Preconditions.checkArgument(
        !draftId.equals(activeId),
        "Draft and Active IDs are the same ({}) for Question {}, this should not be possible.",
        draftId,
        question.getQuestionDefinition().getName());

    Version draftVersion = versionRepositoryProvider.get().getDraftVersion();
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

  @Override
  public ImmutableList<QuestionDefinition> getQuestionsForTag(QuestionTag tag) {
    return questionRepository.getAllQuestionsForTag(tag);
  }

  @Override
  public void setExportState(QuestionDefinition questionDefinition, QuestionTag questionExportState)
      throws QuestionNotFoundException, InvalidUpdateException {
    Optional<Question> questionMaybe =
        questionRepository.lookupQuestion(questionDefinition.getId()).toCompletableFuture().join();
    if (questionMaybe.isEmpty()) {
      throw new QuestionNotFoundException(questionDefinition.getId());
    }
    Question question = questionMaybe.get();
    if (ExporterService.NON_EXPORTED_QUESTION_TYPES.contains(
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
    Optional<Question> maybeConflict =
        questionRepository.findConflictingQuestion(questionDefinition);
    if (maybeConflict.isPresent()) {
      Question conflict = maybeConflict.get();
      String errorMessage;
      if (questionDefinition.getEnumeratorId().isEmpty()) {
        errorMessage =
            String.format(
                "Question '%s' conflicts with question id: %s",
                questionDefinition.getQuestionPathSegment(), conflict.id);
      } else {
        errorMessage =
            String.format(
                "Question '%s' with Enumerator ID %d conflicts with question id: %d",
                questionDefinition.getQuestionPathSegment(),
                questionDefinition.getEnumeratorId().get(),
                conflict.id);
      }
      return ImmutableSet.of(CiviFormError.of(errorMessage));
    }
    return ImmutableSet.of();
  }

  /**
   * Validates that a question's updates do not change its immutable members.
   *
   * <p>Question immutable members are: name, enumerator id, path, and type.
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

    if (!questionDefinition.getQuestionType().equals(toUpdate.getQuestionType())) {
      errors.add(
          CiviFormError.of(
              String.format(
                  "question types mismatch: %s does not match %s",
                  questionDefinition.getQuestionType(), toUpdate.getQuestionType())));
    }
    return errors.build();
  }
}
