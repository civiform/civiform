package services.question;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import services.CiviFormError;
import services.ErrorAnd;
import services.Path;

/**
 * The service responsible for accessing the Question resource. Admins create {@link
 * QuestionDefinition}s which are consumed by {@link services.program.ProgramService} to define
 * program-specific applications and {@link services.applicant.ApplicantService} for storing
 * applicants' answers to questions. The full set of questions at a given version defines the data
 * that can be collected for a given applicant across all programs.
 */
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
  ErrorAnd<QuestionDefinition, CiviFormError> create(QuestionDefinition definition);

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
      Path path, Locale locale, String questionText, Optional<String> questionHelpText)
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
  ErrorAnd<QuestionDefinition, CiviFormError> update(QuestionDefinition definition)
      throws InvalidUpdateException;
}
