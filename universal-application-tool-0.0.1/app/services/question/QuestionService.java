package services.question;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;

public interface QuestionService {

  /**
   * Creates a new Question Definition.
   * Returns a QuestionDefinition object on success and null on failure.
   * 
   * This will fail if:
   *    - The path provided already resolves to a QuestionDefinition or Scalar.
   *
   * NOTE: This does not update the version.
   */
  public abstract Optional<QuestionDefinition> create(QuestionDefinition definition);
    
  /**
   * Adds a new translation to an existing question definition.
   * Returns true if the write is successful.
   * 
   * The write will fail if:
   *    - The path does not resolve to a QuestionDefinition.
   *    - A translation with that Locale already exists for a given question path. 
   *
   * NOTE: This does not update the version.
   */
  public abstract boolean addTranslation(
      String path, Locale Locale, String questionText, Optional<String> questionHelpText);
  
  /**
   * Destructive overwrite of a question at a given path.
   * 
   * NOTE: This updates the service and question versions.
   */
  public abstract QuestionDefinition update(QuestionDefinition definition);

  /** Checks whether a specific path is valid. */
  public abstract boolean isValid(String pathString);

  /** Gets the question definition for a given path. */
  public abstract Optional<QuestionDefinition> getQuestionDefinition(String pathString);

  /**
   * Returns all of the scalar properties for a given path. If the path is invalid this returns
   * Optional.empty().
   */
  public abstract Optional<ImmutableMap<String, ScalarType>> getPathScalars(String pathString);

  /** Gets the type of the node if it exist. Otherwise returns Optional.empty(). */
  public abstract Optional<PathType> getPathType(String pathString);

  /** Returns all question definitions for this version. */
  public abstract QuestionDefinition[] getAllQuestions();

  /**
   * Returns a map of full path to ScalarType for all scalars referenced in the question service.
   */
  public abstract ImmutableMap<String, ScalarType> getFullyQualifiedScalars();
}
