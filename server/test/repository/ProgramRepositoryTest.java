package repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import auth.ProgramAcls;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.util.Providers;
import io.ebean.DB;
import io.ebean.DataIntegrityException;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import models.AccountModel;
import models.ApplicantModel;
import models.ApplicationEventModel;
import models.ApplicationModel;
import models.DisplayMode;
import models.ProgramModel;
import models.VersionModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import play.cache.NamedCacheImpl;
import play.cache.SyncCacheApi;
import play.inject.BindingKey;
import play.libs.F;
import services.IdentifierBasedPaginationSpec;
import services.LocalizedStrings;
import services.PageNumberBasedPaginationSpec;
import services.PaginationResult;
import services.WellKnownPaths;
import services.applicant.ApplicantData;
import services.application.ApplicationEventDetails;
import services.application.ApplicationEventDetails.StatusEvent;
import services.program.ProgramDefinition;
import services.program.ProgramNotFoundException;
import services.program.ProgramType;
import services.program.StatusDefinitions;
import services.question.QuestionAnswerer;
import services.settings.SettingsManifest;
import support.CfTestHelpers;
import support.ProgramBuilder;

@RunWith(JUnitParamsRunner.class)
public class ProgramRepositoryTest extends ResetPostgres {

  private ProgramRepository repo;
  private VersionRepository versionRepo;
  private SyncCacheApi programCache;
  private SyncCacheApi programDefCache;
  private SyncCacheApi versionsByProgramCache;
  private SettingsManifest mockSettingsManifest;

  @Before
  public void setup() {
    versionRepo = instanceOf(VersionRepository.class);
    mockSettingsManifest = Mockito.mock(SettingsManifest.class);
    programCache = instanceOf(SyncCacheApi.class);
    versionsByProgramCache = instanceOf(SyncCacheApi.class);

    BindingKey<SyncCacheApi> programDefKey =
        new BindingKey<>(SyncCacheApi.class)
            .qualifiedWith(new NamedCacheImpl("program-definition"));
    programDefCache = instanceOf(programDefKey.asScala());

    repo =
        new ProgramRepository(
            instanceOf(DatabaseExecutionContext.class),
            Providers.of(versionRepo),
            mockSettingsManifest,
            programCache,
            programDefCache,
            versionsByProgramCache);
  }

  @Test
  public void lookupProgram_returnsEmptyOptionalWhenProgramNotFound() {
    Optional<ProgramModel> found = repo.lookupProgram(1L).toCompletableFuture().join();

    assertThat(found).isEmpty();
  }

  @Test
  public void lookupProgram_findsCorrectProgram() {
    resourceCreator.insertActiveProgram("one");
    ProgramModel two = resourceCreator.insertActiveProgram("two");

    Optional<ProgramModel> found = repo.lookupProgram(two.id).toCompletableFuture().join();

    assertThat(found).hasValue(two);
  }

  @Test
  public void lookupProgram_usesCacheWhenEnabled() {
    Mockito.when(mockSettingsManifest.getProgramCacheEnabled()).thenReturn(true);

    resourceCreator.insertActiveProgram("one");
    ProgramModel two = resourceCreator.insertActiveProgram("two");

    assertThat(programCache.get(String.valueOf(two.id))).isEmpty();

    Optional<ProgramModel> found = repo.lookupProgram(two.id).toCompletableFuture().join();

    assertThat(found).hasValue(two);
    assertThat(programCache.get(String.valueOf(two.id))).hasValue(found);
  }

  @Test
  public void setFullProgramDefinitionFromCache_doesNotSetWhenDraft() {
    Mockito.when(mockSettingsManifest.getQuestionCacheEnabled()).thenReturn(true);
    ProgramModel program = resourceCreator.insertDraftProgram("testDraftInCache");

    repo.setFullProgramDefinitionCache(program.id, program.getProgramDefinition());
    Optional<ProgramDefinition> programDefFromCache =
        repo.getFullProgramDefinitionFromCache(program);

    assertThat(programDefFromCache).isEmpty();
  }

  @Test
  public void getFullProgramDefinitionFromCache_getsFromCacheWhenPresent() {
    Mockito.when(mockSettingsManifest.getQuestionCacheEnabled()).thenReturn(true);
    ProgramModel program = resourceCreator.insertActiveProgram("testInCache");
    repo.setFullProgramDefinitionCache(program.id, program.getProgramDefinition());

    Optional<ProgramDefinition> programDefFromCache =
        repo.getFullProgramDefinitionFromCache(program);

    assertThat(programDefFromCache).isPresent();
    assertThat(programDefFromCache.get().getQuestionIdsInProgram())
        .isEqualTo(program.getProgramDefinition().getQuestionIdsInProgram());
  }

  @Test
  public void getFullProgramDefinitionFromCache_returnsEmptyOptionalWhenNotPresent() {
    Mockito.when(mockSettingsManifest.getQuestionCacheEnabled()).thenReturn(true);
    ProgramModel program = resourceCreator.insertActiveProgram("testNotInCache");

    // We don't set the cache, but we try to get it here.
    Optional<ProgramDefinition> programDefFromCache =
        repo.getFullProgramDefinitionFromCache(program);

    assertThat(programDefFromCache).isEmpty();
  }

  @Test
  public void getFullProgramDefinitionFromCache_returnsEmptyOptionalWhenCacheDisabled() {
    Mockito.when(mockSettingsManifest.getQuestionCacheEnabled()).thenReturn(false);
    ProgramModel program = resourceCreator.insertActiveProgram("testCacheDisabled");
    repo.setFullProgramDefinitionCache(program.id, program.getProgramDefinition());

    Optional<ProgramDefinition> programDefFromCache =
        repo.getFullProgramDefinitionFromCache(program);

    assertThat(programDefFromCache).isEmpty();
  }

  // Verify the StatusDefinitions default value in evolution 40 loads.
  @Test
  public void loadStatusDefinitionsEvolution() {
    DB.sqlUpdate(
            "insert into programs (name, description, block_definitions, status_definitions,"
                + " localized_name, localized_description, program_type) values ('Status Default',"
                + " 'Description', '[]', '{\"statuses\": []}', '{\"isRequired\": true,"
                + " \"translations\": {\"en_US\": \"Status Default\"}}',  '{\"isRequired\": true,"
                + " \"translations\": {\"en_US\": \"\"}}', 'default');")
        .execute();
    DB.sqlUpdate(
            "insert into versions_programs (versions_id, programs_id) values ("
                + "(select id from versions where lifecycle_stage = 'active'),"
                + "(select id from programs where name = 'Status Default'));")
        .execute();

    ProgramModel found =
        versionRepo.getActiveVersion().getPrograms().stream()
            .filter(program -> program.getProgramDefinition().adminName().equals("Status Default"))
            .findFirst()
            .get();

    assertThat(found.getProgramDefinition().adminName()).isEqualTo("Status Default");
    assertThat(found.getStatusDefinitions().getStatuses()).isEmpty();
  }

  @Test
  public void getForSlug_findsCorrectProgram() {
    ProgramModel program = resourceCreator.insertActiveProgram("Something With A Name");

    ProgramModel found =
        repo.getActiveProgramFromSlug("something-with-a-name").toCompletableFuture().join();

    assertThat(found).isEqualTo(program);
  }

  /* This test is meant to exercise the database trigger defined in server/conf/evolutions/default/56.sql */
  @Test
  public void insertingDuplicateDraftPrograms_raisesDatabaseException() throws Exception {
    var versionRepo = instanceOf(VersionRepository.class);
    var draftVersion = versionRepo.getDraftVersionOrCreate();

    ProgramModel program = resourceCreator.insertActiveProgram("test");
    assertThat(program.id).isNotNull();

    var draftOne =
        new ProgramModel(
            "test-program",
            "desc",
            "test-program",
            "description",
            "",
            "",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            draftVersion,
            ProgramType.DEFAULT,
            new ProgramAcls());
    draftOne.save();

    var draftTwo =
        new ProgramModel(
            "test-program",
            "desc",
            "test-program",
            "description",
            "",
            "",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            draftVersion,
            ProgramType.DEFAULT,
            new ProgramAcls());

    var throwableAssert = assertThatThrownBy(() -> draftTwo.save());
    throwableAssert.hasMessageContaining("Program test-program already has a draft!");
    throwableAssert.isExactlyInstanceOf(DataIntegrityException.class);
  }

  @Test
  public void insertProgramSync() throws Exception {
    ProgramModel program =
        new ProgramModel(
            "ProgramRepository",
            "desc",
            "name",
            "description",
            "",
            "",
            DisplayMode.PUBLIC.getValue(),
            ImmutableList.of(),
            versionRepo.getDraftVersionOrCreate(),
            ProgramType.DEFAULT,
            new ProgramAcls());
    ProgramModel withId = repo.insertProgramSync(program);

    ProgramModel found = repo.lookupProgram(withId.id).toCompletableFuture().join().get();
    assertThat(found.getProgramDefinition().localizedName().get(Locale.US)).isEqualTo("name");
  }

  @Test
  public void updateProgramSync() {
    ProgramModel existing = resourceCreator.insertActiveProgram("old name");
    ProgramModel updates =
        new ProgramModel(
            existing.getProgramDefinition().toBuilder()
                .setLocalizedName(LocalizedStrings.of(Locale.US, "new name"))
                .build());

    ProgramModel updated = repo.updateProgramSync(updates);

    assertThat(updated.getProgramDefinition().id()).isEqualTo(existing.id);
    assertThat(updated.getProgramDefinition().localizedName())
        .isEqualTo(LocalizedStrings.of(Locale.US, "new name"));
  }

  @Test
  public void getAllProgramNames() {
    resourceCreator.insertActiveProgram("old name");
    resourceCreator.insertDraftProgram("old name");
    resourceCreator.insertDraftProgram("new name");

    ImmutableSet<String> result = repo.getAllProgramNames();

    assertThat(result).isEqualTo(ImmutableSet.of("old name", "new name"));
  }

  @Test
  public void getVersionsForProgram() {
    ProgramModel program = resourceCreator.insertActiveProgram("old name");

    ImmutableList<VersionModel> versions = repo.getVersionsForProgram(program);

    assertThat(versions).hasSize(1);
  }

  @Test
  public void getVersionsForProgram_usesCacheWhenEnabled() {
    Mockito.when(mockSettingsManifest.getProgramCacheEnabled()).thenReturn(true);
    ProgramModel program = resourceCreator.insertActiveProgram("old name");

    ImmutableList<VersionModel> versions = repo.getVersionsForProgram(program);

    assertThat(versionsByProgramCache.get(String.valueOf(program.id)).get()).isEqualTo(versions);
  }

  @Test
  public void returnsAllAdmins() throws ProgramNotFoundException {
    ProgramModel withAdmins = resourceCreator.insertActiveProgram("with admins");
    AccountModel admin = new AccountModel();
    admin.save();
    assertThat(repo.getProgramAdministrators(withAdmins.id)).isEmpty();
    admin.addAdministeredProgram(withAdmins.getProgramDefinition());
    admin.save();
    assertThat(repo.getProgramAdministrators(withAdmins.id)).containsExactly(admin);

    // This draft, despite not existing when the admin association happened, should
    // still have the same admin associated.
    ProgramModel newDraft = repo.createOrUpdateDraft(withAdmins);
    assertThat(repo.getProgramAdministrators(newDraft.id)).containsExactly(admin);
  }

  @Test
  public void getApplicationsForAllProgramVersions_searchById() {
    ProgramModel program = resourceCreator.insertActiveProgram("test program");

    ApplicantModel bob = resourceCreator.insertApplicantWithAccount(Optional.of("bob@example.com"));
    ApplicationModel bobApp = makeApplicationWithName(bob, program, "Bob", "MiddleName", "Doe");
    ApplicantModel jane =
        resourceCreator.insertApplicantWithAccount(Optional.of("jane@example.com"));
    makeApplicationWithName(jane, program, "Jane", "MiddleName", "Doe");

    PaginationResult<ApplicationModel> paginationResult =
        repo.getApplicationsForAllProgramVersions(
            program.id,
            F.Either.Left(IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG),
            SubmittedApplicationFilter.builder()
                .setSearchNameFragment(Optional.of(bobApp.id.toString()))
                .setSubmitTimeFilter(TimeFilter.EMPTY)
                .build());

    assertThat(
            paginationResult.getPageContents().stream()
                .map(a -> a.getApplicant().getAccount().getEmailAddress())
                .collect(ImmutableSet.toImmutableSet()))
        .containsExactly("bob@example.com");
    assertThat(paginationResult.getNumPages()).isEqualTo(1);
  }

  private static ImmutableList<Object[]> getSearchByNameOrEmailData() {
    // Assumes that the test has been seeded with three applications:
    // 1. bob@example.com - Bob Doe
    // 2. jane@example.com - Jane Doe
    // 3. chris@exAMPLE.com - Chris Person
    return ImmutableList.<Object[]>of(
        new Object[] {"Bob Doe", ImmutableSet.of("bob@example.com")},
        new Object[] {"Doe Bob", ImmutableSet.of("bob@example.com")},
        new Object[] {"Doe, Bob", ImmutableSet.of("bob@example.com")},
        new Object[] {"Doe", ImmutableSet.of("bob@example.com", "jane@example.com")},
        new Object[] {"Bob", ImmutableSet.of("bob@example.com")},
        new Object[] {"Person", ImmutableSet.of("chris@exAMPLE.com")},
        new Object[] {"Other Person", ImmutableSet.of()},

        // Searching by applicant email or TI email returns the application
        new Object[] {"bob@example.com", ImmutableSet.of("bob@example.com")},
        new Object[] {"bobs_ti@example.com", ImmutableSet.of("bob@example.com")},

        // Searching by partial email returns the application
        new Object[] {
          "example", ImmutableSet.of("bob@example.com", "jane@example.com", "chris@exAMPLE.com")
        },
        new Object[] {"bobs_ti", ImmutableSet.of("bob@example.com")},

        // Case insensitive search.
        new Object[] {"bOb dOe", ImmutableSet.of("bob@example.com")},
        new Object[] {"CHRIS@example.com", ImmutableSet.of("chris@exAMPLE.com")},

        // Leading and trailing whitespace is ignored.
        new Object[] {"    Bob Doe    ", ImmutableSet.of("bob@example.com")},

        // Degenerate cases.
        // Email isn't found.
        new Object[] {"fake@example.com", ImmutableSet.of()},
        // Only match a single space between first and last name.
        new Object[] {"Bob  Doe", ImmutableSet.of()});
  }

  @Test
  @Parameters(method = "getSearchByNameOrEmailData")
  public void getApplicationsForAllProgramVersions_searchByNameOrEmail(
      String searchFragment, ImmutableSet<String> wantEmails) {
    ProgramModel program = resourceCreator.insertActiveProgram("test program");

    ApplicantModel bob = resourceCreator.insertApplicantWithAccount(Optional.of("bob@example.com"));
    makeApplicationWithName(bob, program, "Bob", "MiddleName", "Doe")
        .setSubmitterEmail("bobs_ti@example.com")
        .save();
    ApplicantModel jane =
        resourceCreator.insertApplicantWithAccount(Optional.of("jane@example.com"));
    makeApplicationWithName(jane, program, "Jane", "MiddleName", "Doe");
    // Note: The mixed casing on the email is intentional for tests of case insensitivity.
    ApplicantModel chris =
        resourceCreator.insertApplicantWithAccount(Optional.of("chris@exAMPLE.com"));
    makeApplicationWithName(chris, program, "Chris", "MiddleName", "Person");

    ApplicantModel otherApplicant =
        resourceCreator.insertApplicantWithAccount(Optional.of("other@example.com"));
    resourceCreator.insertDraftApplication(otherApplicant, program);

    PaginationResult<ApplicationModel> paginationResult =
        repo.getApplicationsForAllProgramVersions(
            program.id,
            F.Either.Left(IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG),
            SubmittedApplicationFilter.builder()
                .setSearchNameFragment(Optional.of(searchFragment))
                .setSubmitTimeFilter(TimeFilter.EMPTY)
                .build());

    assertThat(
            paginationResult.getPageContents().stream()
                .map(a -> a.getApplicant().getAccount().getEmailAddress())
                .collect(ImmutableSet.toImmutableSet()))
        .isEqualTo(wantEmails);
    assertThat(paginationResult.getNumPages()).isEqualTo(wantEmails.isEmpty() ? 0 : 1);
  }

  private ApplicationModel makeApplicationWithName(
      ApplicantModel applicant,
      ProgramModel program,
      String firstName,
      String middleName,
      String lastName) {
    ApplicationModel application = resourceCreator.insertActiveApplication(applicant, program);
    ApplicantData applicantData = application.getApplicantData();
    QuestionAnswerer.answerNameQuestion(
        applicantData, WellKnownPaths.APPLICANT_NAME, firstName, middleName, lastName);
    application.setApplicantData(applicantData);
    application.save();
    return application;
  }

  private static final StatusDefinitions.Status FIRST_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("First status")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("First status"))
          .build();

  private static final StatusDefinitions.Status SECOND_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Second status")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Second status"))
          .build();

  private static final StatusDefinitions.Status THIRD_STATUS =
      StatusDefinitions.Status.builder()
          .setStatusText("Third status")
          .setLocalizedStatusText(LocalizedStrings.withDefaultValue("Third status"))
          .build();

  @Test
  public void getApplicationsForAllProgramVersions_filterByStatus() throws Exception {
    ProgramModel program =
        ProgramBuilder.newActiveProgram("test program", "description")
            .withStatusDefinitions(
                new StatusDefinitions(ImmutableList.of(FIRST_STATUS, SECOND_STATUS, THIRD_STATUS)))
            .build();

    AccountModel adminAccount = resourceCreator.insertAccountWithEmail("admin@example.com");

    ApplicationModel firstStatusApplication =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    createStatusEvents(
        adminAccount, firstStatusApplication, ImmutableList.of(Optional.of(FIRST_STATUS)));

    ApplicationModel secondStatusApplication =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    createStatusEvents(
        adminAccount, secondStatusApplication, ImmutableList.of(Optional.of(SECOND_STATUS)));

    ApplicationModel thirdStatusApplication =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    // Create a few status events before-hand to ensure that the latest status is used.
    createStatusEvents(
        adminAccount,
        thirdStatusApplication,
        ImmutableList.of(
            Optional.of(FIRST_STATUS), Optional.of(SECOND_STATUS), Optional.of(THIRD_STATUS)));

    ApplicationModel noStatusApplication =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);

    ApplicationModel backToNoStatusApplication =
        resourceCreator.insertActiveApplication(
            resourceCreator.insertApplicantWithAccount(), program);
    // Application has transitioned through statuses and arrived back at an unset status.
    createStatusEvents(
        adminAccount,
        backToNoStatusApplication,
        ImmutableList.of(Optional.of(SECOND_STATUS), Optional.empty()));

    // Unspecified status returns all results.
    assertThat(applicationIdsForProgramAndFilter(program, SubmittedApplicationFilter.EMPTY))
        .isEqualTo(
            ImmutableSet.of(
                firstStatusApplication.id,
                secondStatusApplication.id,
                thirdStatusApplication.id,
                noStatusApplication.id,
                backToNoStatusApplication.id));

    // Empty status returns all results.
    assertThat(
            applicationIdsForProgramAndFilter(
                program,
                SubmittedApplicationFilter.builder()
                    .setApplicationStatus(Optional.of(""))
                    .setSubmitTimeFilter(TimeFilter.EMPTY)
                    .build()))
        .isEqualTo(
            ImmutableSet.of(
                firstStatusApplication.id,
                secondStatusApplication.id,
                thirdStatusApplication.id,
                noStatusApplication.id,
                backToNoStatusApplication.id));

    // Specific status returns only statuses with that result.
    assertThat(
            applicationIdsForProgramAndFilter(
                program,
                SubmittedApplicationFilter.builder()
                    .setApplicationStatus(Optional.of(SECOND_STATUS.statusText()))
                    .setSubmitTimeFilter(TimeFilter.EMPTY)
                    .build()))
        .isEqualTo(ImmutableSet.of(secondStatusApplication.id));

    // Specifies only with no status set.
    assertThat(
            applicationIdsForProgramAndFilter(
                program,
                SubmittedApplicationFilter.builder()
                    .setApplicationStatus(
                        Optional.of(SubmittedApplicationFilter.NO_STATUS_FILTERS_OPTION_UUID))
                    .setSubmitTimeFilter(TimeFilter.EMPTY)
                    .build()))
        .isEqualTo(ImmutableSet.of(noStatusApplication.id, backToNoStatusApplication.id));

    // Unrecognized status option.
    assertThat(
            applicationIdsForProgramAndFilter(
                program,
                SubmittedApplicationFilter.builder()
                    .setApplicationStatus(Optional.of("some-random-status"))
                    .setSubmitTimeFilter(TimeFilter.EMPTY)
                    .build()))
        .isEqualTo(ImmutableSet.of());
  }

  private ImmutableSet<Long> applicationIdsForProgramAndFilter(
      ProgramModel program, SubmittedApplicationFilter filter) {
    PaginationResult<ApplicationModel> result =
        repo.getApplicationsForAllProgramVersions(
            program.id,
            F.Either.Left(IdentifierBasedPaginationSpec.MAX_PAGE_SIZE_SPEC_LONG),
            filter);
    assertThat(result.hasMorePages()).isEqualTo(false);
    return result.getPageContents().stream()
        .map(app -> app.id)
        .collect(ImmutableSet.toImmutableSet());
  }

  private void createStatusEvents(
      AccountModel actorAccount,
      ApplicationModel application,
      ImmutableList<Optional<StatusDefinitions.Status>> statuses)
      throws InterruptedException {
    for (Optional<StatusDefinitions.Status> status : statuses) {
      String statusText = status.map(StatusDefinitions.Status::statusText).orElse("");
      ApplicationEventDetails details =
          ApplicationEventDetails.builder()
              .setEventType(ApplicationEventDetails.Type.STATUS_CHANGE)
              .setStatusEvent(
                  StatusEvent.builder().setStatusText(statusText).setEmailSent(true).build())
              .build();
      ApplicationEventModel event =
          new ApplicationEventModel(application, Optional.of(actorAccount), details);
      event.save();

      // When persisting models with @WhenModified fields, EBean
      // truncates the persisted timestamp to milliseconds:
      // https://github.com/seattle-uat/civiform/pull/2499#issuecomment-1133325484.
      // Sleep for a few milliseconds to ensure that a subsequent
      // update would have a distinct timestamp.
      TimeUnit.MILLISECONDS.sleep(5);
    }
  }

  @Test
  public void getApplicationsForAllProgramVersions_withDateRange() {
    ProgramModel program = resourceCreator.insertActiveProgram("test program");

    ApplicantModel applicantTwo =
        resourceCreator.insertApplicantWithAccount(Optional.of("two@example.com"));
    ApplicantModel applicantThree =
        resourceCreator.insertApplicantWithAccount(Optional.of("three@example.com"));
    ApplicantModel applicantOne =
        resourceCreator.insertApplicantWithAccount(Optional.of("one@example.com"));

    var applicationOne = resourceCreator.insertActiveApplication(applicantOne, program);
    CfTestHelpers.withMockedInstantNow(
        "2022-01-01T00:00:00Z",
        () -> {
          applicationOne.setSubmitTimeToNow();
          applicationOne.save();
        });

    var applicationTwo = resourceCreator.insertActiveApplication(applicantTwo, program);
    CfTestHelpers.withMockedInstantNow(
        "2022-02-01T00:00:00Z",
        () -> {
          applicationTwo.setSubmitTimeToNow();
          applicationTwo.save();
        });

    var applicationThree = resourceCreator.insertActiveApplication(applicantThree, program);
    CfTestHelpers.withMockedInstantNow(
        "2022-03-01T00:00:00Z",
        () -> {
          applicationThree.setSubmitTimeToNow();
          applicationThree.save();
        });

    PaginationResult<ApplicationModel> paginationResult =
        repo.getApplicationsForAllProgramVersions(
            program.id,
            F.Either.Right(new PageNumberBasedPaginationSpec(/* pageSize= */ 10)),
            SubmittedApplicationFilter.builder()
                .setSubmitTimeFilter(
                    TimeFilter.builder()
                        .setFromTime(Optional.of(Instant.parse("2022-01-25T00:00:00Z")))
                        .setUntilTime(Optional.of(Instant.parse("2022-02-10T00:00:00Z")))
                        .build())
                .build());

    assertThat(paginationResult.hasMorePages()).isFalse();
    assertThat(
            paginationResult.getPageContents().stream()
                .map(a -> a.id)
                .collect(ImmutableList.toImmutableList()))
        .isEqualTo(ImmutableList.of(applicationTwo.id));
  }

  @Test
  public void getApplicationsForAllProgramVersions_multipleVersions_pageNumberBasedPagination() {
    ApplicantModel applicantOne =
        resourceCreator.insertApplicantWithAccount(Optional.of("one@example.com"));
    ProgramModel originalVersion = resourceCreator.insertActiveProgram("test program");

    resourceCreator.insertActiveApplication(applicantOne, originalVersion);

    ProgramModel nextVersion = resourceCreator.insertDraftProgram("test program");
    resourceCreator.publishNewSynchronizedVersion();

    ApplicantModel applicantTwo =
        resourceCreator.insertApplicantWithAccount(Optional.of("two@example.com"));
    ApplicantModel applicantThree =
        resourceCreator.insertApplicantWithAccount(Optional.of("three@example.com"));
    resourceCreator.insertActiveApplication(applicantTwo, nextVersion);
    resourceCreator.insertActiveApplication(applicantThree, nextVersion);

    PaginationResult<ApplicationModel> paginationResult =
        repo.getApplicationsForAllProgramVersions(
            nextVersion.id,
            F.Either.Right(new PageNumberBasedPaginationSpec(/* pageSize= */ 2)),
            SubmittedApplicationFilter.EMPTY);

    assertThat(paginationResult.getNumPages()).isEqualTo(2);
    assertThat(paginationResult.getPageContents().size()).isEqualTo(2);

    assertThat(paginationResult.getPageContents().get(0).getApplicant()).isEqualTo(applicantThree);
    assertThat(paginationResult.getPageContents().get(1).getApplicant()).isEqualTo(applicantTwo);

    paginationResult =
        repo.getApplicationsForAllProgramVersions(
            nextVersion.id,
            F.Either.Right(
                new PageNumberBasedPaginationSpec(/* pageSize= */ 2, /* currentPage= */ 2)),
            SubmittedApplicationFilter.EMPTY);

    assertThat(paginationResult.getNumPages()).isEqualTo(2);
    assertThat(paginationResult.getPageContents().size()).isEqualTo(1);

    assertThat(paginationResult.getPageContents().get(0).getApplicant()).isEqualTo(applicantOne);
  }

  @Test
  public void getApplicationsForAllProgramVersions_multipleVersions_offsetBasedPagination() {
    ApplicantModel applicantOne =
        resourceCreator.insertApplicantWithAccount(Optional.of("one@example.com"));
    ProgramModel originalVersion = resourceCreator.insertActiveProgram("test program");

    resourceCreator.insertActiveApplication(applicantOne, originalVersion);

    ProgramModel nextVersion = resourceCreator.insertDraftProgram("test program");
    resourceCreator.publishNewSynchronizedVersion();

    ApplicantModel applicantTwo =
        resourceCreator.insertApplicantWithAccount(Optional.of("two@example.com"));
    ApplicantModel applicantThree =
        resourceCreator.insertApplicantWithAccount(Optional.of("three@example.com"));
    resourceCreator.insertActiveApplication(applicantTwo, nextVersion);
    resourceCreator.insertActiveApplication(applicantThree, nextVersion);

    PaginationResult<ApplicationModel> paginationResult =
        repo.getApplicationsForAllProgramVersions(
            nextVersion.id,
            F.Either.Left(new IdentifierBasedPaginationSpec<>(2, Long.MAX_VALUE)),
            SubmittedApplicationFilter.EMPTY);

    assertThat(paginationResult.getNumPages()).isEqualTo(2);
    assertThat(paginationResult.getPageContents().size()).isEqualTo(2);

    assertThat(paginationResult.getPageContents().get(0).getApplicant()).isEqualTo(applicantThree);
    assertThat(paginationResult.getPageContents().get(1).getApplicant()).isEqualTo(applicantTwo);

    paginationResult =
        repo.getApplicationsForAllProgramVersions(
            nextVersion.id,
            F.Either.Left(
                new IdentifierBasedPaginationSpec<>(
                    2, paginationResult.getPageContents().get(1).id)),
            SubmittedApplicationFilter.EMPTY);

    assertThat(paginationResult.getPageContents().size()).isEqualTo(1);
    assertThat(paginationResult.getPageContents().get(0).getApplicant()).isEqualTo(applicantOne);
  }
}
