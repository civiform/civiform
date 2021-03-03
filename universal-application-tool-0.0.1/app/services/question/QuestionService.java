package services.question;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface QuestionService {

  /**
   * Get a {@link ReadOnlyQuestionService} which implements synchronous, in-memory read behavior for
   * questions.
   */
  CompletionStage<ReadOnlyQuestionService> getReadOnlyQuestionService();

  /**
   * Creates a new Question Definition. Returns a QuestionDefinition object on success and {@link
   * Optional#empty} on failure.
   *
   * <p>This will fail if the path provided already resolves to a QuestionDefinition or Scalar.
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
      String path, Locale locale, String questionText, Optional<String> questionHelpText)
      throws InvalidPathException;

  /**
   * Destructive overwrite of a question at a given path.
   *
   * <p>The write will fail if:
   *
   * <p>- The QuestionDefinition is not persisted yet.
   *
   * <p>- The path is different from the original path.
   *
   * <p>NOTE: This does not update the version.
   */
  QuestionDefinition update(QuestionDefinition definition) throws InvalidUpdateException;
}
