package services.question;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
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

  public boolean addTranslation(
      String path, Locale Locale, String questionText, Optional<String> questionHelpText)
      throws InvalidPathException {
    throw new java.lang.UnsupportedOperationException("Not supported yet.");
  }

  public Optional<QuestionDefinition> create(QuestionDefinition definition) {
    // TODO(https://github.com/seattle-uat/universal-application-tool/issues/194): Add validation.
    Question question = questionRepository.insertQuestionSync(new Question(definition));
    return Optional.of(question.getQuestionDefinition());
  }

  public CompletionStage<ReadOnlyQuestionService> getReadOnlyQuestionService() {
    return listQuestionDefinitionsAsync()
        .thenApply(questionDefinitions -> new ReadOnlyQuestionServiceImpl(questionDefinitions));
  }

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
}
