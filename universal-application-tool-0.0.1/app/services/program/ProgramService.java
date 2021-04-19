package services.program;

import com.google.common.collect.ImmutableList;
import forms.BlockForm;
import java.util.concurrent.CompletionStage;
import models.Application;
import services.CiviFormError;
import services.ErrorAnd;
import services.question.exceptions.QuestionNotFoundException;
import services.question.types.QuestionDefinition;

/**
 * The service responsible for accessing the Program resource. Admins create programs to represent
 * specific benefits programs that applicants can apply for. Each program consists of a list of
 * sequential {@link BlockDefinition}s that are rendered one per-page for the applicant. A {@link
 * BlockDefinition} contains one or more {@link QuestionDefinition}s defined in the {@link
 * services.question.QuestionService}.
 */
public interface ProgramService {

  /**
   * List all programs.
   *
   * @return a list of {@link ProgramDefinition}s
   */
  ImmutableList<ProgramDefinition> listProgramDefinitions();

  /**
   * List all programs asynchronously.
   *
   * @return a list of {@link ProgramDefinition}s
   */
  CompletionStage<ImmutableList<ProgramDefinition>> listProgramDefinitionsAsync();

  /**
   * Get the definition for a given program.
   *
   * @param id the ID of the program to retrieve
   * @return the {@link ProgramDefinition} for the given ID if it exists
   * @throws ProgramNotFoundException when ID does not correspond to a real Program
   */
  ProgramDefinition getProgramDefinition(long id) throws ProgramNotFoundException;

  /**
   * Get the definition of a given program asynchronously.
   *
   * @param id the ID of the program to retrieve
   * @return the {@link ProgramDefinition} for the given ID if it exists, or a
   *     ProgramNotFoundException is thrown when the future completes and ID does not correspond to
   *     a real Program
   */
  CompletionStage<ProgramDefinition> getProgramDefinitionAsync(long id);

  /**
   * Create a new program with an empty block.
   *
   * @param name a name for this program
   * @param description the description of what the program provides
   * @return the {@link ProgramDefinition} that was created if succeeded, or a set of errors if
   *     failed
   */
  ErrorAnd<ProgramDefinition, CiviFormError> createProgramDefinition(
      String name, String description);

  /**
   * Update a program's name and description.
   *
   * @param programId the ID of the program to update
   * @param name a name for this program
   * @param description the description of what the program provides
   * @return the {@link ProgramDefinition} that was updated if succeeded, or a set of errors if
   *     failed
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   */
  ErrorAnd<ProgramDefinition, CiviFormError> updateProgramDefinition(
      long programId, String name, String description) throws ProgramNotFoundException;

  /**
   * Adds an empty {@link BlockDefinition} to the given program.
   *
   * @param programId the ID of the program to update
   * @return the {@link ProgramDefinition} that was updated if succeeded, or a set of errors with
   *     the unmodified program definition if failed
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   */
  ErrorAnd<ProgramDefinition, CiviFormError> addBlockToProgram(long programId)
      throws ProgramNotFoundException;

  /**
   * Adds an empty repeated {@link BlockDefinition} to the given program.
   *
   * @param programId the ID of the program to update
   * @param repeaterId an reference to a repeater block
   * @return the {@link ProgramDefinition} that was updated if succeeded, or a set of errors with
   *     the unmodified program definition if failed
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   */
  ErrorAnd<ProgramDefinition, CiviFormError> addRepeatedBlockToProgram(
      long programId, long repeaterId) throws ProgramNotFoundException;

  /**
   * Update a {@link BlockDefinition}'s attributes.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param blockForm a {@link BlockForm} object containing the new attributes for the block
   * @return the {@link ProgramDefinition} that was updated if succeeded, or a set of errors with
   *     the unmodified program definition if failed
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockNotFoundException when blockDefinitionId does not correspond to a real
   *     Block.
   */
  ErrorAnd<ProgramDefinition, CiviFormError> updateBlock(
      long programId, long blockDefinitionId, BlockForm blockForm)
      throws ProgramNotFoundException, ProgramBlockNotFoundException;

  /**
   * Update a {@link BlockDefinition} with a set of questions.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param programQuestionDefinitions an {@link ImmutableList} of questions for the block
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockNotFoundException when blockDefinitionId does not correspond to a real
   *     Block.
   */
  ProgramDefinition setBlockQuestions(
      long programId,
      long blockDefinitionId,
      ImmutableList<ProgramQuestionDefinition> programQuestionDefinitions)
      throws ProgramNotFoundException, ProgramBlockNotFoundException;

  /**
   * Update a {@link BlockDefinition} to include additional questions.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param questionIds an {@link ImmutableList} of question IDs for the block
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockNotFoundException when blockDefinitionId does not correspond to a real
   *     Block.
   * @throws QuestionNotFoundException when questionIds does not correspond to real Questions.
   * @throws DuplicateProgramQuestionException if the block already contains any of the Questions.
   */
  ProgramDefinition addQuestionsToBlock(
      long programId, long blockDefinitionId, ImmutableList<Long> questionIds)
      throws ProgramNotFoundException, ProgramBlockNotFoundException, QuestionNotFoundException,
          DuplicateProgramQuestionException;

  /**
   * Update a {@link BlockDefinition} to remove questions.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param questionIds an {@link ImmutableList} of question IDs to be removed from the block
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockNotFoundException when blockDefinitionId does not correspond to a real
   *     Block.
   * @throws QuestionNotFoundException when questionIds does not correspond to real Questions.
   */
  ProgramDefinition removeQuestionsFromBlock(
      long programId, long blockDefinitionId, ImmutableList<Long> questionIds)
      throws ProgramNotFoundException, ProgramBlockNotFoundException, QuestionNotFoundException;

  /**
   * Set the hide {@link Predicate} for a block. This predicate describes under what conditions the
   * block should be hidden from an applicant filling out the program form.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param predicate the {@link Predicate} for hiding the block
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockNotFoundException when blockDefinitionId does not correspond to a real
   *     Block.
   */
  ProgramDefinition setBlockHidePredicate(
      long programId, long blockDefinitionId, Predicate predicate)
      throws ProgramNotFoundException, ProgramBlockNotFoundException;

  /**
   * Set the optional {@link Predicate} for a block. This predicate describes under what conditions
   * the block should be optional when filling out the program form.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param predicate the {@link Predicate} for making the block optional
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockNotFoundException when blockDefinitionId does not correspond to a real
   *     Block.
   */
  ProgramDefinition setBlockOptionalPredicate(
      long programId, long blockDefinitionId, Predicate predicate)
      throws ProgramNotFoundException, ProgramBlockNotFoundException;

  /**
   * Delete a block from a program if the block ID is present. Otherwise, does nothing.
   *
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramNeedsABlockException when trying to delete the last block of a Program.
   */
  ProgramDefinition deleteBlock(long programId, long blockDefinitionId)
      throws ProgramNotFoundException, ProgramNeedsABlockException;

  /**
   * Get all the program's applications.
   *
   * @param programId the program id.
   * @return A list of Application objects for the specified program.
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   */
  ImmutableList<Application> getProgramApplications(long programId) throws ProgramNotFoundException;

  /** Create a new draft starting from the program specified by `id`. */
  ProgramDefinition newDraftOf(long id) throws ProgramNotFoundException;
}
