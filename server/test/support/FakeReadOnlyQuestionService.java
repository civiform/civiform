package support;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.NotImplementedException;
import services.question.ActiveAndDraftQuestions;
import services.question.ReadOnlyQuestionService;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;

/** Wraps a list of {@link QuestionDefinition}s for testing purposes. */
public class FakeReadOnlyQuestionService implements ReadOnlyQuestionService {

  private final ImmutableList<QuestionDefinition> questions;

  public FakeReadOnlyQuestionService(ImmutableList<QuestionDefinition> questions) {
    this.questions = questions;
  }

  @Override
  public ImmutableList<QuestionDefinition> getAllQuestions() {
    return questions;
  }

  @Override
  public ImmutableList<QuestionDefinition> getUpToDateQuestions() {
    return questions;
  }

  @Override
  public ImmutableList<EnumeratorQuestionDefinition> getAllEnumeratorQuestions() {
    return questions.stream()
        .filter(QuestionDefinition::isEnumerator)
        .map(qd -> (EnumeratorQuestionDefinition) qd)
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public ImmutableList<EnumeratorQuestionDefinition> getUpToDateEnumeratorQuestions() {
    throw new NotImplementedException();
  }

  @Override
  public ActiveAndDraftQuestions getActiveAndDraftQuestions() {
    throw new NotImplementedException();
  }

  @Override
  public QuestionDefinition getQuestionDefinition(long id) throws QuestionNotFoundException {
    return questions.stream()
        .filter(qd -> qd.getId() == id)
        .findAny()
        .orElseThrow(() -> new QuestionNotFoundException(id));
  }
}
