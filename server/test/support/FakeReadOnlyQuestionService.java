package support;

import com.google.common.collect.ImmutableList;
import services.question.ActiveAndDraftQuestions;
import services.question.ReadOnlyQuestionService;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;

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
    return null;
  }

  @Override
  public ImmutableList<EnumeratorQuestionDefinition> getUpToDateEnumeratorQuestions() {
    return null;
  }

  @Override
  public ActiveAndDraftQuestions getActiveAndDraftQuestions() {
    return null;
  }

  @Override
  public QuestionDefinition getQuestionDefinition(long id) throws QuestionNotFoundException {
    return questions.stream()
        .filter(qd -> qd.getId() == id)
        .findAny()
        .orElseThrow(() -> new QuestionNotFoundException(id));
  }
}
