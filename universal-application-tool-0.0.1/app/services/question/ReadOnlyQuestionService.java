package services.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;
import services.Path;
import services.question.exceptions.InvalidPathException;
import services.question.exceptions.InvalidQuestionTypeException;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionDefinition;
import services.question.types.RepeaterQuestionDefinition;
import services.question.types.ScalarType;

/**
 * The ReadOnlyQuestionService contains all synchronous, in-memory operations for
 * QuestionDefinitions.
 */
public interface ReadOnlyQuestionService {

  /** Returns all question definitions. */
  ImmutableList<QuestionDefinition> getAllQuestions();

  /** Returns all up-to-date question definitions for this version. */
  ImmutableList<QuestionDefinition> getUpToDateQuestions();

  /** Returns all repeater question definitions. */
  ImmutableList<RepeaterQuestionDefinition> getAllRepeaterQuestions();

  /** Returns all repeater question definitions. */
  ImmutableList<RepeaterQuestionDefinition> getUpToDateRepeaterQuestions();

  /** Returns all scalars for this version. */
  ImmutableMap<Path, ScalarType> getAllScalars();

  /**
   * Create the {@link Path} for a question from the path of the repeater id (if provided) and the
   * question name.
   */
  Path makePath(Optional<Long> repeaterId, String questionName, boolean isRepeater)
      throws InvalidQuestionTypeException, QuestionNotFoundException;

  /**
   * Returns all of the scalar properties for a given path.
   *
   * <p>If the path is to a QUESTION, it will return the question's scalar objects.
   *
   * <p>If the path is to a SCALAR, it will return a single scalar.
   *
   * <p>If the path is invalid it will throw an InvalidPathException.
   */
  ImmutableMap<Path, ScalarType> getPathScalars(Path path) throws InvalidPathException;

  /**
   * Gets the type of the node if it exist.
   *
   * <p>If the path is invalid it will return PathType.NONE.
   */
  PathType getPathType(Path path);

  /**
   * Gets the question definition for a ID.
   *
   * @throws QuestionNotFoundException if the question for the ID does not exist.
   */
  QuestionDefinition getQuestionDefinition(long id) throws QuestionNotFoundException;

  /**
   * Checks whether a specific path is valid. A path is valid in this context if it represents a
   * real path to a value.
   */
  boolean isValid(Path path);

  /**
   * When getting question text and help text we need to send the Locale. If absent it will use the
   * preferred locale.
   */
  void setPreferredLocale(Locale locale);

  Locale getPreferredLocale();
}
