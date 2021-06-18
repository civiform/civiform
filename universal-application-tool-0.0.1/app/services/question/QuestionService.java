package services.question;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import models.QuestionTag;
import models.Version;
import services.CiviFormError;
import services.ErrorAnd;
import services.question.exceptions.InvalidUpdateException;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionDefinition;

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
   * questions in current active and draft versions.
   */
  CompletionStage<ReadOnlyQuestionService> getReadOnlyQuestionService();

  /**
   * Get a {@link ReadOnlyQuestionService} which implements synchronous, in-memory read behavior for
   * questions in a particular version.
   */
  ReadOnlyQuestionService getReadOnlyVersionedQuestionService(Version version);

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

  /** If this question is archived but a new version has not been published yet, un-archive it. */
  void restoreQuestion(Long id) throws InvalidUpdateException;

  /** If this question is not used in any program, archive it. */
  void archiveQuestion(Long id) throws InvalidUpdateException;

  /** If this is a draft question, remove it from the draft version and update all programs. */
  void discardDraft(Long id) throws InvalidUpdateException;

  /** Return all active questions which have the given tag. */
  ImmutableList<QuestionDefinition> getQuestionsForTag(QuestionTag tag);

  /** Set the export state of the question provided. */
  void setExportState(QuestionDefinition questionDefinition, QuestionTag questionExportState)
      throws QuestionNotFoundException, InvalidUpdateException;
}
