package services.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import models.VersionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repository.VersionRepository;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.NullQuestionDefinition;
import services.question.types.QuestionDefinition;

/**
 * Implementation class for {@link ReadOnlyQuestionService} interface. It contains all questions
 * that are in the specified version.
 *
 * <p>See {@link QuestionService#getReadOnlyVersionedQuestionService(VersionModel)}.
 */
public final class ReadOnlyVersionedQuestionServiceImpl implements ReadOnlyQuestionService {

  private final ImmutableMap<Long, QuestionDefinition> questionsById;
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ReadOnlyVersionedQuestionServiceImpl.class);

  public ReadOnlyVersionedQuestionServiceImpl(
      VersionModel version, VersionRepository versionRepository) {
    questionsById =
        versionRepository.getQuestionDefinitionsForVersion(version).stream()
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

    LOGGER.error("Question not found for ID: {}", id);
    return new NullQuestionDefinition(id);
  }
}
