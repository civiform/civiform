package services.question;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import models.Question;
import repository.QuestionRepository;
import services.CiviFormError;
import services.ErrorAnd;
import services.Path;

public final class QuestionServiceImpl implements QuestionService {

  private QuestionRepository questionRepository;

  @Inject
  public QuestionServiceImpl(QuestionRepository questionRepository) {
    this.questionRepository = checkNotNull(questionRepository);
  }

  @Override
  public boolean addTranslation(
      Path path, Locale locale, String questionText, Optional<String> questionHelpText)
      throws InvalidPathException {
    throw new java.lang.UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public ErrorAnd<QuestionDefinition, CiviFormError> create(QuestionDefinition definition) {
    ImmutableSet<CiviFormError> errors = validate(definition);
    if (!errors.isEmpty()) {
      return ErrorAnd.error(errors);
    }
    Question question = questionRepository.insertQuestionSync(new Question(definition));
    return ErrorAnd.of(question.getQuestionDefinition());
  }

  @Override
  public CompletionStage<ReadOnlyQuestionService> getReadOnlyQuestionService() {
    return listQuestionDefinitionsAsync()
        .thenApply(questionDefinitions -> new ReadOnlyQuestionServiceImpl(questionDefinitions));
  }

  @Override
  public ErrorAnd<QuestionDefinition, CiviFormError> update(QuestionDefinition definition)
      throws InvalidUpdateException {
    if (!definition.isPersisted()) {
      throw new InvalidUpdateException("question definition is not persisted");
    }

    ImmutableSet<CiviFormError> validateErrors = definition.validate();

    Optional<Question> maybeQuestion =
        questionRepository.lookupQuestion(definition.getId()).toCompletableFuture().join();
    if (!maybeQuestion.isPresent()) {
      throw new InvalidUpdateException(
          String.format("question with id %d does not exist", definition.getId()));
    }
    Question question = maybeQuestion.get();
    ImmutableSet<CiviFormError> invariantErrors =
        validateQuestionInvariants(question.getQuestionDefinition(), definition);

    ImmutableSet<CiviFormError> errors =
        ImmutableSet.<CiviFormError>builder()
            .addAll(validateErrors)
            .addAll(invariantErrors)
            .build();
    if (!errors.isEmpty()) {
      return ErrorAnd.error(errors);
    }

    // This switch directs the update to the right place.  If an update is sent to a draft,
    // it is applied directly.  To a deleted question, it is ignored.  To an active or
    // obsolete question, to the draft for that question.
    switch (question.getLifecycleStage()) {
      case DRAFT:
        question = questionRepository.updateQuestionSync(new Question(definition));
        break;
      case DELETED:
        return ErrorAnd.error(
            ImmutableSet.of(
                CiviFormError.of(String.format("Question %d was DELETED.", definition.getId()))));
      case ACTIVE:
        // fallthrough
      case OBSOLETE:
        question = questionRepository.updateOrCreateDraft(definition);
    }
    return ErrorAnd.of(question.getQuestionDefinition());
  }

  private CompletionStage<ImmutableList<QuestionDefinition>> listQuestionDefinitionsAsync() {
    return questionRepository
        .listQuestions()
        .thenApply(
            questions ->
                questions.stream()
                    .map(question -> question.getQuestionDefinition())
                    .collect(ImmutableList.toImmutableList()));
  }

  private ImmutableSet<CiviFormError> validate(QuestionDefinition newDefinition) {
    ImmutableSet<CiviFormError> errors = newDefinition.validate();
    if (!errors.isEmpty()) {
      return errors;
    }
    Path newPath = newDefinition.getPath();
    Optional<Question> maybeConflict =
        questionRepository.findConflictingQuestion(newPath).toCompletableFuture().join();
    if (maybeConflict.isPresent()) {
      Question question = maybeConflict.get();
      return ImmutableSet.of(
          CiviFormError.of(
              String.format(
                  "path '%s' conflicts with question: %s", newPath.path(), question.getPath())));
    }
    return ImmutableSet.of();
  }

  private ImmutableSet<CiviFormError> validateQuestionInvariants(
      QuestionDefinition definition, QuestionDefinition toUpdate) {
    ImmutableSet.Builder<CiviFormError> errors = new ImmutableSet.Builder<CiviFormError>();
    if (!definition.getPath().equals(toUpdate.getPath())) {
      errors.add(
          CiviFormError.of(
              String.format(
                  "question paths mismatch: %s does not match %s",
                  definition.getPath().path(), toUpdate.getPath().path())));
    }
    if (!definition.getQuestionType().equals(toUpdate.getQuestionType())) {
      errors.add(
          CiviFormError.of(
              String.format(
                  "question types mismatch: %s does not match %s",
                  definition.getQuestionType().toString(), toUpdate.getQuestionType().toString())));
    }
    return errors.build();
  }
}
