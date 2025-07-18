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
import io.ebean.annotation.TxIsolation;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
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
import services.pagination.BasePaginationSpec;
import services.pagination.PaginationResult;
import services.program.BlockDefinition;
import services.program.ProgramDefinition;
import services.program.ProgramDraftNotFoundException;
import services.program.ProgramNotFoundException;
import services.program.ProgramQuestionDefinition;
import services.question.types.QuestionDefinition;
import services.question.types.QuestionType;
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
  private final DatabaseExecutionContext dbExecutionContext;
  private final Provider<VersionRepository> versionRepository;
  private final SettingsManifest settingsManifest;
  private final SyncCacheApi programCache;
  private final SyncCacheApi programDefCache;
  private final SyncCacheApi versionsByProgramCache;
  private final TransactionManager transactionManager;

  @Inject
  public ProgramRepository(
      DatabaseExecutionContext dbExecutionContext,
      Provider<VersionRepository> versionRepository,
      SettingsManifest settingsManifest,
      @NamedCache("program") SyncCacheApi programCache,
      @NamedCache("full-program-definition") SyncCacheApi programDefCache,
      @NamedCache("program-versions") SyncCacheApi versionsByProgramCache) {
    this.database = DB.getDefault();
    this.dbExecutionContext = checkNotNull(dbExecutionContext);
    this.versionRepository = checkNotNull(versionRepository);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.programCache = checkNotNull(programCache);
    this.programDefCache = checkNotNull(programDefCache);
    this.versionsByProgramCache = checkNotNull(versionsByProgramCache);
    this.transactionManager = new TransactionManager();
  }

  public CompletionStage<Optional<ProgramModel>> lookupProgram(long id) {
    // Use the cache if it is enabled and there isn't a draft version in progress.
    if (settingsManifest.getProgramCacheEnabled()
        && versionRepository.get().getDraftVersion().isEmpty()) {
      return supplyAsync(
          () -> programCache.getOrElseUpdate(String.valueOf(id), () -> lookupProgramSync(id)),
          dbExecutionContext);
    }
    return supplyAsync(() -> lookupProgramSync(id), dbExecutionContext);
  }

  public boolean checkProgramAdminNameExists(String name) {
    return database
        .find(ProgramModel.class)
        .setLabel("ProgramModel.findByName")
        .setProfileLocation(queryProfileLocationBuilder.create("lookupProgramByAdminName"))
        .where()
        .eq("name", name)
        .exists();
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
          String.valueOf(program.id), program::getVersions);
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

  public ImmutableSet<String> getAllNonExternalProgramNames() {
    ImmutableSet.Builder<String> names = ImmutableSet.builder();
    List<SqlRow> rows =
        database
            .sqlQuery("SELECT DISTINCT name FROM programs WHERE program_type <> 'external'")
            .findList();

    for (SqlRow row : rows) {
      names.add(row.getString("name"));
    }

    return names.build();
  }

  /**
   * Retrieves the program definition for the given program.
   *
   * <p>This method prioritizes fetching the program definition from the cache. If not found in the
   * cache, it retrieves the definition directly from the program model.
   *
   * <p>This method should replace any calls to ProgramModel.getProgramDefinition()
   */
  public ProgramDefinition getShallowProgramDefinition(ProgramModel program) {
    return getFullProgramDefinitionFromCache(program).orElseGet(program::getProgramDefinition);
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
    if (!settingsManifest.getQuestionCacheEnabled()
        // We only set the cache if it hasn't yet been set for the ID.
        || getFullProgramDefinitionFromCache(programId).isPresent()) {
      return;
    }
    // We should never set the cache for draft programs.
    if (versionRepository.get().isDraftProgram(programId)) {
      return;
    }
    ImmutableList<BlockDefinition> blocksWithNullQuestion =
        programDefinition.blockDefinitions().stream()
            .filter(BlockDefinition::hasNullQuestion)
            .collect(ImmutableList.toImmutableList());
    if (blocksWithNullQuestion.isEmpty()) {
      programDefCache.set(String.valueOf(programId), programDefinition);
      return;
    }

    String nullQuestionIds =
        blocksWithNullQuestion.stream()
            .flatMap(block -> block.programQuestionDefinitions().stream())
            .map(ProgramQuestionDefinition::getQuestionDefinition)
            .filter(qd -> qd.getQuestionType().equals(QuestionType.NULL_QUESTION))
            .map(QuestionDefinition::getId)
            .map(String::valueOf)
            .collect(Collectors.joining(", "));
    logger.warn(
        "Program {} with ID {} has the following null question ID(s): {} so we won't set it"
            + " into the cache. This is an issue in {} / {} blocks.",
        programDefinition.slug(),
        programDefinition.id(),
        nullQuestionIds,
        blocksWithNullQuestion.size(),
        programDefinition.blockDefinitions().size());
  }

  /**
   * Makes {@code existingProgram} the DRAFT revision configuration of the program, creating a new
   * DRAFT if necessary.
   */
  public ProgramModel createOrUpdateDraft(ProgramModel existingProgram) {
    return createOrUpdateDraft(getShallowProgramDefinition(existingProgram));
  }

  public ProgramModel createOrUpdateDraft(ProgramDefinition existingProgram) {
    // Inside a question update, this will be a savepoint rather than a full transaction.
    // Otherwise, it will be creating a new transaction.
    // After the fact note: Based on the docs this isn't a savepoint as the
    // above says, because setNestedUseSavepoint() was not called, other code
    // does do that FWIW.
    // https://ebean.io/docs/transactions/savepoints
    Transaction transaction =
        database.beginTransaction(TxScope.required().setIsolation(TxIsolation.SERIALIZABLE));
    try {
      // Replace the existing draft if present.
      VersionModel draftVersion = versionRepository.get().getDraftVersionOrCreate();
      Optional<ProgramModel> optionalExistingProgramDraft =
          versionRepository
              .get()
              .getProgramByNameForVersion(existingProgram.adminName(), draftVersion);
      if (optionalExistingProgramDraft.isPresent()) {
        ProgramModel existingProgramDraft = optionalExistingProgramDraft.get();
        if (!existingProgramDraft.id.equals(existingProgram.id())) {
          // This may be indicative of a coding error, as it does a reset of the draft and not an
          // update of the draft, so log it.
          logger.warn(
              "Replacing Draft revision {} with definition from a different revision {}.",
              existingProgramDraft.id,
              existingProgram.id());
        }
        // After the fact comment: This appears to largely be a no-op outside
        // the previous warning if block.  If that block doesn't trigger then
        // the ids are the same and nothing is being changed.
        ProgramModel updatedDraft =
            existingProgram.toBuilder().setId(existingProgramDraft.id).build().toProgram();
        updateProgramSync(updatedDraft);
        transaction.commit();
        return updatedDraft;
      }

      // Create a draft.
      // Program -> builder -> back to program in order to clear any metadata stored in the program
      // (for example, version information).
      ProgramModel newDraft = new ProgramModel(existingProgram.toBuilder().build(), draftVersion);
      newDraft = insertProgramSync(newDraft);
      draftVersion.refresh();
      Preconditions.checkState(
          versionRepository.get().getProgramsForVersion(draftVersion).contains(newDraft),
          "Must have successfully added draft version.");
      Preconditions.checkState(
          draftVersion.getLifecycleStage().equals(LifecycleStage.DRAFT),
          "Draft version must remain a draft throughout this transaction.");
      // Ensure we didn't add a duplicate with other code running at the same time.
      String programName = existingProgram.adminName();
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
      // After the fact comment: potential infinite loop here for systemic
      // issues. Unclear from the original 2021 PR what situation warranted
      // catching IllegalStateException, and then also retrying when the
      // exception is typically for un-retryable things.
      return createOrUpdateDraft(existingProgram);
    } finally {
      // This may come after a prior call to `transaction.end` in the event of a precondition
      // failure - this is okay, since it a double-call to `end` on a particular transaction.  Only
      // double calls to database.endTransaction must be avoided.
      transaction.end();
    }
  }

  /** Returns the slug of {@code} programId. */
  public String getSlug(long programId) throws ProgramNotFoundException {
    // Check the cache first
    Optional<ProgramDefinition> cachedProgram = getFullProgramDefinitionFromCache(programId);
    if (cachedProgram.isPresent()) {
      return cachedProgram.get().slug();
    }

    // Lookup the slug
    Optional<SqlRow> maybeRow =
        database
            .sqlQuery(String.format("SELECT slug FROM programs WHERE id = %d", programId))
            .findOneOrEmpty();

    return maybeRow
        .map(row -> row.getString("slug"))
        .orElseThrow(() -> new ProgramNotFoundException(programId));
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
        dbExecutionContext.current());
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

  /** Get the current active or draft program with the provided slug. */
  public CompletableFuture<ProgramModel> getDraftOrActiveProgramFromSlug(String slug) {
    return supplyAsync(
        () ->
            transactionManager.execute(
                () -> {
                  ImmutableList<ProgramModel> activePrograms =
                      versionRepository
                          .get()
                          .getProgramsForVersion(versionRepository.get().getActiveVersion());
                  ImmutableList<ProgramModel> draftPrograms =
                      versionRepository
                          .get()
                          .getProgramsForVersion(versionRepository.get().getDraftVersion());
                  Optional<ProgramModel> foundDraftProgram =
                      draftPrograms.stream()
                          .filter(draftProgram -> draftProgram.getSlug().equals(slug))
                          .findFirst();

                  return foundDraftProgram.orElseGet(
                      () ->
                          activePrograms.stream()
                              .filter(activeProgram -> activeProgram.getSlug().equals(slug))
                              .findFirst()
                              .orElseThrow(
                                  () -> new RuntimeException(new ProgramNotFoundException(slug))));
                }));
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
        .fetch("categories")
        .where()
        .in("name", programNameQuery)
        .query()
        .findList()
        .stream()
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Get all submitted applications for this program and all other previous and future versions of
   * it where the application matches the specified filters. Results returned in reverse order that
   * the applications were created.
   *
   * <p>Pagination is supported via the passed {@link BasePaginationSpec} object.
   */
  public PaginationResult<ApplicationModel> getApplicationsForAllProgramVersions(
      long programId, BasePaginationSpec paginationSpec, SubmittedApplicationFilter filters) {
    ExpressionList<ApplicationModel> query =
        database
            .find(ApplicationModel.class)
            .setLabel("ApplicationModel.findList")
            .setProfileLocation(
                queryProfileLocationBuilder.create("getApplicationsForAllProgramVersions"))
            .fetch("applicant")
            .fetch("applicant.account.managedByGroup")
            .where()
            .in("program_id", allProgramVersionsQuery(programId))
            .in("lifecycle_stage", filters.lifecycleStages());

    if (filters.submitTimeFilter().fromTime().isPresent()) {
      query = query.where().ge("submit_time", filters.submitTimeFilter().fromTime().get());
    }

    if (filters.submitTimeFilter().untilTime().isPresent()) {
      query = query.where().lt("submit_time", filters.submitTimeFilter().untilTime().get());
    }

    if (filters.searchNameFragment().isPresent() && !filters.searchNameFragment().get().isBlank()) {
      String search = filters.searchNameFragment().get().trim();
      query = searchUsingPrimaryApplicantInfo(search, query);
    }

    String toMatchStatus = filters.applicationStatus().orElse("");
    if (!toMatchStatus.isBlank()) {
      if (toMatchStatus.equals(SubmittedApplicationFilter.NO_STATUS_FILTERS_OPTION_UUID)) {
        query = query.where().isNull("latest_status");
      } else {
        query = query.where().eq("latest_status", toMatchStatus);
      }
    }

    // Sort order is dictated by the pagination spec that was specified.
    PagedList<ApplicationModel> pagedQuery = paginationSpec.apply(query.query()).findPagedList();
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

  /**
   * Add an ExpressionList to {@code query} that searches applications based on the implied contents
   * of {@code search}.
   *
   * <p>A phone number only search is done if {@code search} contains no alphabetic characters and
   * some numeric characters. All other characters are removed.
   *
   * <p>Otherwise, a broad search is returned which includes a match on any of phone number,
   * submitter email, and first name last name patterns.
   */
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
    }
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

  /**
   * Get the most recent id for the active program. In the case that there are no active versions of
   * a program, an empty value is returned.
   *
   * @param programId to use when looking for the most recent program
   * @return The programId of the most recent active program or empty.
   */
  public Optional<Long> getMostRecentActiveProgramId(long programId) {
    /*
     * We need for this to always get the most recent active program ID, thus we are unable to
     * cache the value without building out a much more complicated caching solution. This will
     * be called frequently enough that I'm electing to go with a native sql query.  Attempts to
     * have ebeans build the sql resulted in slower queries and much more difficult to follow
     * queries. This is taking less than 1ms.
     */
    final String sql =
        """
        select max(programs.id)
        from programs
        inner join versions_programs
          on versions_programs.programs_id = programs.id
        inner join versions
          on versions_programs.versions_id = versions.id
        where versions.lifecycle_stage = 'active'
        and programs.name =
        (
          select name
          from programs
          where id = :programId
          limit 1
        )
        limit 1
        """;

    Long latestProgramId =
        database
            .sqlQuery(sql)
            .setLabel("ProgramRepository.getMostRecentActiveProgramId")
            .setParameter("programId", programId)
            .mapToScalar(Long.class)
            .findOne();

    return Optional.ofNullable(latestProgramId);
  }
}
