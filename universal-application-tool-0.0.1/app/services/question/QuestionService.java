package services.question;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Locale;
import java.util.Optional;

public interface QuestionService {

  /**
   * Creates a new Question Definition. Returns a QuestionDefinition object on success and null on
   * failure.
   *
   * <p>This will fail if he path provided already resolves to a QuestionDefinition or Scalar.
   *
   * <p>NOTE: This does not update the version.
   */
  Optional<QuestionDefinition> create(QuestionDefinition definition);

  /**
   * Adds a new translation to an existing question definition. Returns true if the write is
   * successful.
   *
   * <p>The write will fail if:
   *
   * <p>- The path does not resolve to a QuestionDefinition.
   *
   * <p>- A translation with that Locale already exists for a given question path.
   *
   * <p>NOTE: This does not update the version.
   */
  boolean addTranslation(
      String path, Locale Locale, String questionText, Optional<String> questionHelpText)
      throws InvalidPathException;

  /**
   * Destructive overwrite of a question at a given path.
   *
   * <p>NOTE: This updates the service and question versions.
   */
  QuestionDefinition update(QuestionDefinition definition);

  /** Checks whether a specific path is valid. */
  boolean isValid(String pathString);

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
   * <p>If the path is invalid it will throw an InvalidPathException.
   */
  PathType getPathType(String pathString) throws InvalidPathException;

  /** Returns all question definitions for this version. */
  ImmutableList<QuestionDefinition> getAllQuestions();
}
