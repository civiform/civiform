package services.applicant;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.CompletionStage;
import models.Applicant;
import models.Application;
import services.program.ProgramDefinition;

/**
 * The service responsible for accessing the Applicant resource. Applicants can view program
 * applications defined by the {@link services.program.ProgramService} as a series of {@link
 * Block}s, one per-page. When an applicant submits the form for a Block the ApplicantService is
 * responsible for validating and persisting their answers and then providing the next Block for
 * them to view, if any.
 */
public interface ApplicantService {

  /**
   * Attempts to perform a set of updates to the applicant's {@link ApplicantData}. If updates are
   * valid, they are saved to storage. If not, a set of errors are returned along with the modified
   * {@link ApplicantData}, but none of the updates are persisted to storage.
   *
   * <p>Updates are atomic i.e. if any of them fail validation, none of them will be written.
   *
   * @return a {@link ReadOnlyApplicantProgramService} that reflects the updates regardless of
   *     whether they are presisted or not, which may have invalid data with errors associated with
   *     it. If the service cannot perform the update due to exceptions, they are wrapped in
   *     `CompletionException`s.
   *     <p>Below list all possible exceptions:
   *     <p>
   *     <ul>
   *       <li>`ApplicantNotFoundException` - Invalid applicantId is given.
   *       <li>`IllegalArgumentException` - Invalid paths that collide with reserved keys.
   *       <li>`PathNotInBlockException` - Specified paths do not point to any scalars defined in
   *           the block.
   *       <li>`ProgramBlockNotFoundException` - Invalid combination of programId and blockId is
   *           given.
   *       <li>`ProgramNotFoundException` - Specified programId does not correspond to a real
   *           Program.
   *       <li>`UnsupportedScalarTypeException` - Specified paths point to an unsupported type of
   *           scalar.
   *     </ul>
   */
  CompletionStage<ReadOnlyApplicantProgramService> stageAndUpdateIfValid(
      long applicantId, long programId, String blockId, ImmutableMap<String, String> updateMap);

  /** Creates a new {@link models.Applicant} at for latest application version for a given user. */
  CompletionStage<Applicant> createApplicant(long userId);

  /**
   * Get a {@link ReadOnlyApplicantProgramService} which implements synchronous, in-memory read
   * behavior relevant to an applicant for a specific program.
   *
   * <p>A ProgramNotFoundException may be thrown when the future completes if the programId does not
   * correspond to a real Program.
   */
  CompletionStage<ReadOnlyApplicantProgramService> getReadOnlyApplicantProgramService(
      long applicantId, long programId);

  /** Get a {@link ReadOnlyApplicantProgramService} from an application. */
  CompletionStage<ReadOnlyApplicantProgramService> getReadOnlyApplicantProgramService(
      Application application);

  /**
   * Returns all programs that are appropriate to serve to an applicant - which is any active
   * program, plus any program where they have an application in the draft stage.
   */
  CompletionStage<ImmutableList<ProgramDefinition>> relevantPrograms(long applicantId);
}
