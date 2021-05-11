package services.question;

import com.google.common.collect.ImmutableList;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.EnumeratorQuestionDefinition;
import services.question.types.QuestionDefinition;

/**
 * The ReadOnlyQuestionService contains all synchronous, in-memory operations for
 * QuestionDefinitions.
 */
public interface ReadOnlyQuestionService {

  /** Returns all question definitions. */
  ImmutableList<QuestionDefinition> getAllQuestions();

  /** Returns all up-to-date question definitions for this version. */
  ImmutableList<QuestionDefinition> getUpToDateQuestions();

  /** Returns all enumerator question definitions. */
  ImmutableList<EnumeratorQuestionDefinition> getAllEnumeratorQuestions();

  /** Returns all up-to-date enumerator question definitions. */
  ImmutableList<EnumeratorQuestionDefinition> getUpToDateEnumeratorQuestions();

  /** Get the data object about the questions that are in the active or draft version. */
  ActiveAndDraftQuestions getActiveAndDraftQuestions();

  /**
   * Gets the question definition for a ID.
   *
   * @throws QuestionNotFoundException if the question for the ID does not exist.
   */
  QuestionDefinition getQuestionDefinition(long id) throws QuestionNotFoundException;
}
