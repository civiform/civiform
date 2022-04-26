package services.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import models.Question;
import models.Version;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;

/**
 * Implementation class for {@link ReadOnlyQuestionService} interface. It contains all questions
 * that are in the specified version.
 *
 * <p>See {@link QuestionService#getReadOnlyVersionedQuestionService(Version)}.
 */
public final class ReadOnlyVersionedQuestionServiceImpl implements ReadOnlyQuestionService {

  private final ImmutableMap<Long, QuestionDefinition> questionsById;

  public ReadOnlyVersionedQuestionServiceImpl(Version version) {
    questionsById =
        version.getQuestions().stream()
            .map(Question::getQuestionDefinition)
            .collect(ImmutableMap.toImmutableMap(QuestionDefinition::getId, qd -> qd));
  }

  @Override
  public ActiveAndDraftQuestions getActiveAndDraftQuestions() {
    throw new RuntimeException(
        "ReadOnlyVersionedQuestionServiceImpl does not support getActiveAndDraftQuestions.");
  }

  @Override
  public ImmutableList<QuestionDefinition> getAllQuestions() {
    return questionsById.values().asList();
  }

  @Override
  public ImmutableList<QuestionDefinition> getUpToDateQuestions() {
    throw new RuntimeException(
        "ReadOnlyVersionedQuestionServiceImpl does not support getUpToDateQuestions.");
  }

  @Override
  public ImmutableList<EnumeratorQuestionDefinition> getUpToDateEnumeratorQuestions() {
    throw new RuntimeException(
        "ReadOnlyVersionedQuestionServiceImpl does not support getUpToDateEnumeratorQuestions.");
  }

  @Override
  public ImmutableList<EnumeratorQuestionDefinition> getAllEnumeratorQuestions() {
    return getAllQuestions().stream()
        .filter(QuestionDefinition::isEnumerator)
        .map(questionDefinition -> (EnumeratorQuestionDefinition) questionDefinition)
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public QuestionDefinition getQuestionDefinition(long id) throws QuestionNotFoundException {
    if (questionsById.containsKey(id)) {
      return questionsById.get(id);
    }
    throw new QuestionNotFoundException(id);
  }
}
