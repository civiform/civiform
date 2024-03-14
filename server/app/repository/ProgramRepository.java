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
import models.AccountModel;
import models.ApplicationModel;
import models.LifecycleStage;
import models.ProgramModel;
import models.VersionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.cache.NamedCache;
import play.cache.SyncCacheApi;
import play.libs.F;
import play.mvc.Http.Request;
import services.IdentifierBasedPaginationSpec;
import services.PageNumberBasedPaginationSpec;
import services.PaginationResult;
import services.Path;
import services.WellKnownPaths;
import services.program.ProgramDefinition;
import services.program.ProgramDraftNotFoundException;
import services.program.ProgramNotFoundException;
import services.settings.SettingsManifest;

/**
 * ProgramRepository performs complicated operations on {@link ProgramModel} that often involve
 * other EBean models or asynchronous handling.
 */
public final class ProgramRepository {
  private static final Logger logger = LoggerFactory.getLogger(ProgramRepository.class);
  private static final QueryProfileLocationBuilder queryProfileLocationBuilder =
      new QueryProfileLocationBuilder("ProgramRepository");

  private final Database database;
  private final DatabaseExecutionContext executionContext;
  private final Provider<VersionRepository> versionRepository;
  private final SettingsManifest settingsManifest;
  private final SyncCacheApi programCache;
  private final SyncCacheApi programDefCache;
  private final SyncCacheApi versionsByProgramCache;

  @Inject
  public ProgramRepository(
      DatabaseExecutionContext executionContext,
      Provider<VersionRepository> versionRepository,
      SettingsManifest settingsManifest,
      @NamedCache("program") SyncCacheApi programCache,
      @NamedCache("full-program-definition") SyncCacheApi programDefCache,
      @NamedCache("program-versions") SyncCacheApi versionsByProgramCache) {
    this.database = DB.getDefault();
    this.executionContext = checkNotNull(executionContext);
    this.versionRepository = checkNotNull(versionRepository);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.programCache = checkNotNull(programCache);
    this.programDefCache = checkNotNull(programDefCache);
    this.versionsByProgramCache = checkNotNull(versionsByProgramCache);
  }

  public CompletionStage<Optional<ProgramModel>> lookupProgram(long id) {
    // Use the cache if it is enabled and there isn't a draft version in progress.
    if (settingsManifest.getProgramCacheEnabled()
        && !versionRepository.get().getDraftVersion().isPresent()) {
      return supplyAsync(
          () -> programCache.getOrElseUpdate(String.valueOf(id), () -> lookupProgramSync(id)),
          executionContext);
    }
    return supplyAsync(() -> lookupProgramSync(id), executionContext);
  }

  private Optional<ProgramModel> lookupProgramSync(long id) {
    return database
        .find(ProgramModel.class)
        .setLabel("ProgramModel.findById")
        .setProfileLocation(queryProfileLocationBuilder.create("lookupProgramSync"))
        .where()
        .eq("id", id)
        .findOneOrEmpty();
  }

  public ProgramModel insertProgramSync(ProgramModel program) {
    program.id = null;
    database.insert(program);
    program.refresh();
    return program;
  }

  public ProgramModel updateProgramSync(ProgramModel program) {
    database.update(program);
    return program;
  }

  public ImmutableList<VersionModel> getVersionsForProgram(ProgramModel program) {
    if (settingsManifest.getProgramCacheEnabled()) {
      return versionsByProgramCache.getOrElseUpdate(
          String.valueOf(program.id), () -> program.getVersions());
    }
    return program.getVersions();
  }

  public ImmutableSet<String> getAllProgramNames() {
    ImmutableSet.Builder<String> names = ImmutableSet.builder();
    List<SqlRow> rows = database.sqlQuery("SELECT DISTINCT name FROM programs").findList();

    for (SqlRow row : rows) {
      names.add(row.getString("name"));
    }

    return names.build();
  }

  /**
   * Gets the program definition without the associated question data.
   *
   * <p>This method should replace any calls to ProgramModel.getProgramDefinition()
   */
  public ProgramDefinition getShallowProgramDefinition(ProgramModel program) {
    return program.getProgramDefinition();
  }

  /**
   * Gets the program definition that contains the related question data from the cache (if
   * enabled).
   */
  public Optional<ProgramDefinition> getFullProgramDefinitionFromCache(ProgramModel program) {
    return getFullProgramDefinitionFromCache(program.id);
  }

  public Optional<ProgramDefinition> getFullProgramDefinitionFromCache(long programId) {
    if (settingsManifest.getQuestionCacheEnabled()) {
      return programDefCache.get(String.valueOf(programId));
    }
    return Optional.empty();
  }

  /**
   * Sets the program definition that contains the related question data in the cache (if enabled).
   *
   * <p>Draft program definition data must not be set in the cache.
   */
  public void setFullProgramDefinitionCache(long programId, ProgramDefinition programDefinition) {
    if (settingsManifest.getQuestionCacheEnabled()
        // We only set the cache if it hasn't yet been set for the ID.
        && getFullProgramDefinitionFromCache(programId).isEmpty()) {
      // We should never set the cache for draft programs.
      if (!versionRepository.get().isDraftProgram(programId)) {
        programDefCache.set(String.valueOf(programId), programDefinition);
      }
    }
  }

  /**
   * Makes {@code existingProgram} the DRAFT revision configuration of the question, creating a new
   * DRAFT if necessary.
   */
  public ProgramModel createOrUpdateDraft(ProgramModel existingProgram) {
    VersionModel draftVersion = versionRepository.get().getDraftVersionOrCreate();
    Optional<ProgramModel> existingDraftOpt =
        versionRepository
            .get()
            .getProgramByNameForVersion(
                getShallowProgramDefinition(existingProgram).adminName(), draftVersion);
    if (existingDraftOpt.isPresent()) {
      ProgramModel existingDraft = existingDraftOpt.get();
      if (!existingDraft.id.equals(existingProgram.id)) {
        // This may be indicative of a coding error, as it does a reset of the draft and not an
        // update of the draft, so log it.
        logger.warn(
            "Replacing Draft revision {} with definition from a different revision {}.",
            existingDraft.id,
            existingProgram.id);
      }
      ProgramModel updatedDraft =
          getShallowProgramDefinition(existingProgram).toBuilder()
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
      ProgramModel newDraft =
          new ProgramModel(
              getShallowProgramDefinition(existingProgram).toBuilder().build(), draftVersion);
      newDraft = insertProgramSync(newDraft);
      draftVersion.refresh();
      Preconditions.checkState(
          versionRepository.get().getProgramsForVersion(draftVersion).contains(newDraft),
          "Must have successfully added draft version.");
      Preconditions.checkState(
          draftVersion.getLifecycleStage().equals(LifecycleStage.DRAFT),
          "Draft version must remain a draft throughout this transaction.");
      // Ensure we didn't add a duplicate with other code running at the same time.
      String programName = getShallowProgramDefinition(existingProgram).adminName();
      Preconditions.checkState(
          versionRepository.get().getProgramsForVersion(draftVersion).stream()
                  .map(this::getShallowProgramDefinition)
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
  public CompletableFuture<ProgramModel> getActiveProgramFromSlug(String slug) {
    return supplyAsync(
        () -> {
          ImmutableList<ProgramModel> activePrograms =
              versionRepository
                  .get()
                  .getProgramsForVersion(versionRepository.get().getActiveVersion());
          return activePrograms.stream()
              .filter(activeProgram -> activeProgram.getSlug().equals(slug))
              .findFirst()
              .orElseThrow(() -> new RuntimeException(new ProgramNotFoundException(slug)));
        },
        executionContext.current());
  }

  /** Get the current draft program with the provided slug. */
  public ProgramModel getDraftProgramFromSlug(String slug) throws ProgramDraftNotFoundException {

    Optional<VersionModel> version = versionRepository.get().getDraftVersion();

    if (version.isEmpty()) {
      throw new ProgramDraftNotFoundException(slug);
    }

    ImmutableList<ProgramModel> draftPrograms =
        versionRepository.get().getProgramsForVersion(version.get());

    return draftPrograms.stream()
        .filter(draftProgram -> draftProgram.getSlug().equals(slug))
        .findFirst()
        .orElseThrow(() -> new ProgramDraftNotFoundException(slug));
  }

  public ImmutableList<AccountModel> getProgramAdministrators(String programName) {
    return ImmutableList.copyOf(
        database
            .find(AccountModel.class)
            .setLabel("Account.findList")
            .setProfileLocation(queryProfileLocationBuilder.create("getProgramAdministrators"))
            .where()
            .arrayContains("admin_of", programName)
            .findList());
  }

  public ImmutableList<AccountModel> getProgramAdministrators(long programId)
      throws ProgramNotFoundException {
    Optional<ProgramModel> program =
        database
            .find(ProgramModel.class)
            .setLabel("ProgramModel.findById")
            .setProfileLocation(queryProfileLocationBuilder.create("getProgramAdministrators"))
            .setId(programId)
            .findOneOrEmpty();
    if (program.isEmpty()) {
      throw new ProgramNotFoundException(programId);
    }
    return getProgramAdministrators(getShallowProgramDefinition(program.get()).adminName());
  }

  public ImmutableList<ProgramModel> getAllProgramVersions(long programId) {
    Query<ProgramModel> programNameQuery =
        database
            .find(ProgramModel.class)
            .setLabel("ProgramModel.findById")
            .setProfileLocation(queryProfileLocationBuilder.create("getAllProgramVersions"))
            .select("name")
            .where()
            .eq("id", programId)
            .setMaxRows(1)
            .query();

    return database
        .find(ProgramModel.class)
        .setLabel("ProgramModel.findList")
        .setProfileLocation(queryProfileLocationBuilder.create("getAllProgramVersions"))
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
  public PaginationResult<ApplicationModel> getApplicationsForAllProgramVersions(
      long programId,
      F.Either<IdentifierBasedPaginationSpec<Long>, PageNumberBasedPaginationSpec>
          paginationSpecEither,
      SubmittedApplicationFilter filters,
      Request request) {
    ExpressionList<ApplicationModel> query =
        database
            .find(ApplicationModel.class)
            .setLabel("ApplicationModel.findList")
            .setProfileLocation(
                queryProfileLocationBuilder.create("getApplicationsForAllProgramVersions"))
            .fetch("program")
            .fetch("applicant")
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
      if (settingsManifest.getPrimaryApplicantInfoQuestionsEnabled(request)) {
        System.out.println("searching using PAI");
        query = searchUsingPrimaryApplicantInfo(search, query);
      } else {
        System.out.println("searching using WKP");
        query = searchUsingWellKnownPaths(search, query);
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

    PagedList<ApplicationModel> pagedQuery;

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

    return new PaginationResult<ApplicationModel>(
        pagedQuery.hasNext(),
        pagedQuery.getTotalPageCount(),
        pagedQuery.getList().stream().collect(ImmutableList.toImmutableList()));
  }

  private Query<ProgramModel> allProgramVersionsQuery(long programId) {
    Query<ProgramModel> programNameQuery =
        database
            .find(ProgramModel.class)
            .select("name")
            .setLabel("ProgramModel.findByName")
            .setProfileLocation(queryProfileLocationBuilder.create("allProgramVersionsQuery"))
            .where()
            .eq("id", programId)
            .query();

    return database
        .find(ProgramModel.class)
        .select("id")
        .setLabel("ProgramModel.findById")
        .setProfileLocation(queryProfileLocationBuilder.create("allProgramVersionsQuery"))
        .where()
        .in("name", programNameQuery)
        .query();
  }

  private ExpressionList<ApplicationModel> searchUsingPrimaryApplicantInfo(
      String search, ExpressionList<ApplicationModel> query) {
    // Remove all special characters
    String maybeOnlyDigits = search.replaceAll("[^a-zA-Z0-9]", "");
    // Check if remaining string is actually only digits
    if (maybeOnlyDigits.matches("^\\d+$")) {
      return query
          .or()
          .eq("id", Long.parseLong(maybeOnlyDigits))
          .ilike("applicant.phoneNumber", "%" + maybeOnlyDigits + "%")
          .endOr();
    } else {
      String firstNamePath = "applicant.firstName";
      String lastNamePath = "applicant.lastName";
      return query
          .or()
          .ilike("applicant.emailAddress", "%" + search + "%")
          .ilike("submitter_email", "%" + search + "%")
          .ilike(firstNamePath + " || ' ' || " + lastNamePath, "%" + search + "%")
          .ilike(lastNamePath + " || ' ' || " + firstNamePath, "%" + search + "%")
          .ilike(lastNamePath + " || ', ' || " + firstNamePath, "%" + search + "%")
          .endOr();
    }
  }

  // TODO (#5503): Remove this when we remove the PRIMARY_APPLICANT_INFO_QUESTIONS_ENABLED feature
  // flag
  private ExpressionList<ApplicationModel> searchUsingWellKnownPaths(
      String search, ExpressionList<ApplicationModel> query) {

    if (search.matches("^\\d+$")) {
      return query.eq("id", Integer.parseInt(search));
    } else {
      String firstNamePath = getApplicationObjectPath(WellKnownPaths.APPLICANT_FIRST_NAME);
      String lastNamePath = getApplicationObjectPath(WellKnownPaths.APPLICANT_LAST_NAME);
      return query
          .or()
          .raw("applicant.account.emailAddress ILIKE ?", "%" + search + "%")
          .raw("submitter_email ILIKE ?", "%" + search + "%")
          .raw(firstNamePath + " || ' ' || " + lastNamePath + " ILIKE ?", "%" + search + "%")
          .raw(lastNamePath + " || ' ' || " + firstNamePath + " ILIKE ?", "%" + search + "%")
          .raw(lastNamePath + " || ', ' || " + firstNamePath + " ILIKE ?", "%" + search + "%")
          .endOr();
    }
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
