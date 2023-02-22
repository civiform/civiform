package repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.ebean.DB;
import io.ebean.Database;
import io.ebean.ExpressionList;
import io.ebean.PagedList;
import io.ebean.Query;
import io.ebean.SqlRow;
import io.ebean.Transaction;
import io.ebean.TxScope;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import javax.inject.Provider;
import models.Account;
import models.Application;
import models.LifecycleStage;
import models.Program;
import models.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.F;
import services.IdentifierBasedPaginationSpec;
import services.PageNumberBasedPaginationSpec;
import services.PaginationResult;
import services.Path;
import services.WellKnownPaths;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;

/**
 * ProgramRepository performs complicated operations on {@link Program} that often involve other
 * EBean models or asynchronous handling.
 */
public final class ProgramRepository {
  private static final Logger logger = LoggerFactory.getLogger(ProgramRepository.class);

  private final Database database;
  private final DatabaseExecutionContext executionContext;
  private final Provider<VersionRepository> versionRepository;

  @Inject
  public ProgramRepository(
      DatabaseExecutionContext executionContext, Provider<VersionRepository> versionRepository) {
    this.database = DB.getDefault();
    this.executionContext = checkNotNull(executionContext);
    this.versionRepository = checkNotNull(versionRepository);
  }

  public CompletionStage<Optional<Program>> lookupProgram(long id) {
    return supplyAsync(
        () -> database.find(Program.class).where().eq("id", id).findOneOrEmpty(), executionContext);
  }

  public Program insertProgramSync(Program program) {
    program.id = null;
    database.insert(program);
    program.refresh();
    return program;
  }

  public Program updateProgramSync(Program program) {
    database.update(program);
    return program;
  }

  public ImmutableSet<String> getAllProgramNames() {
    ImmutableSet.Builder<String> names = ImmutableSet.builder();
    List<SqlRow> rows = database.sqlQuery("SELECT DISTINCT name FROM programs").findList();

    for (SqlRow row : rows) {
      names.add(row.getString("name"));
    }

    return names.build();
  }

  public Boolean commonIntakeFormExists() {
    return database.find(Program.class).where().eq("program_type", "common_intake_form").exists();
  }

  /**
   * Makes {@code existingProgram} the DRAFT revision configuration of the question, creating a new
   * DRAFT if necessary.
   */
  public Program createOrUpdateDraft(Program existingProgram) {
    Version draftVersion = versionRepository.get().getDraftVersion();
    Optional<Program> existingDraftOpt =
        draftVersion.getProgramByName(existingProgram.getProgramDefinition().adminName());
    if (existingDraftOpt.isPresent()) {
      Program existingDraft = existingDraftOpt.get();
      if (!existingDraft.id.equals(existingProgram.id)) {
        // This may be indicative of a coding error, as it does a reset of the draft and not an
        // update of the draft, so log it.
        logger.warn(
            "Replacing Draft revision {} with definition from a different revision {}.",
            existingDraft.id,
            existingProgram.id);
      }
      Program updatedDraft =
          existingProgram.getProgramDefinition().toBuilder()
              .setId(existingDraft.id)
              .build()
              .toProgram();
      return updateProgramSync(updatedDraft);
    }

    // Inside a question update, this will be a savepoint rather than a full transaction.  Otherwise
    // it will be creating a new transaction.
    Transaction transaction = database.beginTransaction(TxScope.required());
    try {
      // Program -> builder -> back to program in order to clear any metadata stored in the program
      // (for example, version information).
      Program newDraft =
          new Program(existingProgram.getProgramDefinition().toBuilder().build(), draftVersion);
      newDraft = insertProgramSync(newDraft);
      draftVersion.refresh();
      Preconditions.checkState(
          draftVersion.getPrograms().contains(newDraft),
          "Must have successfully added draft version.");
      Preconditions.checkState(
          draftVersion.getLifecycleStage().equals(LifecycleStage.DRAFT),
          "Draft version must remain a draft throughout this transaction.");
      // Ensure we didn't add a duplicate with other code running at the same time.
      String programName = existingProgram.getProgramDefinition().adminName();
      Preconditions.checkState(
          draftVersion.getPrograms().stream()
                  .map(Program::getProgramDefinition)
                  .map(ProgramDefinition::adminName)
                  .filter(programName::equals)
                  .count()
              == 1,
          "Must be exactly one program with this name in the draft.");
      versionRepository.get().updateQuestionVersions(newDraft);
      transaction.commit();
      return newDraft;
    } catch (IllegalStateException e) {
      transaction.rollback();
      // We must end the transaction here since we are going to recurse and try again.
      // We cannot have this transaction on the thread-local transaction stack when that
      // happens.
      transaction.end();
      return createOrUpdateDraft(existingProgram);
    } finally {
      // This may come after a prior call to `transaction.end` in the event of a precondition
      // failure - this is okay, since it a double-call to `end` on a particular transaction.  Only
      // double calls to database.endTransaction must be avoided.
      transaction.end();
    }
  }

  /** Get the current active program with the provided slug. */
  public CompletableFuture<Program> getForSlug(String slug) {
    return supplyAsync(
        () -> {
          for (Program program : database.find(Program.class).where().isNull("slug").findList()) {
            program.getSlug();
            program.save();
          }
          ImmutableList<Program> activePrograms =
              versionRepository.get().getActiveVersion().getPrograms();
          List<Program> programsMatchingSlug =
              database.find(Program.class).where().eq("slug", slug).findList();
          return activePrograms.stream()
              .filter(programsMatchingSlug::contains)
              .findFirst()
              .orElseThrow(() -> new RuntimeException(new ProgramNotFoundException(slug)));
        },
        executionContext.current());
  }

  public ImmutableList<Account> getProgramAdministrators(String programName) {
    return ImmutableList.copyOf(
        database.find(Account.class).where().arrayContains("admin_of", programName).findList());
  }

  public ImmutableList<Account> getProgramAdministrators(long programId)
      throws ProgramNotFoundException {
    Optional<Program> program = database.find(Program.class).setId(programId).findOneOrEmpty();
    if (program.isEmpty()) {
      throw new ProgramNotFoundException(programId);
    }
    return getProgramAdministrators(program.get().getProgramDefinition().adminName());
  }

  public ImmutableList<Program> getAllProgramVersions(long programId) {
    Query<Program> programNameQuery =
        database
            .find(Program.class)
            .select("name")
            .where()
            .eq("id", programId)
            .setMaxRows(1)
            .query();

    return database
        .find(Program.class)
        .where()
        .in("name", programNameQuery)
        .query()
        .findList()
        .stream()
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Get all submitted applications for this program and all other previous and future versions of
   * it where the application matches the specified filters. Does not include drafts or deleted
   * applications. Results returned in reverse order that the applications were created.
   *
   * <p>Both offset-based and page number-based pagination are supported. For paginationSpecEither
   * the caller may pass either a {@link IdentifierBasedPaginationSpec <Long>} or {@link
   * PageNumberBasedPaginationSpec} using play's {@link F.Either} wrapper.
   */
  public PaginationResult<Application> getApplicationsForAllProgramVersions(
      long programId,
      F.Either<IdentifierBasedPaginationSpec<Long>, PageNumberBasedPaginationSpec>
          paginationSpecEither,
      SubmittedApplicationFilter filters) {
    ExpressionList<Application> query =
        database
            .find(Application.class)
            .fetch("program")
            .orderBy("id desc")
            .where()
            .in("program_id", allProgramVersionsQuery(programId))
            .in(
                "lifecycle_stage",
                ImmutableList.of(LifecycleStage.ACTIVE, LifecycleStage.OBSOLETE));

    if (filters.submitTimeFilter().fromTime().isPresent()) {
      query = query.where().ge("submit_time", filters.submitTimeFilter().fromTime().get());
    }

    if (filters.submitTimeFilter().untilTime().isPresent()) {
      query = query.where().lt("submit_time", filters.submitTimeFilter().untilTime().get());
    }

    if (filters.searchNameFragment().isPresent() && !filters.searchNameFragment().get().isBlank()) {
      String search = filters.searchNameFragment().get().trim();

      if (search.matches("^\\d+$")) {
        query = query.eq("id", Integer.parseInt(search));
      } else {
        String firstNamePath = getApplicationObjectPath(WellKnownPaths.APPLICANT_FIRST_NAME);
        String lastNamePath = getApplicationObjectPath(WellKnownPaths.APPLICANT_LAST_NAME);
        query =
            query
                .or()
                .ieq("submitter_email", search)
                .raw(firstNamePath + " || ' ' || " + lastNamePath + " ILIKE ?", "%" + search + "%")
                .raw(lastNamePath + " || ' ' || " + firstNamePath + " ILIKE ?", "%" + search + "%")
                .raw(lastNamePath + " || ', ' || " + firstNamePath + " ILIKE ?", "%" + search + "%")
                .endOr();
      }
    }

    String toMatchStatus = filters.applicationStatus().orElse("");
    if (!toMatchStatus.isBlank()) {
      if (toMatchStatus.equals(SubmittedApplicationFilter.NO_STATUS_FILTERS_OPTION_UUID)) {
        query = query.where().isNull("latest_status");
      } else {
        query = query.where().eq("latest_status", toMatchStatus);
      }
    }

    PagedList<Application> pagedQuery;

    if (paginationSpecEither.left.isPresent()) {
      IdentifierBasedPaginationSpec<Long> paginationSpec = paginationSpecEither.left.get();
      pagedQuery =
          query
              .where()
              .lt("id", paginationSpec.getCurrentPageOffsetIdentifier())
              .setMaxRows(paginationSpec.getPageSize())
              .findPagedList();
    } else {
      PageNumberBasedPaginationSpec paginationSpec = paginationSpecEither.right.get();
      pagedQuery =
          query
              .setFirstRow(paginationSpec.getCurrentPageOffset())
              .setMaxRows(paginationSpec.getPageSize())
              .findPagedList();
    }

    pagedQuery.loadCount();

    return new PaginationResult<Application>(
        pagedQuery.hasNext(),
        pagedQuery.getTotalPageCount(),
        pagedQuery.getList().stream().collect(ImmutableList.toImmutableList()));
  }

  private Query<Program> allProgramVersionsQuery(long programId) {
    Query<Program> programNameQuery =
        database.find(Program.class).select("name").where().eq("id", programId).query();

    return database.find(Program.class).select("id").where().in("name", programNameQuery).query();
  }

  private String getApplicationObjectPath(Path path) {
    StringBuilder result = new StringBuilder();

    result.append("(");

    // While the column type is JSONB, CiviForm writes the JSON object into the database as a
    // string.
    // This requires queries that interact with the JSON column to instruct postgres to parse the
    // contents
    // into JSON, which we do here with (object #>> '{}')::jsonb
    result.append("(object #>> '{}')::jsonb");

    int lastIndex = path.segments().size() - 1;
    for (int i = 0; i < lastIndex; i++) {
      result.append(" -> '");
      result.append(path.segments().get(i));
      result.append("'");
    }

    result.append(" ->> '");
    result.append(path.segments().get(lastIndex));
    result.append("')");

    return result.toString();
  }
}
