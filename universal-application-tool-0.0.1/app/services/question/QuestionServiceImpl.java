package services.question;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import models.Question;
import repository.QuestionRepository;

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
  public Optional<QuestionDefinition> create(QuestionDefinition definition) {
    if (!isValid(definition)) {
      return Optional.empty();
    }
    Question question = questionRepository.insertQuestionSync(new Question(definition));
    return Optional.of(question.getQuestionDefinition());
  }

  @Override
  public CompletionStage<ReadOnlyQuestionService> getReadOnlyQuestionService() {
    return listQuestionDefinitionsAsync()
        .thenApply(questionDefinitions -> new ReadOnlyQuestionServiceImpl(questionDefinitions));
  }

  @Override
  public QuestionDefinition update(QuestionDefinition definition) {
    throw new java.lang.UnsupportedOperationException("Not supported yet.");
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

  private boolean isValid(QuestionDefinition definition) {
    String newPath = definition.getPath();
    if (!isValidPathPattern(newPath)) {
      return false;
    }
    boolean hasConflict =
        questionRepository.lookupPathConflict(newPath).toCompletableFuture().join().booleanValue();
    return !hasConflict;
  }

  private boolean isValidPathPattern(String path) {
    return URLEncoder.encode(path, StandardCharsets.UTF_8).equals(path);
  }
}
