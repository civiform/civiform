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
import services.ErrorAnd;

public final class QuestionServiceImpl implements QuestionService {

  private QuestionRepository questionRepository;

  @Inject
  public QuestionServiceImpl(QuestionRepository questionRepository) {
    this.questionRepository = checkNotNull(questionRepository);
  }

  @Override
  public boolean addTranslation(
      String path, Locale locale, String questionText, Optional<String> questionHelpText)
      throws InvalidPathException {
    throw new java.lang.UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public ErrorAnd<QuestionDefinition, QuestionServiceError> create(QuestionDefinition definition) {
    ImmutableSet<QuestionServiceError> errors = validate(definition);
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
  public ErrorAnd<QuestionDefinition, QuestionServiceError> update(QuestionDefinition definition)
      throws InvalidUpdateException {
    if (!definition.isPersisted()) {
      throw new InvalidUpdateException("question definition is not persisted");
    }
    Optional<Question> maybeQuestion =
        questionRepository.lookupQuestion(definition.getId()).toCompletableFuture().join();
    if (!maybeQuestion.isPresent()) {
      throw new InvalidUpdateException(
          String.format("question with id %d does not exist", definition.getId()));
    }
    Question question = maybeQuestion.get();
    ImmutableSet<QuestionServiceError> errors =
        validateQuestionInvariants(question.getQuestionDefinition(), definition);
    if (!errors.isEmpty()) {
      return ErrorAnd.error(errors);
    }
    question = questionRepository.updateQuestionSync(new Question(definition));
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

  private ImmutableSet<QuestionServiceError> validate(QuestionDefinition newDefinition) {
    ImmutableSet<QuestionServiceError> errors = newDefinition.validate();
    if (!errors.isEmpty()) {
      return errors;
    }
    String newPath = newDefinition.getPath();
    Optional<Question> maybeConflict =
        questionRepository.findConflictingQuestion(newPath).toCompletableFuture().join();
    if (maybeConflict.isPresent()) {
      Question question = maybeConflict.get();
      return ImmutableSet.of(
          QuestionServiceError.of(
              String.format("path '%s' conflicts with question: %s", newPath, question.getPath())));
    }
    return ImmutableSet.of();
  }

  private ImmutableSet<QuestionServiceError> validateQuestionInvariants(
      QuestionDefinition definition, QuestionDefinition toUpdate) {
    ImmutableSet.Builder<QuestionServiceError> errors =
        new ImmutableSet.Builder<QuestionServiceError>();
    if (!definition.getPath().equals(toUpdate.getPath())) {
      errors.add(
          QuestionServiceError.of(
              String.format(
                  "question paths mismatch: %s does not match %s",
                  definition.getPath(), toUpdate.getPath())));
    }
    if (!definition.getQuestionType().equals(toUpdate.getQuestionType())) {
      errors.add(
          QuestionServiceError.of(
              String.format(
                  "question types mismatch: %s does not match %s",
                  definition.getQuestionType().toString(), toUpdate.getQuestionType().toString())));
    }
    return errors.build();
  }
}
