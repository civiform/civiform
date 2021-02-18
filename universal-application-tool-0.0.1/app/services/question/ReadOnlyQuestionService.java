package services.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;

/**
 * The ReadOnlyQuestionService contains all synchronous, in-memory operations for
 * QuestionDefinitions.
 */
interface ReadOnlyQuestionService {

  /** Returns all question definitions for this version. */
  ImmutableList<QuestionDefinition> getAllQuestions();

  /** Returns all scalars for this version. */
  ImmutableMap<String, ScalarType> getAllScalars();

  /**
   * Returns all of the scalar properties for a given path.
   *
   * <p>If the path is to a QUESTION, it will return the question's scalar objects.
   *
   * <p>If the path is to a SCALAR, it will return a single scalar.
   *
   * <p>If the path is invalid it will throw an InvalidPathException.
   */
  ImmutableMap<String, ScalarType> getPathScalars(String pathString) throws InvalidPathException;

  /**
   * Gets the type of the node if it exist.
   *
   * <p>If the path is invalid it will return PathType.NONE.
   */
  PathType getPathType(String pathString);

  /**
   * Gets the question definition for a given path.
   *
   * <p>If the path is to a QUESTION, it will return that.
   *
   * <p>If the path is to a SCALAR, it will return the parent QuestionDefinition for that Scalar.
   *
   * <p>If the path is invalid it will throw an InvalidPathException.
   */
  QuestionDefinition getQuestionDefinition(String pathString) throws InvalidPathException;

  /** Checks whether a specific path is valid. */
  boolean isValid(String pathString);

  /**
   * When getting question text and help text we need to send the Locale. If absent it will use the
   * preferred locale.
   */
  void setPreferredLocale(Locale locale);
}
