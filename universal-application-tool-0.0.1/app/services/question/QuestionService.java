package services.question;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;

public abstract class QuestionService {
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
