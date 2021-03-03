package services.program;

import com.google.common.collect.ImmutableList;
import forms.BlockForm;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import services.question.QuestionNotFoundException;

/** Operations you can perform on {@link ProgramDefinition}s. */
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
   * @return an {@link Optional} of the {@link ProgramDefinition} for the given ID if it exists, or
   *     {@code Optional#empty} if not
   */
  Optional<ProgramDefinition> getProgramDefinition(long id);

  /**
   * Get the definition of a given program asynchronously.
   *
   * @param id the ID of the program to retrieve
   * @return an {@link Optional} of the {@link ProgramDefinition} for the given ID if it exists, or
   *     {@code Optional#empty} if not
   */
  CompletionStage<Optional<ProgramDefinition>> getProgramDefinitionAsync(long id);

  /**
   * Create a new program.
   *
   * @param name a name for this program
   * @param description the description of what the program provides
   * @return the {@link ProgramDefinition} that was created
   */
  ProgramDefinition createProgramDefinition(String name, String description);

  /**
   * Update a program's name and description.
   *
   * @param programId the ID of the program to update
   * @param name a name for this program
   * @param description the description of what the program provides
   * @return the {@link ProgramDefinition} that was updated
   */
  ProgramDefinition updateProgramDefinition(long programId, String name, String description)
      throws ProgramNotFoundException;

  /**
   * Adds a {@link BlockDefinition} to the given program.
   *
   * @param programId the ID of the program to update
   * @param blockName a name for the block to add
   * @param blockDescription a description of what the questions in the block address
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   */
  ProgramDefinition addBlockToProgram(long programId, String blockName, String blockDescription)
      throws ProgramNotFoundException;

  /**
   * Adds an empty {@link BlockDefinition} to the given program.
   *
   * @param programId the ID of the program to update
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   */
  ProgramDefinition addBlockToProgram(long programId) throws ProgramNotFoundException;

  /**
   * Adds a {@link BlockDefinition} to the given program.
   *
   * @param programId the ID of the program to update
   * @param blockName a name for the block to add
   * @param blockDescription a description of what the questions in the block address
   * @param questionDefinitions an {@link ImmutableList} of questions for the block
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   */
  ProgramDefinition addBlockToProgram(
      long programId,
      String blockName,
      String blockDescription,
      ImmutableList<ProgramQuestionDefinition> questionDefinitions)
      throws ProgramNotFoundException;

  /**
   * Update a {@link BlockDefinition}'s attributes.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param blockForm a {@link BlockForm} object containing the new attributes for the block
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockNotFoundException when blockID does not correspond to a real Block.
   */
  ProgramDefinition updateBlock(long programId, long blockDefinitionId, BlockForm blockForm)
      throws ProgramNotFoundException, ProgramBlockNotFoundException;

  /**
   * Update a {@link BlockDefinition} with a set of questions.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param programQuestionDefinitions an {@link ImmutableList} of questions for the block
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
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
   * @return the updated {@link ProgramDefinition} * @throws ProgramNotFoundException when programId
   *     does not correspond to a real Program.
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
   */
  ProgramDefinition setBlockOptionalPredicate(
      long programId, long blockDefinitionId, Predicate predicate)
      throws ProgramNotFoundException, ProgramBlockNotFoundException;

  /**
   * Delete a block from a program if the block ID is present. Otherwise, does nothing.
   *
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   */
  ProgramDefinition deleteBlock(long programId, long blockDefinitionId)
      throws ProgramNotFoundException;
}
