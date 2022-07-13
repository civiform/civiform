package services.applicant;

import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import models.Applicant;
import models.Application;
import models.LifecycleStage;
import repository.TimeFilter;
import services.applicant.exception.ApplicationSubmissionException;
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
   * Attempt to perform a set of updates to the applicant's {@link ApplicantData}. If updates are
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

  /**
   * Create a new active {@link Application} for the applicant applying to the program.
   *
   * <p>An application is a snapshot of all the answers the applicant has filled in so far, along
   * with association with the applicant and a program that the applicant is applying to.
   *
   * @return the saved {@link Application}. If the submission failed, a {@link
   *     ApplicationSubmissionException} is thrown and wrapped in a `CompletionException`.
   */
  CompletionStage<Application> submitApplication(
      long applicantId, long programId, CiviFormProfile submittingProfile);

  /** Create a new {@link Applicant} for a given user. */
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

  /** Get a {@link ReadOnlyApplicantProgramService} from an application and program definition. */
  ReadOnlyApplicantProgramService getReadOnlyApplicantProgramService(
      Application application, ProgramDefinition programDefinition);

  /**
   * Return all programs that are appropriate to serve to an applicant - which is any active program
   * that is public and any program where they have an application in the draft stage.
   *
   * <p>The programs do not have question definitions loaded into its program question definitions.
   */
  CompletionStage<ImmutableMap<LifecycleStage, ImmutableList<ProgramDefinition>>> relevantPrograms(
      long applicantId);

  /** Return the name of the given applicant id. */
  CompletionStage<Optional<String>> getName(long applicantId);

  /** Return the email of the given applicant id if they have one. */
  CompletionStage<Optional<String>> getEmail(long applicantId);

  /**
   * Return a filtered set of applications, including applications from previous versions, with
   * program, applicant, and account associations eager loaded. Results are ordered by application
   * ID in ascending order.
   */
  ImmutableList<Application> getApplications(TimeFilter submitTimeFilter);
}
