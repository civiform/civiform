package services.program;

import com.google.common.collect.ImmutableList;
import java.util.Optional;

/** Operations you can perform on {@link ProgramDefinition}s. */
public interface ProgramService {

  /**
   * List all programs.
   *
   * @return a list of {@link ProgramDefinition}s
   */
  ImmutableList<ProgramDefinition> listProgramDefinitions();

  /**
   * Get the definition for a given program.
   *
   * @param id the ID of the program to retrieve
   * @return an {@link Optional} of the {@link ProgramDefinition} for the given ID if it exists, or
   *     {@code Optional#empty} if not
   */
  Optional<ProgramDefinition> getProgramDefinition(long id);

  /**
   * Create a new program.
   *
   * @param name a name for this program
   * @param description the description of what the program provides
   * @return the {@link ProgramDefinition} that was created
   */
  ProgramDefinition createProgram(String name, String description);

  /**
   * Adds a {@link BlockDefinition} to the given program.
   *
   * @param programId the ID of the program to update
   * @param blockName a name for the block to add
   * @param blockDescription a description of what the questions in the block address
   * @return the updated {@link ProgramDefinition}
   */
  ProgramDefinition addBlockToProgram(long programId, String blockName, String blockDescription);

  /**
   * Update a {@link BlockDefinition} with a set of questions.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param questionDefinitionIds an {@link ImmutableList} of questions for the block
   * @return the updated {@link ProgramDefinition}
   */
  ProgramDefinition setBlockQuestions(
      long programId, int blockDefinitionId, ImmutableList<String> questionDefinitionIds);

  /**
   * Set the hide {@link Predicate} for a block. This predicate describes under what conditions the
   * block should be hidden from an applicant filling out the program form.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param predicate the {@link Predicate} for hiding the block
   * @return the updated {@link ProgramDefinition}
   */
  ProgramDefinition setBlockHidePredicate(long programId, int blockDefinitionId, Predicate predicate);

  /**
   * Set the optional {@link Predicate} for a block. This predicate describes under what conditions
   * the block should be optional when filling out the program form.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param predicate the {@link Predicate} for making the block optional
   * @return the updated {@link ProgramDefinition}
   */
  ProgramDefinition setBlockOptionalPredicate(long programId, int blockDefinitionId, Predicate predicate);
}
