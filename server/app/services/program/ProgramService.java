package services.program;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import forms.BlockForm;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import models.Application;
import models.Program;
import play.libs.F;
import services.CiviFormError;
import services.ErrorAnd;
import services.IdentifierBasedPaginationSpec;
import services.PageNumberBasedPaginationSpec;
import services.PaginationResult;
import services.program.predicate.PredicateDefinition;
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
   * Get the definition for a given program.
   *
   * <p>This method loads question definitions for all block definitions from a version the program
   * is in. If the program contains a question that is not in any versions associated with the
   * program, a `RuntimeException` is thrown caused by an unexpected QuestionNotFoundException.
   *
   * @param id the ID of the program to retrieve
   * @return the {@link ProgramDefinition} for the given ID if it exists
   * @throws ProgramNotFoundException when ID does not correspond to a real Program
   */
  ProgramDefinition getProgramDefinition(long id) throws ProgramNotFoundException;

  /** Get the data object about the programs that are in the active or draft version. */
  ActiveAndDraftPrograms getActiveAndDraftPrograms();

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
   * Get the definition of a given program asynchronously. Gets the active version for the slug.
   *
   * @param programSlug the slug of the program to retrieve
   * @return the {@link ProgramDefinition} for the given slug if it exists, or a
   *     ProgramNotFoundException is thrown when the future completes and slug does not correspond
   *     to a real Program
   */
  CompletionStage<ProgramDefinition> getProgramDefinitionAsync(String programSlug);

  /**
   * Create a new program with an empty block.
   *
   * @param adminName a name for this program for internal use by admins - this is immutable once
   *     set
   * @param adminDescription a description of this program for use by admins
   * @param defaultDisplayName the name of this program to display to applicants
   * @param defaultDisplayDescription a description for this program to display to applicants
   * @param externalLink A link to an external page containing additional program details
   * @param displayMode The display mode for the program
   * @return the {@link ProgramDefinition} that was created if succeeded, or a set of errors if
   *     failed
   */
  ErrorAnd<ProgramDefinition, CiviFormError> createProgramDefinition(
      String adminName,
      String adminDescription,
      String defaultDisplayName,
      String defaultDisplayDescription,
      String externalLink,
      String displayMode);

  /**
   * Update a program's mutable fields: admin description, display name and description for
   * applicants.
   *
   * @param programId the ID of the program to update
   * @param locale the locale for this update - only applies to applicant display name and
   *     description
   * @param adminDescription the description of this program - visible only to admins
   * @param displayName a name for this program
   * @param displayDescription the description of what the program provides
   * @param externalLink A link to an external page containing additional program details
   * @param displayMode The display mode for the program
   * @return the {@link ProgramDefinition} that was updated if succeeded, or a set of errors if
   *     failed
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   */
  ErrorAnd<ProgramDefinition, CiviFormError> updateProgramDefinition(
      long programId,
      Locale locale,
      String adminDescription,
      String displayName,
      String displayDescription,
      String externalLink,
      String displayMode)
      throws ProgramNotFoundException;

  /**
   * Add or update a localization of the program's publicly-visible display name and description.
   *
   * @param programId the ID of the program to update
   * @param locale the {@link Locale} to update
   * @param displayName a localized display name for this program
   * @param displayDescription a localized description for this program
   * @return the {@link ProgramDefinition} that was successfully updated, or a set of errors if the
   *     update failed
   * @throws ProgramNotFoundException if the programId does not correspond to a valid program
   */
  ErrorAnd<ProgramDefinition, CiviFormError> updateLocalization(
      long programId, Locale locale, String displayName, String displayDescription)
      throws ProgramNotFoundException;

  /**
   * Adds an empty {@link BlockDefinition} to the end of a given program.
   *
   * @param programId the ID of the program to update
   * @return the {@link ProgramBlockAdditionResult} including the updated program and block if it
   *     succeeded, or a set of errors with the unmodified program and no block if failed.
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   */
  ErrorAnd<ProgramBlockAdditionResult, CiviFormError> addBlockToProgram(long programId)
      throws ProgramNotFoundException;

  /**
   * Adds an empty repeated {@link BlockDefinition} to the given program. The block should be added
   * after the last repeated or nested repeated block with the same ancestor. See {@link
   * ProgramDefinition#orderBlockDefinitions()} for more details about block positioning.
   *
   * @param programId the ID of the program to update
   * @param enumeratorBlockId ID of the enumerator block
   * @return a {@link ProgramBlockAdditionResult} including the updated program and block if it
   *     succeeded, or a set of errors with the unmodified program definition and no block if
   *     failed.
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockDefinitionNotFoundException when enumeratorBlockId does not correspond to
   *     an enumerator block in the Program.
   */
  ErrorAnd<ProgramBlockAdditionResult, CiviFormError> addRepeatedBlockToProgram(
      long programId, long enumeratorBlockId)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException;

  /**
   * Move the block definition one position in the direction specified. If the movement is not
   * allowed, then it is not moved.
   *
   * <p>Movement is not allowed if:
   *
   * <ul>
   *   <li>it would move the block past the ends of the list
   *   <li>it would move a repeated block such that it is not contiguous with its enumerator block's
   *       repeated and nested repeated blocks.
   * </ul>
   *
   * @param programId the ID of the program to update
   * @param blockId the ID of the block to move
   * @return the program definition, with the block moved if it is allowed.
   * @throws IllegalPredicateOrderingException if moving this block violates a program predicate
   */
  ProgramDefinition moveBlock(long programId, long blockId, ProgramDefinition.Direction direction)
      throws ProgramNotFoundException, IllegalPredicateOrderingException;

  /**
   * Update a {@link BlockDefinition}'s attributes.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param blockForm a {@link BlockForm} object containing the new attributes for the block
   * @return the {@link ProgramDefinition} that was updated if succeeded, or a set of errors with
   *     the unmodified program definition if failed
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockDefinitionNotFoundException when blockDefinitionId does not correspond to a
   *     real Block.
   */
  ErrorAnd<ProgramDefinition, CiviFormError> updateBlock(
      long programId, long blockDefinitionId, BlockForm blockForm)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException;

  /**
   * Update a {@link BlockDefinition} with a set of questions.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param programQuestionDefinitions an {@link ImmutableList} of questions for the block
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockDefinitionNotFoundException when blockDefinitionId does not correspond to a
   *     real Block.
   * @throws IllegalPredicateOrderingException if changing this block's questions invalidates a
   *     program predicate
   */
  ProgramDefinition setBlockQuestions(
      long programId,
      long blockDefinitionId,
      ImmutableList<ProgramQuestionDefinition> programQuestionDefinitions)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException,
          IllegalPredicateOrderingException;

  /**
   * Update a {@link BlockDefinition} to include additional questions.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param questionIds an {@link ImmutableList} of question IDs for the block
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockDefinitionNotFoundException when blockDefinitionId does not correspond to a
   *     real Block.
   * @throws QuestionNotFoundException when questionIds does not correspond to real Questions.
   * @throws DuplicateProgramQuestionException if the block already contains any of the Questions.
   */
  ProgramDefinition addQuestionsToBlock(
      long programId, long blockDefinitionId, ImmutableList<Long> questionIds)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException,
          QuestionNotFoundException, DuplicateProgramQuestionException;

  /**
   * Update a {@link BlockDefinition} to remove questions.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param questionIds an {@link ImmutableList} of question IDs to be removed from the block
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockDefinitionNotFoundException when blockDefinitionId does not correspond to a
   *     real Block.
   * @throws QuestionNotFoundException when questionIds does not correspond to real Questions.
   * @throws IllegalPredicateOrderingException if removing one or more of the questions invalidates
   *     a predicate - that is, there exists a predicate in this program that depends on at least
   *     one question to remove
   */
  ProgramDefinition removeQuestionsFromBlock(
      long programId, long blockDefinitionId, ImmutableList<Long> questionIds)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException,
          QuestionNotFoundException, IllegalPredicateOrderingException;

  /**
   * Set the visibility {@link PredicateDefinition} for a block. This predicate describes under what
   * conditions the block should be hidden from an applicant filling out the program form.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param predicate the {@link PredicateDefinition} for hiding the block
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockDefinitionNotFoundException when blockDefinitionId does not correspond to a
   *     real Block.
   * @throws IllegalPredicateOrderingException if this predicate cannot be added to this block
   */
  ProgramDefinition setBlockPredicate(
      long programId, long blockDefinitionId, PredicateDefinition predicate)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException,
          IllegalPredicateOrderingException;

  /**
   * Remove the visibility {@link PredicateDefinition} for a block.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockDefinitionNotFoundException when blockDefinitionId does not correspond to a
   *     real Block.
   */
  ProgramDefinition removeBlockPredicate(long programId, long blockDefinitionId)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException;

  /**
   * Delete a block from a program if the block ID is present. Otherwise, does nothing.
   *
   * @return the updated {@link ProgramDefinition}
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramNeedsABlockException when trying to delete the last block of a Program.
   * @throws IllegalPredicateOrderingException if deleting this block invalidates a predicate in
   *     this program
   */
  ProgramDefinition deleteBlock(long programId, long blockDefinitionId)
      throws ProgramNotFoundException, ProgramNeedsABlockException,
          IllegalPredicateOrderingException;

  /**
   * Set a program question definition to optional or required. If the question definition ID is not
   * present in the program's block, then nothing is changed.
   *
   * @param programId the ID of the program to update
   * @param blockDefinitionId the ID of the block to update
   * @param questionDefinitionId the ID of the question to update
   * @param optional boolean representing whether the question is optional or required
   * @return the updated program definition
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   * @throws ProgramBlockDefinitionNotFoundException when blockDefinitionId does not correspond to a
   *     real Block
   * @throws ProgramQuestionDefinitionNotFoundException when questionDefinitionId does not
   *     correspond to a real question in the block
   */
  ProgramDefinition setProgramQuestionDefinitionOptionality(
      long programId, long blockDefinitionId, long questionDefinitionId, boolean optional)
      throws ProgramNotFoundException, ProgramBlockDefinitionNotFoundException,
          ProgramQuestionDefinitionNotFoundException;

  /**
   * Get all the program's submitted applications. Does not include drafts or deleted applications.
   *
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   */
  ImmutableList<Application> getSubmittedProgramApplications(long programId)
      throws ProgramNotFoundException;

  /**
   * Get all submitted applications for this program and all other previous and future versions of
   * it where the applicant's first name, last name, email, or application ID contains the search
   * query. Does not include drafts or deleted applications.
   *
   * <p>If searchNameFragment is not an unsigned integer, the query will filter to applications with
   * email, first name, or last name that contain it.
   *
   * <p>If searchNameFragment is an unsigned integer, query will filter to applications with an
   * applicant ID matching it.
   *
   * @param paginationSpecEither the query supports two types of pagination, F.Either wraps the
   *     pagination spec to use for a given call.
   * @param searchNameFragment a text fragment used for filtering the applications.
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   */
  PaginationResult<Application> getSubmittedProgramApplicationsAllVersions(
      long programId,
      F.Either<IdentifierBasedPaginationSpec<Long>, PageNumberBasedPaginationSpec>
          paginationSpecEither,
      Optional<String> searchNameFragment)
      throws ProgramNotFoundException;

  /**
   * Get all submitted applications for this program and all other previous and future versions of
   * it where the application's submit time is in the specified range.
   *
   * @param paginationSpecEither the query supports two types of pagination, F.Either wraps the
   *     pagination spec to use for a given call.
   * @param submitTimeFrom specifies the oldest submission time to include.
   * @param submitTimeTo specifies a time all results must have been submitted before.
   * @throws ProgramNotFoundException when programId does not correspond to a real Program.
   */
  PaginationResult<Application> getSubmittedProgramApplicationsAllVersions(
      long programId,
      F.Either<IdentifierBasedPaginationSpec<Long>, PageNumberBasedPaginationSpec>
          paginationSpecEither,
      Optional<Instant> submitTimeFrom,
      Optional<Instant> submitTimeTo)
      throws ProgramNotFoundException;

  /** Create a new draft starting from the program specified by `id`. */
  ProgramDefinition newDraftOf(long id) throws ProgramNotFoundException;

  /**
   * Get the email addresses to send a notification to - the program admins if there are any, or the
   * global admins if none.
   */
  ImmutableList<String> getNotificationEmailAddresses(String programName);

  /** Get all other programs with the same name. */
  ImmutableList<Program> getOtherProgramVersions(long programId);

  /** Get all versions of the program with a version matching programId, including that one */
  ImmutableList<ProgramDefinition> getAllProgramDefinitionVersions(long programId);

  /** Get the names for active programs. */
  ImmutableSet<String> getActiveProgramNames();

  /** Get the names for all programs. */
  ImmutableSet<String> getAllProgramNames();

  /** Get the slugs for all programs. */
  ImmutableSet<String> getAllProgramSlugs();
}
